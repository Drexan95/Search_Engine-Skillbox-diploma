package UrlService;

import Database.DBConnection;
import Lemmatizer.Lem;
import lombok.Getter;
import lombok.Setter;
import main.model.*;
import main.repository.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class URLCollector extends RecursiveTask<Integer> {
    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private String root;
    @Getter
    private  Page page = null;

    @Autowired
    @Getter
    @Setter
    private PageRepository pageRepository;
    @Autowired
    @Getter
    @Setter
    private FieldRepository fieldRepository;
    @Autowired
    @Getter
    @Setter
    private LemmaRepository lemmaRepository;
    @Autowired
    @Getter
    @Setter
    private SearchIndexRepository searchIndexRepository;
    @Autowired
    @Getter
    @Setter
    private SiteRepository siteRepository;


    private List<String> urls = new ArrayList<>();
    private List<URLCollector> tasks = new ArrayList<>();

    @Setter
    private Set<String> visitedInternalLink = Collections.synchronizedSet(new HashSet<>());
    @Getter
    @Setter
    private ConcurrentHashMap<String, Integer> frequency = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Float> ranks;
    @Getter
    @Setter
    private ConcurrentHashMap<String, Integer> lemmaIds = new ConcurrentHashMap<>();
    @Getter
    @Setter
    private Site site;
    private int thisPageId;
    private Integer pageCount = 1;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    @Getter
    private final static AtomicInteger pageId = new AtomicInteger(0);
    @Getter
    private final static AtomicInteger fieldId = new AtomicInteger(0);
    @Getter
    private final static AtomicInteger lemmaId = new AtomicInteger(0);


    public URLCollector() throws SQLException {
        DBConnection.getConnection();
    }

    public void createCollector(Site site) throws SQLException, IOException {
        this.site = site;
        Lem.createMorph();
        this.setSite(site);
        path = HTMLDataFilter.slashAtEnd( site.getUrl());
        root = site.getUrl();
        ranks = new ConcurrentHashMap<>();
        visitedInternalLink.add(HTMLDataFilter.slashAtEnd( root));
    }

    //Task
    public URLCollector(String path, Set<String> visitedInternalLink) throws SQLException {
        this.path = path;
        this.visitedInternalLink = visitedInternalLink;
        visitedInternalLink.add(path);
        ranks = new ConcurrentHashMap<>();
        DBConnection.getConnection();


    }

    @Override
    protected Integer compute() {
        try {
            Connection.Response response;
            response = Jsoup.connect(path).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) VEGASearchBot/1.0.0 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36")
                    .referrer("http://www.google.ru")
                    .execute();
            Document doc = response.parse();
            Thread.sleep(2000);
            Elements fields = doc.select("html > *");
            Elements links = doc.select("a[abs:href^=" + path + "]");
            int code = response.statusCode();
            String content = doc.toString();
//======================================PAGES================================================//
            /**
             * Pages
             */
            synchronized (pageId) {
                pageId.getAndIncrement();
                path = path.split(root)[1];
                page = new Page(pageId.get(), path, code, content);
                thisPageId = page.getId();
                addPage(page);
                try {//update time after each page
                    Optional<Site> optionalSite = siteRepository.findById(site.getId());
                    if (optionalSite.isPresent()) {
                        optionalSite.get().setStatusTime(simpleDateFormat.parse(simpleDateFormat.format(new Date())));
                        siteRepository.save(optionalSite.get());
                    }
//
                } catch (ParseException exception) {
                    exception.printStackTrace();
                    site.setError("Ошибка! Страница сайта недоступна \\n"+exception.getMessage());
                }
            }
//======================================HTML Elements================================================//
            for (Element field : fields) {
                Elements pageElements = field.getAllElements();
                Element element;
                for (int i = 0; i < pageElements.size(); i++) {
                    element = pageElements.get(i);

                    String text = HTMLDataFilter.findText(element.toString());
                    if (HTMLDataFilter.elementIsRedundant(element, text) || code == 204) {
                        element.remove();
                        continue;
                    }

                    //==================================FIELDS==========================================//

                    float fieldWeight = HTMLDataFilter.calculateFieldWeight(element);
                    /**
                     * Fields
                     */
                    synchronized (fieldId) {
                        fieldId.getAndIncrement();
                        addField(fieldId.get(), element.tagName(), element.cssSelector(), fieldWeight);
                    }
                    //==================================RANKS AND LEMMS==============================================//
                    ConcurrentHashMap<String, Integer> finalFieldLems = new ConcurrentHashMap<>(Lem.searchForLem(text));
                    collectRanksAndLemmsFromField(finalFieldLems, fieldWeight);
                }
            }

            /**
             * Index
             */
            if (code < 400) {
                ranks.keySet().forEach(lema -> {
                    synchronized (pageId) {
                        addIndex(thisPageId, lemmaIds.get(lema), ranks.get(lema));
                    }
                });
            }
            //======================================================================================//
            collectLinks(links);
            forkTasks(urls);
            joinTasks();

        } catch (IOException | InterruptedException | NullPointerException | SQLException exception) {
            exception.printStackTrace();
            site.setError("Ошибка! Страница сайта недоступна \\n"+exception.getMessage());
        }
        return pageCount;
    }

    private void newLemma(String lemma) {
        if (!lemmaIds.containsKey(lemma)) {
            try {
                /**
                 * Lemms
                 */
                lemmaId.getAndIncrement();
                lemmaIds.put(lemma, lemmaId.get());
                addLemma(lemmaId.get(), lemma);

            } catch (NullPointerException ex) {
                ex.printStackTrace();
                site.setError(ex.getMessage());
            }

        }
    }
    //==========================================================================================================================================================//
    private void collectRanksAndLemmsFromField(ConcurrentHashMap<String, Integer> fieldLemms, float fieldWeight) {
        fieldLemms.keySet().forEach(lemma -> {
            synchronized (lemmaId) {
                newLemma(lemma);
                float lemRank = fieldLemms.get(lemma) * fieldWeight;
                if (ranks.containsKey(lemma)) {
                    ranks.put(lemma, ranks.get(lemma) + lemRank);
                } else {
                    ranks.put(lemma, lemRank);
                    int lemmFrequency = frequency.getOrDefault(lemma, 0);
                    frequency.put(lemma, lemmFrequency + 1);
                }
            }
        });
    }

    private void collectLinks(Elements links) {
        for (Element element : links) {
            String url = element.absUrl("href");
            if (visitedInternalLink.contains(url) || HTMLDataFilter.skip(url, root) || !HTMLDataFilter.isInternalLink(url, root)) {
                continue;
            }
            urls.add(url);
            visitedInternalLink.add(url);
        }
    }

    private void joinTasks() throws IOException, SQLException {
        for (URLCollector task : tasks) {
            pageCount += task.join();
        }

    }

    private void forkTasks(List<String> urls) throws SQLException {
        for (String url : urls) {
            URLCollector task = new URLCollector(url, visitedInternalLink);
            task.setSiteRepository(siteRepository);
            task.setPageRepository(pageRepository);
            task.setFieldRepository(fieldRepository);
            task.setSearchIndexRepository(searchIndexRepository);
            task.setLemmaRepository(lemmaRepository);
            task.setFrequency(frequency);
            task.setLemmaIds(lemmaIds);
            task.setRoot(root);
            task.setSite(site);
            task.fork();
            tasks.add(task);
        }
    }

    public void collectFrequency() throws SQLException {
        Iterable<Lemma> lemmaIterable = lemmaRepository.findAll();
        lemmaIterable.iterator().forEachRemaining(lemma -> {
            if (frequency.containsKey(lemma.getName())) {

                lemma.setFrequency(frequency.get(lemma.getName()));
            }
        });
        lemmaRepository.saveAll(lemmaIterable);
    }

    private void addPage(Page page) {
        page.setSiteid(site.getId());
        page.setSite(site.getUrl());
        page.setSiteName(site.getName());
        pageRepository.save(page);
    }

    private void addField(int Id, String name, String selector, float weight) {
        Field field = new Field(Id, name, selector, weight);
        fieldRepository.save(field);
    }

    private void addLemma(int Id, String name) {
        Lemma lemma = new Lemma(Id, name, 0);
        lemma.setSiteId(site.getId());
        lemmaRepository.save(lemma);
    }

    private void addIndex(int pageId, int lemma_Id, float lemmaRank) {
        SearchIndex index = new SearchIndex(pageId, lemma_Id, lemmaRank);
        searchIndexRepository.save(index);

    }
    //==========================================================================================================================================================//


}
