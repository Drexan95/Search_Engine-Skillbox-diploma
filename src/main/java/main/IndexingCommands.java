package main;

import Database.DBConnection;
import UrlService.URLCollector;
import lombok.Getter;
import main.model.Lemma;
import main.model.Page;
import main.model.Site;
import main.model.StatusType;
import main.repository.*;
import org.hibernate.StaleObjectStateException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;

@Component
public class IndexingCommands {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private FieldRepository fieldRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SearchIndexRepository searchIndexRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private ManagementCommands managementCommands;
    private Statement statement;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Value("${sites.url}")
    private String[] urls;
    @Value("${sites.name}")
    private String[] names;
    @Autowired
    URLCollector collector;
    @Getter
    private final List<URLCollector> tasks = new ArrayList<>();
    @Getter
    private final List<ForkJoinPool> pools = new ArrayList<>();
    @Getter
    private final List<Thread> threads = new ArrayList<>();
    private final  HashSet<Site> sites = new HashSet<>();
    private Site site;

    @org.springframework.transaction.annotation.Transactional
    void start() throws SQLException, IOException, ParseException, StaleObjectStateException, InterruptedException
    {

        String query = "INSERT INTO site(id, last_error, name, status, status_time, url) VALUES(?, ? , ? , ? , ?, ?)";
        PreparedStatement preparedStatement = DBConnection.getConnection().prepareStatement(query);
        Thread.sleep(1000);
        for (int i = 0; i < urls.length; i++) {
            site = new Site(urls[i]);
            site.setId((long) (i + 1));
            site.setName(names[i]);
            site.setStatusTime(simpleDateFormat.parse(simpleDateFormat.format(new Date())));
            site.setStatus(StatusType.FAILED);
            site.setError("none");
            preparedStatement.setLong(1,site.getId());
            preparedStatement.setString(2,"none");
            preparedStatement.setString(3,site.getName());
            preparedStatement.setString(4, String.valueOf(StatusType.FAILED));
            java.sql.Date date = new java.sql.Date(site.getStatusTime().getTime());
            preparedStatement.setDate(5,date);
            preparedStatement.setString(6,site.getUrl());
            preparedStatement.executeUpdate();

            URLCollector collector = new URLCollector();
            autowireCapableBeanFactory.autowireBean(collector);
            collector.createCollector(site);
            tasks.add(collector);
        }



    }

    void indexing(List<URLCollector> collectors) throws SQLException {
        Long start = System.currentTimeMillis();
        collectors.forEach(collector -> {

                threads.add(new Thread(){
                    @Override
                    public void run() {
                        try {

                            Thread.sleep(1000);
                            ForkJoinPool pool = new ForkJoinPool();
                            pools.add(pool);
                            pool.execute(collector);
                            int pageCount = collector.join();
                            collector.collectFrequency();
                            System.out.println("Сайт " + collector.getSite().getName() + " проиндексирован,кол-во ссылок - "+pageCount);
                            System.out.println("Время выполнения: " + (System.currentTimeMillis() - start));
                                                     Optional<Site> optionalSite =   siteRepository.findById(collector.getSite().getId());
                       if(optionalSite.isPresent()){
                           Site site = optionalSite.get();
                           site.setStatus(StatusType.INDEXED);
                           siteRepository.save(site);
                       }
                        }
                        catch (InterruptedException | SQLException | CancellationException ex) {
                            ex.printStackTrace();
                            collector.getSite().setError("Ошибка индексации: "+ ex.getMessage());
                            collector.getSite().setStatus(StatusType.FAILED);
                            siteRepository.save(collector.getSite());
                        }
                    }

                });
        });
        threads.forEach(Thread::start);
        pools.forEach(ForkJoinPool::shutdown);
        pools.clear();
        threads.clear();
        tasks.clear();
        sites.clear();
    }
    /**
     * Method start reindexing the given page
     * Url must be correspond the sites given in application.yml
     * @param url
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws JSONException
     */

