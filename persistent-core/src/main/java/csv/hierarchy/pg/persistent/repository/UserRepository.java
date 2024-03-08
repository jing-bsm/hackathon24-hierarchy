package csv.hierarchy.pg.persistent.repository;

import csv.hierarchy.pg.persistent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findOneByUserIdAndDefinitionId(Long userId, Long definition);

    @Query(nativeQuery = true, value = """
            SELECT jsonb_exists(entire,:nodeId), scope = 1 or not jsonb_exists(explicit,:nodeId)
            FROM user_auth
            WHERE user_id = :userId
            AND hierarchy_id = :hierarchyId
            ORDER BY definition_id DESC
            limit 1
            """)
    List<Boolean[]> queryNodeByHierarchyId(Long userId, Long hierarchyId, String nodeId);
}
