package csv.hierarchy.pg.persistent.service;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.Operator;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.util.HierarchyQueryBuilder;
import csv.hierarchy.pg.persistent.repository.DefinitionRepository;
import csv.hierarchy.pg.persistent.repository.NodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class NodeConditionService {
    private final EntityManager entityManager;
    private final NodeRepository nodeRepository;
    private final DefinitionRepository definitionRepository;

    public List<Node> getDescendantsByHierarchy(@NonNull Long hierarchyId, @NonNull String startNodeId, List<Condition> conditions) {
        Long definition = definitionRepository.getCurrentDefinition(hierarchyId);
        return getDescendants(definition, startNodeId, null == conditions ? Collections.emptyList() : conditions);
    }

    public List<Node> getDescendants(@NonNull Long definitionId, @NonNull String startNodeId, @NonNull List<Condition> conditions) {
        var queryPair = HierarchyQueryBuilder.getQueryPair(definitionId, startNodeId, conditions);
        return process(queryPair);
    }

    public List<Node> getDescendantsByHierarchy(@NonNull Long hierarchyId, @NonNull String startNodeId) {
        return nodeRepository.findDescendants(definitionRepository.getCurrentDefinition(hierarchyId), startNodeId);
    }

    public List<Node> getNodes(@NonNull Long hierarchyId){
        return nodeRepository.findNodesByDefinition(definitionRepository.getCurrentDefinition(hierarchyId));
    }

    /**
     * For explicit, we just need to check if that node match the condition
     *
     * @param definitionId
     * @param singleNodeId
     * @param conditions
     * @return
     */
    public List<Node> getSingle(@NonNull Long definitionId, @NonNull String singleNodeId, @NonNull List<Condition> conditions) {
        var queryPair = HierarchyQueryBuilder.getQueryPairSingle(definitionId, singleNodeId, conditions);
        return process(queryPair);
    }

    public List<Node> getLeaves(Long definitionId, String startNodeId, @NonNull List<Condition> conditions) {
        ArrayList<Condition> list = new ArrayList<>(conditions);
        list.add(new Condition(Operator.EQ, true, "leaf", true));
        return getDescendants(definitionId, startNodeId, list);
    }

    public Node getOne(Long definition, String nodeId) {
        return nodeRepository.findNodeByDefinitionAndId(definition, nodeId);
    }

    @SuppressWarnings("unchecked")
    private List<Node> process(Pair<String, Map<String, Object>> queryPair) {
        Query query = entityManager.createNativeQuery(queryPair.getFirst(), Node.class);
        queryPair.getSecond().forEach(query::setParameter);
        return query.getResultList();
    }
}
