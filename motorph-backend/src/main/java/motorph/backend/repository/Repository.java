package motorph.backend.repository;

import java.util.List;

/**
 * Defines common database operations shared by entity repositories.
 */
public interface Repository<T, ID> {

    T findById(ID id);

    List<T> findAll();

    void save(T entity);
}
