package csv.hierarchy.pg.persistent.repository;

import csv.hierarchy.pg.persistent.entity.Definition;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;

public interface DefinitionRepository extends JpaRepository<Definition, Long> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO definition (client_id, hierarchy_id, effective_from, schema, version)
            VALUES (:clientId, :hierarchyId, :effectiveFrom, :schema,
             (SELECT COALESCE(MAX(version),0) + 1 FROM definition WHERE hierarchy_id = :hierarchyId)
            )
            """)
    @CacheEvict(value = "def-hierarchy-last", key = "#hierarchyId")
    void saveDef(Long clientId, Long hierarchyId, Timestamp effectiveFrom, String schema);

    @Query(nativeQuery = true, value = "SELECT LASTVAL()")
    Long getLastInsertId();

    @Query(nativeQuery = true, value = """
            SELECT id FROM definition WHERE hierarchy_id = :hierarchyId order by version desc LIMIT 1
            """)
    @Cacheable(value = "def-hierarchy-last", key = "#hierarchyId")
    Long getCurrentDefinition(Long hierarchyId);

    @Query(nativeQuery = true, value = """
            SELECT id FROM definition
            WHERE hierarchy_id = :hierarchyId
            AND effective_from <= :effectiveFrom
            ORDER BY version DESC
            LIMIT 1;
            """)
    Long getDefinitionId(Long hierarchyId, Timestamp effectiveFrom);
}
