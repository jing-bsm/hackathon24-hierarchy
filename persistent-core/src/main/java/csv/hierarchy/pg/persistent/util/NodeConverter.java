package csv.hierarchy.pg.persistent.util;

import csv.hierarchy.pg.persistent.dto.NodeDTO;
import csv.hierarchy.pg.persistent.dto.TreeNode;
import csv.hierarchy.pg.persistent.entity.Node;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class NodeConverter {

    public TreeNode toTreeNode(List<Node> nodeList) {
        return fill(nodeList).values().stream()
                .min(Comparator.comparingInt(t -> t.getNode().getDepth())).orElse(null);
    }

    public List<TreeNode> toTreeNodes(List<Node> nodeList) {
        // some parent of nodes may be filtered by conditions, resulting orphans. so we rebuild the tree.
        Map<String, TreeNode> map = fill(nodeList);
        var list = new ArrayList<>(map.values().stream().filter(t -> !map.containsKey(t.getNode().getParentId())).toList());
        list.sort(Comparator.comparingInt(t -> ((TreeNode) t).getChildren().size()).reversed());
        return list;
    }

    public List<TreeNode> toTreeNodesComplex(List<List<Node>> nodeList) {
        return NodeConverter.toTreeNodes(nodeList.stream().flatMap(List::stream).toList());
    }

    private Map<String, TreeNode> fill(List<Node> nodeList) {
        if (null == nodeList || nodeList.isEmpty()) {
            return Collections.emptyMap();
        } else if (nodeList.size() == 1) {
            Node node = nodeList.get(0);
            return Map.of(node.getId(), new TreeNode(toNodeDTO(node), Collections.emptySet()));
        }
        Map<String, TreeNode> map = nodeList.stream().map(NodeConverter::toNodeDTO)
                .collect(Collectors.toMap(NodeDTO::getId, node -> new TreeNode(node, new HashSet<>())));
        map.values().forEach(treeNode -> {
            if (treeNode.getNode().getDepth() != 0) {
                TreeNode parentNode = map.get(treeNode.getNode().getParentId());
                if (parentNode != null) { // possible a subtree and parent is not present
                    parentNode.getChildren().add(treeNode);
                }
            }
        });
        return map;
    }

    public NodeDTO toNodeDTO(Node node) {
        NodeDTO nodeDTO = new NodeDTO();
        BeanUtils.copyProperties(node, nodeDTO);
        return nodeDTO;
    }
}
