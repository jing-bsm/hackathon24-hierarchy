package csv.hierarchy.pg.persistent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {
    private NodeDTO node;
    private Set<TreeNode> children;
}
