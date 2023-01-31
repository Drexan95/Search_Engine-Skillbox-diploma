package service;

import Database.DBConnection;
import Lemmatizer.Lem;
import lombok.Getter;
import model.*;
import repository.LemmaRepository;
import repository.PageRepository;
import repository.SiteRepository;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PageResults {


    private Statement statement;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Getter
    private long maxResults;
    final int RESULTS_TO_SHOW = 20;


    private List<Lemma> getLemms(SearchRequest request) throws IOException {
        maxResults=0;
        try {
            Lem.createMorph();
            statement = DBConnection.getConnection().createStatement();
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
        }
        Map<String, Integer> lemms = new ConcurrentHashMap<>(Lem.searchForLem(request.getText()));
       for(String lemma : lemms.keySet()){
           System.out.println("лемма в леммс "+ lemma);
       }
        List<Lemma> frequencyLemms = new ArrayList<>();//List of lemmas to find

        /**
         * Find lemmas in database
         */
        lemms.keySet().forEach(word -> {
            try {
                ResultSet resultSet = statement.executeQuery("SELECT id FROM lemma WHERE lemma.lemma = " + '\"' + word + '\"');
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    System.out.println(id);
                    Optional<Lemma> lemma = lemmaRepository.findById(id);
                    lemma.ifPresent(frequencyLemms::add);
                }
            } catch (SQLException | NullPointerException exception) {
                exception.printStackTrace();
                System.out.println("Совпадения  не найдены");
            }
        });
        return frequencyLemms;
    }

    /**
     * Find pages where lemma occur
     * @param request
     * @return
     * @throws IOException
     * @throws SQLException
     * @throws NullPointerException
     */
    @Transactional
    private List<Lemma> getListOfUrls(SearchRequest request) throws IOException, SQLException, NullPointerException {
        List<Lemma> frequencyLemms = new ArrayList<>(getLemms(request));
        AtomicLong siteId = new AtomicLong();
        int pageCount = calculatePageCount(request,siteId);

        String query = "SELECT page.id FROM page  JOIN search_index ON page.id = search_index.page_id JOIN lemma ON lemma.id = search_index.lemma_id " +
                "WHERE lemma_id = ?";
        if (siteId.get() != 0) {
            query = query + " AND page.site_id =" + siteId.get();
            frequencyLemms.removeIf(lemma -> !lemma.getSiteId().equals(siteId.get()));
        }
        linkLemmaAndPage(frequencyLemms,query);

        int finalPageCount = pageCount;  //Don't consider lemma if it appears in more than 80% of the pages
        try {
            frequencyLemms.removeIf(lemma -> frequencyLemms.size() > 1 && 100 / (finalPageCount / lemma.getUrls().size()) > 80 && frequencyLemms.indexOf(lemma) > 0);
            if (frequencyLemms.size() > 1) {
                frequencyLemms.stream().sorted().skip(1)
                        .forEach(lemma -> lemma.getUrls().subList(lemma.getUrls().size() - (int) ((lemma.getUrls().size() * 10L) / 100), lemma.getUrls().size()).clear()
                        );
            }//Decrease number of pages for each lemma by 10%
        } catch (ArithmeticException ae) {
            ae.printStackTrace();
        }
        System.out.println("кол-во слов "+frequencyLemms.size());
        return frequencyLemms;
    }

