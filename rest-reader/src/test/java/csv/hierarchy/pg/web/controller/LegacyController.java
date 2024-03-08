package csv.hierarchy.pg.web.controller;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.NodeDTO;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.service.UserConditionService;
import csv.hierarchy.pg.persistent.util.NodeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/legacy")
@RequiredArgsConstructor
public class LegacyController {
    // may need do some mapping TODO
    private final UserConditionService userConditionService;

    @GetMapping("/{userId}/hierarchies/{hierarchyId}/attributes/{attribute}")
    public List<String> getAttributes(@PathVariable Long userId, @PathVariable Long hierarchyId, @PathVariable String attribute) {
        return userConditionService.allUserNodes(userId, hierarchyId).stream()
                .map(node -> node.getExtData().get(attribute).asText()).distinct().toList();
    }

    @GetMapping("/{userId}/hierarchies/{hierarchyId}/ancestry/{nodeId}")
    public List<NodeDTO> getAncestries(@PathVariable Long userId, @PathVariable Long hierarchyId, @PathVariable String nodeId) {
        return userConditionService.getAncestors(userId, hierarchyId, nodeId).stream().map(NodeConverter::toNodeDTO).toList();
    }

    @GetMapping("/{userId}/hierarchies/{hierarchyId}/leaves")
    public List<NodeDTO> getLeaves(@PathVariable Long userId, @PathVariable Long hierarchyId,
                                   @RequestParam(required = false) String startNodeId, @RequestBody(required = false) List<Condition> conditions) {
        return getDescendants(userId, hierarchyId, startNodeId, conditions).stream().map(NodeConverter::toNodeDTO)
                .filter(NodeDTO::isLeaf).toList();
    }

    @PostMapping("/{userId}/hierarchies/{hierarchyId}/vertices")
    public List<NodeDTO> geVertices(@PathVariable Long userId, @PathVariable Long hierarchyId,
                                    @RequestParam(required = false) String startNodeId, @RequestBody(required = false) List<Condition> conditions) {
        // todo may need change the logics
        return getDescendants(userId, hierarchyId, startNodeId, conditions).stream().map(NodeConverter::toNodeDTO).toList();
    }

    private List<Node> getDescendants(Long userId, Long hierarchyId, String startNodeId, List<Condition> conditions) {
        if (startNodeId == null) {
            return userConditionService.allUserNodesWithCondition(userId, hierarchyId, conditions).stream().flatMap(List::stream).toList();
        } else if (conditions == null || conditions.isEmpty()) {
            return userConditionService.getDescendants(userId, hierarchyId, startNodeId);
        }
        return userConditionService.getDescendantsWithConditions(userId, hierarchyId, startNodeId, conditions);
    }
}
