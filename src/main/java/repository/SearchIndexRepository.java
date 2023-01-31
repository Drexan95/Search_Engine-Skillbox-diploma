package repository;

import model.SearchIndex;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchIndexRepository extends CrudRepository<SearchIndex, Integer> {
    @Modifying
    @Query(value = "delete from search_index  where search_index.page_id=:page_id",nativeQuery = true)
    void deleteSearchIndexes(@Param("page_id") Integer page_id);
}
