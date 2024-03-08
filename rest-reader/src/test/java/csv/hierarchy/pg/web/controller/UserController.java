package csv.hierarchy.pg.web.controller;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.TreeNode;
import csv.hierarchy.pg.persistent.service.UserConditionService;
import csv.hierarchy.pg.persistent.util.NodeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Log4j2
public class UserController {
    private final UserConditionService userConditionService;

    @PostMapping("/{userId}/hierarchies/{hierarchyId}/nodes/{startNodeId}")
    public List<TreeNode> getNodesStart(@PathVariable Long userId, @PathVariable Long hierarchyId, @PathVariable String startNodeId,
                                        @RequestBody(required = false) List<Condition> conditions) {
        log.info("looking up {}, {}, n {}, c {}", userId, hierarchyId, startNodeId, conditions);
        var nodes = (null == conditions || conditions.isEmpty()) ?
                userConditionService.getDescendantsWithConditions(userId, hierarchyId, startNodeId, conditions) :
                userConditionService.getDescendants(userId, hierarchyId, startNodeId);
        return NodeConverter.toTreeNodes(nodes);
    }

    @GetMapping("/{userId}/hierarchies/{hierarchyId}/nodes/{startNodeId}")
    public TreeNode getNodesStart(@PathVariable Long userId, @PathVariable Long hierarchyId, @PathVariable String startNodeId) {
        var nodes = userConditionService.getDescendants(userId, hierarchyId, startNodeId);
        return NodeConverter.toTreeNode(nodes);
    }

    @PostMapping("/{userId}/hierarchies/{hierarchyId}")
    public List<TreeNode> getNodes(@PathVariable Long userId, @PathVariable Long hierarchyId, @RequestBody(required = false) List<Condition> conditions) {
        var nodes = userConditionService.allUserNodesWithCondition(userId, hierarchyId, conditions);
        return NodeConverter.toTreeNodesComplex(nodes);
    }
}
