package csv.hierarchy.pg.persistent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A generic simple tree node structure.
 *
 * @author jingx
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public class SimpleTreeNode implements Serializable {
    protected String id;
    protected String parentId;
    protected int depth;

    public static void sort(List<? extends SimpleTreeNode> nodeList) {
        // sort
        Queue<SimpleTreeNode> queue = new LinkedList<>();
        // Find the root node(s)
        for (SimpleTreeNode node : nodeList) {
            if (node.parentId.isEmpty()) {
                queue.add(node);
                node.depth = 0;
            }
        }
        int maxDepth = 0;
        // Perform breadth-first traversal
        while (!queue.isEmpty()) {
            SimpleTreeNode currentNode = queue.poll();
            for (SimpleTreeNode child : nodeList) {
                if (child.parentId.equals(currentNode.id)) {
                    child.depth = currentNode.depth + 1;
                    maxDepth = Math.max(maxDepth, child.depth);
                    queue.add(child);
                }
            }
        }
        // Sort the list based on depth
        nodeList.sort(Comparator.comparingInt(node -> node.depth));
    }

    public static List<String> validate(List<? extends SimpleTreeNode> nodeList) {
        boolean rootNodeAssert = nodeList.stream().filter(node -> node.parentId.isEmpty()).count() == 1;
        var idSet = nodeList.stream().map(SimpleTreeNode::getId).collect(Collectors.toSet());
        boolean idAssert = idSet.size() == nodeList.size();
        var pidSet = nodeList.stream().map(SimpleTreeNode::getParentId)
                .filter(id -> !id.isEmpty() && !idSet.contains(id)).toList();
        List<String> errors = new ArrayList<>();
        hasRecursiveNodes(nodeList, errors);
        if (rootNodeAssert && idAssert && pidSet.isEmpty() && errors.isEmpty()) {
            return errors;
        }

        // wrap it for deletion
        var removableSet = new HashSet<>(idSet);
        nodeList.forEach(node -> {
            if (!rootNodeAssert && node.parentId.isEmpty()) {
                errors.add("Root: " + node.id);
            }
            if (!idAssert && !removableSet.remove(node.id)) {
                errors.add("Duplicate id: " + node.id);
            }
        });
        pidSet.forEach(pid -> errors.add("ParentId obsolete: " + pid));
        return errors;
    }

    static void hasRecursiveNodes(List<? extends SimpleTreeNode> nodeList, List<String> errors) {
        Map<String, List<SimpleTreeNode>> nodeMap = new HashMap<>();

        // Create a map to group nodes by their parentId
        for (SimpleTreeNode node : nodeList) {
            nodeMap.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
        }
        // Perform DFS traversal
        for (SimpleTreeNode node : nodeList) {
            if (hasRecursiveNodesDFS(node, nodeMap, new HashSet<>())) {
                errors.add("Has recursive node id: " + node.id);
            }
        }
    }

    private static boolean hasRecursiveNodesDFS(SimpleTreeNode currentNode, Map<String, List<SimpleTreeNode>> nodeMap, Set<String> visitedNodes) {
        visitedNodes.add(currentNode.getId());
        for (SimpleTreeNode childNode : nodeMap.getOrDefault(currentNode.getId(), Collections.emptyList())) {
            if (visitedNodes.contains(childNode.getId()) || hasRecursiveNodesDFS(childNode, nodeMap, visitedNodes)) {
                return true;
            }
        }
        return false;
    }
}
