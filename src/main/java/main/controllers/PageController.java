package main.controllers;

import main.IndexingCommands;
import main.ManagementCommands;
import main.SearchCommands;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.SQLException;


@RestController
@RequestMapping
public class PageController {

    @Autowired
    private ManagementCommands managementCommands;
    @Autowired
    private SearchCommands searchCommands;
    @Autowired
    private IndexingCommands indexingCommands;



    @Transactional
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "query") String query,
                                    @RequestParam(required = false) @Nullable String site,
                                    @RequestParam(required = false) @Nullable Integer limit,
                                    @RequestParam(required = false) @Nullable Integer offset) throws SQLException, IOException, JSONException
    {
       return searchCommands.search(query,site,limit,offset);
    }

    /**
     * Method launch reindexing of the given page
     * @param url
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws JSONException
     */
    @Transactional
    @PostMapping("/indexPage")
    public ResponseEntity<String> addPage(@RequestParam(name = "url") String url) throws SQLException, IOException, JSONException
    {
        return indexingCommands.addPage(url);

    }
}
