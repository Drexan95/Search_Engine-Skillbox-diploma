package main;

import main.model.Page;
import main.model.Site;
import main.model.StatusType;
import main.repository.SiteRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
public class SearchCommands {
    @Autowired
  private   PageResults pageResults;
    @Autowired
    private ResultResponse resultResponse;
    @Autowired
    private SiteRepository siteRepository;
    /**
     * Method return pages matching search query
     * @param query words to search
     * @param site search in particular site
     * @param limit page results
     * @param offset skip first n results
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws JSONException
     */
    public ResponseEntity<?> search(@RequestParam(name = "query") String query, @Nullable String site, @Nullable Integer limit, @Nullable Integer offset) throws SQLException, IOException, JSONException
    {

        SearchRequest request = new SearchRequestBuilder().setText(query)
                .setSiteUrl(site)
                .setLimit(limit)
                .setOffset(offset)
                .build();
        if(!IsIndexingFinished(request)){
            JSONObject response = new JSONObject();
            response.put("result",false);
            response.put("error","Индексация не завершена");
            return new ResponseEntity<>(response.toString(),HttpStatus.OK);
        }

        List<Page> results = pageResults.getResults(request);

        if(query.equals("")){
            JSONObject response = new JSONObject();
            response.put("result",false);
            response.put("error","Задан пустой поисковой запрос");
            return new ResponseEntity<>( response.toString(), HttpStatus.BAD_REQUEST);
        }
        if(results.size()>0){
            resultResponse.setData(results);
            resultResponse.setCount(pageResults.getMaxResults());
            return new ResponseEntity<>(resultResponse,HttpStatus.OK);
        }
        else {
            JSONObject response = new JSONObject();
            response.put("result",false);
            response.put("error","Совпадения не найдены");
            return new ResponseEntity<>(response.toString(),HttpStatus.OK);
        }
    }

    private boolean IsIndexingFinished(SearchRequest request) {

        Iterable<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            if (!request.getSiteUrl().equals("") && site.getUrl().equals(request.getSiteUrl())) {
                return site.getStatus().equals(StatusType.INDEXED);
            }
            if (!site.getStatus().equals(StatusType.INDEXED)) {
                return false;
            }
        }
        return true;
    }
}
