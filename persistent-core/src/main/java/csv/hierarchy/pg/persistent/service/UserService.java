package csv.hierarchy.pg.persistent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.domain.NodeAccess;
import csv.hierarchy.pg.persistent.domain.UserScope;
import csv.hierarchy.pg.persistent.entity.User;
import csv.hierarchy.pg.persistent.repository.DefinitionRepository;
import csv.hierarchy.pg.persistent.repository.NodeRepository;
import csv.hierarchy.pg.persistent.repository.UserRepository;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final DefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    @Getter(lazy = true)
    private final JsonNode emptyNode = ((Supplier<JsonNode>) objectMapper::createArrayNode).get();

    public User save(Long userId, Long hierarchyId, @NonNull UserScope scope, @NonNull Set<String> cascade, @NonNull Set<String> explicit) {
        Long definitionId = definitionRepository.getCurrentDefinition(hierarchyId);
        User user = userRepository.findOneByUserIdAndDefinitionId(userId, definitionId)
                .orElse(User.builder().userId(userId).hierarchyId(hierarchyId).build());
        user.setDefinitionId(definitionId);
        user.setScope(scope.getNum());
        switch (scope) {
            case NONE -> {
                user.setCascade(getEmptyNode());
                user.setEntire(getEmptyNode());
                user.setExplicit(getEmptyNode());
            }
            case ALL -> {
                Set<String> ids = nodeRepository.findNodesByDefinition(definitionId)
                        .stream().map(Node::getId).collect(Collectors.toSet());
                String rootId = nodeRepository.findRootId(definitionId);
                user.setCascade(objectMapper.valueToTree(List.of(rootId)));
                user.setEntire(objectMapper.valueToTree(ids));
                user.setExplicit(getEmptyNode());
            }
            case SPECIFIC -> setSpecificValue(user, definitionId, cascade, explicit);
        }
        return userRepository.save(user);
    }

    void setSpecificValue(User user, Long definitionId, Set<String> cascade, Set<String> explicit) {
        // validate
        Set<String> allNodes = nodeRepository.findNodesByDefinition(definitionId)
                .stream().map(Node::getId).collect(Collectors.toSet());
        var except = cascade.stream().filter(id -> !allNodes.contains(id)).toList();
        if (!except.isEmpty()) {
            throw new IllegalArgumentException("Unknown nodes in cascade: " + except);
        }
        except = explicit.stream().filter(id -> !allNodes.contains(id)).toList();
        if (!except.isEmpty()) {
            throw new IllegalArgumentException("Unknown nodes in explicit: " + except);
        }

        log.info("clean up the cascade / explicit");
        HashMap<String, List<String>> cascadeMap = new HashMap<>();
        cascade.forEach(id -> cascadeMap.put(id,
                nodeRepository.findDescendants(definitionId, id).stream().map(Node::getId).toList()));
        List<String> cascadeToDelete = cascadeMap.keySet().stream().filter(key -> {
            for (Map.Entry<String, List<String>> entry : cascadeMap.entrySet()) {
                if (!key.equals(entry.getKey()) && entry.getValue().contains(key)) {
                    return true;
                }
            }
            return false;
        }).toList();
        var filteredCascadeMap = cascadeMap.entrySet()
                .stream()
                .filter(entry -> !cascadeToDelete.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<String> allNodesFromCascade = new HashSet<>(filteredCascadeMap.keySet());
        allNodesFromCascade.addAll(filteredCascadeMap.values().stream().flatMap(Collection::stream).toList());
        List<String> explicitToDelete = explicit.stream().filter(allNodesFromCascade::contains).toList();
        Set<String> finalExplicit = new HashSet<>(explicit);
        explicitToDelete.forEach(finalExplicit::remove);
        Set<String> allCascadeNodes = new HashSet<>(allNodesFromCascade);
        allCascadeNodes.addAll(finalExplicit);
        // let's setup user
        user.setCascade(objectMapper.valueToTree(filteredCascadeMap.keySet()));
        user.setExplicit(objectMapper.valueToTree(finalExplicit));
        user.setEntire(objectMapper.valueToTree(allCascadeNodes));
        user.setUpdateTs(Timestamp.from(Instant.now()));
    }

    public NodeAccess userNodeAccess(Long userId, Long hierarchyId, String nodeId) {
        List<Boolean[]> booleans = userRepository.queryNodeByHierarchyId(userId, hierarchyId, nodeId);
        boolean hasAccess = false;
        boolean cascade = false;
        if (!booleans.isEmpty()) {
            var row = booleans.get(0);
            hasAccess = row[0];
            cascade = row[1];
        }
        if (hasAccess) {
            return cascade ? NodeAccess.CASCADE : NodeAccess.EXPLICIT;
        } else {
            return NodeAccess.NONE;
        }
    }
}
