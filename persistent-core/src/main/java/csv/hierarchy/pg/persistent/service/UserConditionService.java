package csv.hierarchy.pg.persistent.service;

import csv.hierarchy.pg.persistent.domain.NodeAccess;
import csv.hierarchy.pg.persistent.domain.UserScope;
import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.entity.User;
import csv.hierarchy.pg.persistent.repository.DefinitionRepository;
import csv.hierarchy.pg.persistent.repository.NodeRepository;
import csv.hierarchy.pg.persistent.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserConditionService {
    private final NodeConditionService nodeConditionService;
    private final DefinitionRepository definitionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;

    public List<Node> getDescendantsWithConditions(@NonNull Long userId, @NonNull Long hierarchyId, @NonNull String startNodeId, @NonNull List<Condition> conditions) {
        Long definition = definitionRepository.getCurrentDefinition(hierarchyId);
        NodeAccess nodeAccess = userService.userNodeAccess(userId, hierarchyId, startNodeId);
        switch (nodeAccess) {
            case EXPLICIT -> {
                return nodeConditionService.getSingle(definition, startNodeId, conditions);
            }
            case CASCADE -> {
                return nodeConditionService.getDescendants(definition, startNodeId, conditions);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    public List<Node> getDescendants(@NonNull Long userId, @NonNull Long hierarchyId, @NonNull String startNodeId) {
        Long definition = definitionRepository.getCurrentDefinition(hierarchyId);
        NodeAccess nodeAccess = userService.userNodeAccess(userId, hierarchyId, startNodeId);
        switch (nodeAccess) {
            case EXPLICIT -> {
                return Collections.singletonList(nodeConditionService.getOne(definition, startNodeId));
            }
            case CASCADE -> {
                return nodeRepository.findDescendants(definition, startNodeId);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    private User findUser(Long userId, Long hierarchyId) {
        Long currentDefinition = definitionRepository.getCurrentDefinition(hierarchyId);
        return userRepository.findOneByUserIdAndDefinitionId(userId, currentDefinition).orElseThrow(NoSuchElementException::new);
    }

    public List<List<Node>> allUserNodesWithCondition(Long userId, Long hierarchyId, List<Condition> nullableConditions) {
        User user = findUser(userId, hierarchyId);
        Long definition = user.getDefinitionId();
        List<Condition> conditionList = null == nullableConditions ? Collections.emptyList() : nullableConditions;
        String rootId = nodeRepository.findRootId(definition);
        if (user.getScope() == UserScope.ALL.getNum()) {
            return Collections.singletonList(nodeConditionService.getDescendants(definition, rootId, conditionList));
        } else if (user.getScope() == UserScope.NONE.getNum()) {
            return Collections.emptyList();
        }
        List<List<Node>> nodes = new ArrayList<>();
        user.getCascade().forEach(node ->
                nodes.add(nodeConditionService.getDescendants(definition, node.asText(), conditionList)));
        user.getExplicit().forEach(node ->
                nodes.add(nodeConditionService.getSingle(definition, node.asText(), conditionList)));
        nodes.sort(Comparator.<List<?>>comparingInt(List::size).reversed());
        return nodes;
    }

    public List<Node> allUserNodes(Long userId, Long hierarchyId) {
        User user = findUser(userId, hierarchyId);
        Long definition = user.getDefinitionId();
        if (user.getScope() == UserScope.ALL.getNum()) {
            return nodeRepository.findNodesByDefinition(definition);
        } else if (user.getScope() == UserScope.NONE.getNum()) {
            return Collections.emptyList();
        }
        List<Node> nodes = new ArrayList<>();
        user.getCascade().forEach(node ->
                nodes.addAll(nodeRepository.findDescendants(definition, node.asText())));
        user.getExplicit().forEach(node ->
                nodes.add(nodeRepository.findNodeByDefinitionAndId(definition, node.asText())));
        return nodes;
    }

    public List<Node> getAncestors(Long userId, Long hierarchyId, String nodeId) {
        NodeAccess nodeAccess = userService.userNodeAccess(userId, hierarchyId, nodeId);
        return nodeAccess == NodeAccess.NONE ? Collections.emptyList() :
                nodeRepository.findAncestors(definitionRepository.getCurrentDefinition(hierarchyId), nodeId);

    }
}
