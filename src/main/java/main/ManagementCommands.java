package main;


import UrlService.URLCollector;
import main.model.Site;
import main.model.Statistics;
import main.model.StatisticsInfo;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ManagementCommands {
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
    URLCollector collector;
    @Autowired
    private StatisticsInfo statisticsInfo;
    @Autowired
    private Statistics statistics;
    @Autowired
    private  IndexingCommands indexingCommands;
    @Autowired
    PageResults pageResults;
    private Statement statement;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Value("${sites.url}")
    private String[] urls;

    /**
     * Method start full indexing sites given in application.yml
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws ParseException
     * @throws JSONException
     */
@Transactional
   public ResponseEntity<String> startIndexing() throws SQLException, IOException, ParseException, JSONException, InterruptedException, StaleObjectStateException {
     if(urls.length==0){
         JSONObject response = new JSONObject();
         response.put("result",false);
         response.put("error","В конфигурационном файле не передано ни одного сайта!");
         return new ResponseEntity<>(response.toString(),HttpStatus.OK);
     }
    Iterable<Site> sites = siteRepository.findAll();
        AtomicBoolean isIndexing = new AtomicBoolean(false);
        sites.forEach(site -> {
            if(site.getStatus().equals(StatusType.INDEXING)){
                isIndexing.set(true);
            }
        });
        if(isIndexing.get()){
            JSONObject response = new JSONObject();
            response.put("result", false);
            response.put("error", "Индексация уже запущена!");
            return new ResponseEntity<>(response.toString(),HttpStatus.OK);
        }
        try {
            indexingCommands.clearDB();
            indexingCommands.start();
            return new ResponseEntity<>(jsonStartIndexing(isIndexing.get()).toString(), HttpStatus.OK);
        }
        catch (StaleObjectStateException ex){
            indexingCommands.getTasks().forEach(collector->{
                collector.getSite().setError(ex.getMessage());
                siteRepository.save(collector.getSite());
            });
            return new ResponseEntity<>("Повторите попытку",HttpStatus.OK);
        }
    }

    /**
     * Method abort indexing
     * @return
     * @throws JSONException
     */
    public ResponseEntity<String> stopIndexing() throws JSONException {
        boolean isIndexing = indexingCommands.getPools().isEmpty();
        if(!isIndexing){
            indexingCommands.getThreads().forEach(Thread::interrupt);
          indexingCommands.getPools().forEach(ForkJoinPool::shutdownNow);
           indexingCommands.getTasks().clear();
           indexingCommands.getPools().clear();
           indexingCommands.getThreads().clear();
           siteRepository.findAll().forEach(site -> {
               site.setStatus(StatusType.FAILED);
               site.setError("Остановка индексации");
               siteRepository.save(site);
           });
        }
        return new ResponseEntity<>(jsonStopIndexing(isIndexing).toString(),HttpStatus.OK);
    }


    public ResponseEntity<?> getStatistics() throws JSONException {
        statisticsInfo.setSites(siteRepository.count());
        statisticsInfo.setPages(pageRepository.count());
        statisticsInfo.setLemmas(lemmaRepository.count());
        Iterable<Site> siteIterable = siteRepository.findAll();
        siteIterable.forEach(site -> {
            try {
                statisticsInfo.getDetailedData(site);
            } catch (SQLException | JSONException exception) {
                exception.printStackTrace();
            }
        });

            JSONObject response = new JSONObject();
            response.put("result", true);
            response.put("statistics", statisticsInfo.getStatistics());
            return new ResponseEntity<>(statisticsInfo, HttpStatus.OK);
    }

    @Transactional
    private JSONObject jsonStartIndexing(boolean status) throws JSONException, StaleObjectStateException, SQLException {
        JSONObject response = new JSONObject();
        if (!status) {
            try {
                indexingCommands.getTasks().forEach(collector->{
            Optional<Site> site = siteRepository.findById(collector.getSite().getId());
            site.get().setStatus(StatusType.INDEXING);
            siteRepository.save(site.get());
                });
                indexingCommands.indexing(indexingCommands.getTasks());
            }
            catch (StaleObjectStateException staleObjectStateException){
                staleObjectStateException.printStackTrace();
                response.put("Повторите попытку",HttpStatus.OK);
                return response;
            }//--->
            response.put("result", true);
            return response;
        } else
            response.put("result", false);
        response.put("error", "Индексация уже запущена");
        return response;
    }

    private static JSONObject jsonStopIndexing(boolean status) throws JSONException
    {
        JSONObject response = new JSONObject();
        if(!status){
            response.put("result",true);
            return response;
        }
        else response.put("result",false);
        response.put("error","Индексация не запущена");
        return response;
    }

}
