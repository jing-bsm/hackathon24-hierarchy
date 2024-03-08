package csv.hierarchy.pg.persistent.repository;

import csv.hierarchy.pg.persistent.entity.Node;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Long> {
    @Cacheable(value = "node-def-list", key = "#definitionId")
    List<Node> findNodesByDefinition(Long definitionId);

    @Query(nativeQuery = true, value = """
            SELECT * FROM node
            WHERE definition_id = :definitionId
            AND leaf = true
            """)
    @Cacheable(value = "node-def-leaves", key = "#definitionId")
    List<Node> findLeaves(Long definitionId);

    @Query(nativeQuery = true, value = """
            SELECT * FROM node n
            WHERE definition_id = :definitionId
            AND n.tree @> (
                select tree from node
                where definition_id=:definitionId
                AND id=:id
            );
            """)
    @Cacheable(value = "node-def-id", key = "#definitionId + '_' + #id")
    List<Node> findAncestors(Long definitionId, String id);

    @Query(nativeQuery = true, value = """
            SELECT * FROM node
            WHERE definition_id = :definitionId
            AND tree <@ (
                select tree from node where definition_id = :definitionId and id = :startNodeId
            )
            """)
    @Cacheable(value = "node-def-descendants", key = "#definitionId + '_' + #startNodeId")
    List<Node> findDescendants(Long definitionId, String startNodeId);

    @Query(nativeQuery = true, value = """
            SELECT id FROM node
            WHERE definition_id = :definitionId
            AND depth = 0
            """)
    @Cacheable(value = "node-def-root", key = "#definitionId")
    String findRootId(Long definitionId);

    @Cacheable(value = "node-def-id-node", key = "#definitionId + '_' + #id")
    Node findNodeByDefinitionAndId(Long definition, String id);


    @Query(nativeQuery = true, value = """
            SELECT * FROM node
            WHERE definition_id = :definitionId
            AND ext_data @> cast(:jsonMap as jsonb);
            """)
    List<Node> findByJsonMap(Long definitionId, String jsonMap);
}
