package motorph.infrastructure.repository;

import java.util.List;

/**
 * Defines common database operations shared by entity repositories.
 */
public interface Repository<T, ID> {

    // finds an entity by id
    T findById(ID id);

    // finds all entities
    List<T> findAll();

    // saves an entity
    void save(T entity);
}
