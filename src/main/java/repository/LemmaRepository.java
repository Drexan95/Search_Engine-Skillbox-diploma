package repository;

import model.Lemma;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma,Integer> {
}