private void linkLemmaAndPage(List<Lemma> frequencyLemms,String query) throws SQLException {
    PreparedStatement preparedStatement = DBConnection.getConnection().prepareStatement(query);
    frequencyLemms.forEach(lemma -> {
        try {
            preparedStatement.setInt(1, lemma.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("page.id");
                Optional<Page> page = pageRepository.findById(id);
                page.ifPresent(value -> lemma.getUrls().add(value));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    });
}
    /**
     * Calculate absolute relevancy
     * method calculates the relevance of pages upon request
     *
     * @param lemms
     * @return
     * @throws SQLException
     * @throws IOException
     */
    @Transactional
    private List<Page> calculateRelevancy(List<Lemma> lemms) throws SQLException, IOException {
        ConcurrentHashMap<Page, Float> pageAndRelevancy = new ConcurrentHashMap<>();//страницы где встречаются леммы
        List<Page> sortedPages = new ArrayList<>();
        int[] lemmsArray = new int[lemms.size()];
        for (int i = 0; i <= lemms.size() - 1; i++) {
            lemmsArray[i] = lemms.get(i).getId();
        }
        String prepareSQl = " UNION ALL (SELECT SUM(search_index.rank) as lemmSum from search_index where page_id= %d and lemma_id = %d)";
        lemms.forEach(lemma -> {
            lemma.getUrls().forEach(page -> {
                page.getLemms().add(lemma.getName());
                StringBuilder sql = new StringBuilder("SELECT SUM(lemmSum) FROM((SELECT sum(search_index.rank) as lemmSum FROM search_index WHERE lemma_id = "
                        + lemmsArray[0] + " AND page_id = " + page.getId() + ")");
                for (int i = 1; i < lemmsArray.length; i++) {
                    sql.append(String.format(prepareSQl, page.getId(), lemmsArray[i]));
                }
                try {
                    ResultSet resultSet = statement.executeQuery(sql + ")search_index");
                    while (resultSet.next()) {
                        pageAndRelevancy.put(page, resultSet.getFloat("SUM(lemmSum)"));
                        page.setAbsRelevancy(resultSet.getFloat("SUM(lemmSum)"));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

            });
        });
       return   sortPages(pageAndRelevancy);
    }

private List<Page> sortPages(ConcurrentHashMap<Page,Float> pageAndRelevancy){
    List<Page> sortedPages = new ArrayList<>();
    pageAndRelevancy.keySet().forEach(page -> {
        page.setRelevance(page.getAbsRelevancy() / Collections.max(pageAndRelevancy.values()));
        sortedPages.add(page);

    });
    maxResults = sortedPages.size();
    return StreamEx.of(sortedPages).distinct(Page::getPath).sorted().toList();
}

    //////////////////////////////////RANKS COLLECTED///////////////////////////////////////////////////////////////
    @Transactional
    public List<Page> getResults(SearchRequest request) {
        StringBuilder builder = new StringBuilder();
        List<Page> results = new ArrayList<>();
        try {
            calculateRelevancy(getListOfUrls(request)).forEach(page -> {
                Optional<Site> site = siteRepository.findById(page.getSiteid());
                page.setSiteName(site.get().getName());
                page.setSite(site.get().getUrl());
                page.setUri(page.getPath());
                String content = page.getContent();
                Pattern patternTitle = Pattern.compile("<title>(.+?)</title>", Pattern.DOTALL);
                Matcher m = patternTitle.matcher(content);
                while (m.find()) {
                    page.setTitle(m.group(1));
                }
                HashMap<String, String> wordAndLemma = null;
                content = HTMLDataFilter.findText(content);
                String finalContent = content;
                try {
                    wordAndLemma = Lem.replaceForLemms(content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (String lemma : page.getLemms()) {
                    String wordFromLemma = wordAndLemma.get(lemma);
                    page.setSnippet(getSnippet(builder,finalContent,wordFromLemma));
                }
                builder.setLength(0);
                results.add(page);
            });
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
        }
         return results.stream().skip(request.getOffset()).limit(countResults(request.getLimit())).collect(Collectors.toList());

    }

//==============================================================================================================================================================
    private int countResults(int requestCount){
        if (requestCount >= RESULTS_TO_SHOW){
            return requestCount;
        }
        else {
        int x = RESULTS_TO_SHOW - requestCount;
        return RESULTS_TO_SHOW - x;
        }
    }
 private String getSnippet(StringBuilder builder, String finalContent,String wordFromLemma){
     Pattern pattern = Pattern.compile(wordFromLemma);
     Matcher matcher = pattern.matcher(finalContent.toLowerCase(Locale.ROOT));

     int contentLentgh = finalContent.length();
     int lemmIndex = 0;
     int lastLemmIndex = 0;
     int textBorder = 0;
     int numberOfChars = 100;

     while (matcher.find()) {
         lemmIndex = matcher.start();
         lastLemmIndex = matcher.end();
         textBorder = contentLentgh - lastLemmIndex;
     }
     if (textBorder > numberOfChars) {
         String word = finalContent.substring(lemmIndex,lastLemmIndex)+"</b>";
         builder.append("...<b>").append(word)
                 .append(finalContent, lastLemmIndex, lastLemmIndex + numberOfChars).append("...\n");
     }
     else { String word = finalContent.substring(lemmIndex,lastLemmIndex)+"</b>";
         builder.append("...<b>").append(word)
                 .append(finalContent, lastLemmIndex, contentLentgh).append("...\n");
     }

     return builder.toString();
 }

    private int calculatePageCount(SearchRequest request,AtomicLong siteId) throws SQLException {
        int pageCount = 0;
        if (!request.getSiteUrl().equals("")) {
            String siteUrl = request.getSiteUrl();
            Iterable<Site> siteIterable = siteRepository.findAll();
            for (Site site : siteIterable) {
                if (site.getUrl().equals(siteUrl)) {
                    siteId.set(site.getId());

                }
            }

            ResultSet result = statement.executeQuery("SELECT COUNT(id) as page_count  FROM page where site_id= "+siteId.get());
            while (result.next()) {
                pageCount = result.getInt("page_count");
            }
        }

        else {
            ResultSet result = statement.executeQuery("SELECT COUNT(id) as page_count  FROM page");
            while (result.next()) {
                pageCount = result.getInt("page_count");
            }
        }
        return pageCount;
    }

 }