    @Transactional
    public ResponseEntity<String> addPage(@RequestParam(name = "url") String url) throws SQLException, IOException, JSONException
    {
        Iterable<Site> sites = siteRepository.findAll();
        Page page = new Page(url);
        JSONObject response = new JSONObject();
        Site pageSite = new Site();

//        Site pageSite = new Site();
//        JSONObject response = new JSONObject();
//        String pageUrl = page.getPath();
//        System.out.println(page.getPath());
//        page.setPath("");
//        for(Site site : sites){
//            if (pageUrl.startsWith(site.getUrl())) {
//                page.setSiteid(site.getId());
//                page.setSite(site.getUrl());
//                page.setSiteName(site.getName());
//                pageSite.setId(site.getId());
//                pageSite.setUrl(site.getUrl());
//                pageSite.setStatus(site.getStatus());
//                page.setPath(pageUrl);
//            }
//        }

        //==========================================================================================================================================
        if(!isSiteIndexed(page,sites,pageSite)){
            try {

                response.put("result", false);
                response.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле или индексация сайта еще не завершена");
                return new ResponseEntity<>(response.toString(),HttpStatus.BAD_REQUEST);
            }
            catch (JSONException ex){
                ex.printStackTrace();
            }
        }
//        else if(!pageSite.getStatus().equals(StatusType.INDEXED)){
//            try {
//
//                response.put("result", false);
//                response.put("error", "Индексация еще не завершена");
//                return new ResponseEntity<>(response.toString(), HttpStatus.CONFLICT);
//            } catch (JSONException ex){
//                ex.printStackTrace();
//            }
//        }
        if(page.getPath().equals(pageSite.getUrl())){
            page.setPath(page.getPath()+"/");
        }
        //==========================================================================================================================================
        Iterable<Page> pageIterable = pageRepository.findAll();
        Set<String> pages = Collections.synchronizedSet(new HashSet<>());
        URLCollector collector = new URLCollector();
        autowireCapableBeanFactory.autowireBean(collector);
        setDataToCollector(collector,page);


        pageIterable.forEach(p -> {
            if (p.getSiteid().equals(page.getSiteid()) && p.getPath().equals(page.getPath().split(pageSite.getUrl())[1])) {
                searchIndexRepository.deleteSearchIndexes(p.getId());
                pageRepository.deleteById(p.getId());
            }
            else if(p.getSiteid().equals(page.getSiteid())) {
                pages.add(pageSite.getUrl()+ p.getPath());

            }
        });
        collector.setVisitedInternalLink(pages);
        collector.createCollector(pageSite);
        collector.setPath(page.getPath());
        ForkJoinPool pool = new ForkJoinPool();
        pool.execute(collector);
        collector.collectFrequency();
        pool.shutdown();
        response.put("result",true);
        return new ResponseEntity<>(response.toString(),HttpStatus.OK);
    }

    private void setDataToCollector(URLCollector collector,Page page) throws SQLException {
        statement = DBConnection.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT MAX(id) as maxId from page");
        while (resultSet.next()){
            page.setId(resultSet.getInt("maxId")+1);
            collector.getPageId().set(page.getId()-1);
        }
        Iterable<Lemma> lemmaIterable = lemmaRepository.findAll();
        collector.getLemmaId().set((int) lemmaRepository.count());
        collector.getFieldId().set((int) fieldRepository.count());
        lemmaIterable.forEach(lemma -> {
            collector.getLemmaIds().put(lemma.getName(), lemma.getId());
            collector.getFrequency().put(lemma.getName(), lemma.getFrequency());
        });
    }
    @org.springframework.transaction.annotation.Transactional
     void clearDB() throws SQLException {

                Statement statement = DBConnection.getConnection().createStatement();
                statement.executeUpdate("DELETE FROM site");
                statement.executeUpdate("DELETE FROM page");
                statement.executeUpdate("DELETE FROM field");
                statement.executeUpdate("DELETE FROM lemma");
                statement.executeUpdate("DELETE FROM search_index");
                statement.executeUpdate("ALTER TABLE search_index AUTO_INCREMENT=1");

            }

    private boolean isSiteIndexed(Page page, Iterable<Site> sites,Site pageSite){

        String pageUrl = page.getPath();
        System.out.println(page.getPath());
        page.setPath("");
        for(Site site : sites){
            if (pageUrl.startsWith(site.getUrl())) {
                page.setSiteid(site.getId());
                page.setSite(site.getUrl());
                page.setSiteName(site.getName());
                pageSite.setId(site.getId());
                pageSite.setUrl(site.getUrl());
                pageSite.setStatus(site.getStatus());
                page.setPath(pageUrl);
            }
        }
        return pageSite.getStatus().equals(StatusType.INDEXED) && !page.getPath().equals("");
    }

    }




