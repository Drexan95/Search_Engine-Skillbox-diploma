package main.model;

import Database.DBConnection;
import lombok.Getter;
import lombok.Setter;
import main.repository.FieldRepository;
import main.repository.LemmaRepository;
import main.repository.PageRepository;
import main.repository.SiteRepository;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class StatisticsInfo {
    public StatisticsInfo() {

    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Autowired
    @Getter
    private Total total;
    @Autowired
    @Getter
    private Statistics statistics;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private FieldRepository fieldRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Getter
    @Setter
    private boolean result = true;


    @Getter
    @Setter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private long sites;
    @Getter
    @Setter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private long pages;

    @Setter
    @Getter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private long lemmas;

    @Getter
    @Setter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean isIndexing = false;

    @Getter
    @Setter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private final List<Site> detailed = new ArrayList<>();

    public void getDetailedData(Site site) throws SQLException, JSONException {

        Statement statement = DBConnection.getConnection().createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT (SELECT COUNT(id) FROM lemma WHERE site_id = " + site.getId() + ") AS lemmscount," +
                "(SELECT COUNT(id) FROM page WHERE site_id = " + site.getId() + ") as pagecount,(SELECT last_error FROM site WHERE id = "+site.getId()+") AS error");
        while (resultSet.next()) {
            long pageCount = resultSet.getInt("pagecount");
            long lemmsCount = resultSet.getInt("lemmscount");
            String error = resultSet.getString("error");
            isIndex(site);
            site.setLemmas(lemmsCount);
            site.setPages(pageCount);
            site.setError(error);
            statistics.getDetailed().add(site);
        }
        total.setIndexing(isIndexing);
        total.setSites(sites);
        total.setLemmas(lemmas);
        total.setPages(pages);
        statistics.setTotal(total);
        statistics.getDetailed().addAll(detailed);
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public boolean isIndex(Site site) {
        if (site.getStatus().equals(StatusType.INDEXING)) {
            return isIndexing = true;
        } else return isIndexing;
    }

}
