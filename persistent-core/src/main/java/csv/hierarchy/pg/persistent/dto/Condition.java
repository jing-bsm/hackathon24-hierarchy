package csv.hierarchy.pg.persistent.dto;

import java.util.List;

public record Condition(Operator operator, Boolean isMain, String label, Object value) {
    public boolean isValid() {
        if (operator == null || isMain == null || label == null || value == null) {
            return false;
        }
        // todo main only allow certain label
        if (operator == Operator.IN) {
            return value instanceof List;
        } else return value instanceof String || value instanceof Boolean || value instanceof Number;
    }
}

