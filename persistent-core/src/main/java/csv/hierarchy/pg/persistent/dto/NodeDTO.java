package csv.hierarchy.pg.persistent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class NodeDTO {
    private String id;
    private String level;
    private Integer depth;
    private String name;
    private String parentId;
    private boolean leaf;
    private JsonNode extData;
}
