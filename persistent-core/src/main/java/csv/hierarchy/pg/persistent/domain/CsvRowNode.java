package csv.hierarchy.pg.persistent.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Log4j2
@EqualsAndHashCode(callSuper = true)
public class CsvRowNode extends SimpleTreeNode implements Serializable {
    private String name;
    private String level;
    private boolean leaf;
    private String tree;
    private Map<String, String> extData;
}
