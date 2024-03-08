package csv.hierarchy.pg.web.controller;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.NodeDTO;
import csv.hierarchy.pg.persistent.dto.TreeNode;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.service.NodeConditionService;
import csv.hierarchy.pg.persistent.util.NodeConverter;
import csv.hierarchy.pg.web.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hierarchies")
@RequiredArgsConstructor
public class NodeController {
    private final NodeConditionService nodeConditionService;

    @PostMapping("/{hierarchyId}/nodes/{startNodeId}")
    public Page<NodeDTO> getTreeNodes(@PathVariable Long hierarchyId, @PathVariable String startNodeId,
                                      @RequestBody(required = false) List<Condition> conditions,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        var nodes = nodeConditionService.getDescendantsByHierarchy(hierarchyId, startNodeId, conditions);
        return WebUtils.paginate(nodes.stream().map(NodeConverter::toNodeDTO).toList(), PageRequest.of(page, size));
    }

    @GetMapping("/{hierarchyId}/nodes/tree")
    public TreeNode getTreeNodes(@PathVariable Long hierarchyId,
                                 @RequestParam(required = false) String startNodeId) {
        return NodeConverter.toTreeNode(getNodes(hierarchyId, startNodeId));
    }

    @GetMapping("/{hierarchyId}/nodes")
    public Page<NodeDTO> getListPaged(@PathVariable Long hierarchyId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return WebUtils.paginate(getNodes(hierarchyId, null).stream().map(NodeConverter::toNodeDTO).toList(),
                PageRequest.of(page, size));
    }

    @GetMapping("/{hierarchyId}/leaves")
    public List<NodeDTO> getLeaves(@PathVariable Long hierarchyId,
                                   @RequestParam(required = false) String startNodeId) {
        return getNodes(hierarchyId, startNodeId).stream()
                .filter(Node::isLeaf)
                .map(NodeConverter::toNodeDTO).toList();
    }

    @GetMapping("/{hierarchyId}/attributes/{attribute}")
    public List<String> getAttributes(@PathVariable Long hierarchyId, @PathVariable String attribute,
                                      @RequestParam(required = false) String startNodeId) {
        return getNodes(hierarchyId, startNodeId).stream()
                .map(Node::getExtData).map(json -> json.get(attribute).asText())
                .distinct()
                .toList();
    }

    private List<Node> getNodes(Long hierarchyId, @Nullable String startNodeId) {
        return Strings.isEmpty(startNodeId) ?
                nodeConditionService.getNodes(hierarchyId) :
                nodeConditionService.getDescendantsByHierarchy(hierarchyId, startNodeId);
    }
}
