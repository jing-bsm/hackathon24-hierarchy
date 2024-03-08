package csv.hierarchy.pg.persistent.util;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.Operator;
import lombok.experimental.UtilityClass;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class HierarchyQueryBuilder {
    static final String SPACE = " ";
    static final String ATTRIBUTE_COLUMN = "ext_data";
    static final String PARAM_PREFIX = ":";
    static final String PARAM_LABEL = "label";
    static final String PARAM_VALUE = "value";

    public Pair<List<StringBuilder>, Map<String, Object>> getWhereClause(List<Condition> conditions) {
        List<Condition> inValidList = conditions.stream().filter(c -> !c.isValid()).toList();
        if (!inValidList.isEmpty()) {
            throw new IllegalArgumentException("Invalid Conditions" + inValidList);
        }
        var list = new ArrayList<>(conditions);
        list.sort((Condition o1, Condition o2) -> {
            // compare the types lexicographically
            int typeComparison = o1.isMain().compareTo(o2.isMain());
            // if the types are equal, compare the operators lexicographically
            if (typeComparison == 0) {
                int opComparison = o1.operator().compareTo(o2.operator());
                if (opComparison == 0) {
                    return o1.label().compareTo(o2.label());
                } else {
                    return opComparison;
                }
                // otherwise, return the type comparison result
            } else {
                return typeComparison;
            }
        });
        var params = new HashMap<String, Object>();
        var queries = new ArrayList<StringBuilder>(list.size());
        for (int i = 0; i < list.size(); i++) {
            queries.add(fromCondition(list.get(i), i, params));
        }
        return Pair.of(queries, params);
    }

    public Pair<String, Map<String, Object>> getQueryPair(Long definitionId, String startNodeId, List<Condition> conditions) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM node
                WHERE definition_id = :definitionId
                AND tree <@ (
                    select tree from node where definition_id = :definitionId and id = :startNodeId
                )
                """);
        var pair = getWhereClause(conditions);
        pair.getFirst().forEach(q -> sb.append(" AND ").append(q));
        Map<String, Object> map = pair.getSecond();
        map.put("definitionId", definitionId);
        map.put("startNodeId", startNodeId);
        return Pair.of(sb.toString(), map);
    }

    public Pair<String, Map<String, Object>> getQueryPairSingle(Long definitionId, String singleNodeId, List<Condition> conditions) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM node
                WHERE definition_id = :definitionId
                AND id = :nodeId
                """);
        var pair = getWhereClause(conditions);
        pair.getFirst().forEach(q -> sb.append(" AND ").append(q));
        Map<String, Object> map = pair.getSecond();
        map.put("definitionId", definitionId);
        map.put("nodeId", singleNodeId);
        return Pair.of(sb.toString(), map);
    }

    public StringBuilder fromCondition(Condition condition, int position, Map<String, Object> queryParams) {
        StringBuilder sb = new StringBuilder(SPACE);
        if (Boolean.TRUE.equals(condition.isMain())) {
            sb.append(condition.label());
        } else {
            queryParams.put(PARAM_LABEL + position, condition.label());
            sb.append(ATTRIBUTE_COLUMN).append(" ->> ").append(PARAM_PREFIX).append(PARAM_LABEL).append(position);
        }
        if (condition.operator() == Operator.IN) {
            sb.append(" IN ").append("(").append(PARAM_PREFIX).append(PARAM_VALUE)
                    .append(position).append(")").append(SPACE);
        } else {
            if (condition.operator() == Operator.EQ) {
                sb.append(" = ");
            } else if (condition.operator() == Operator.NE) {
                sb.append(" != ");
            }
            sb.append(PARAM_PREFIX).append(PARAM_VALUE).append(position).append(SPACE);
        }
        queryParams.put(PARAM_VALUE + position, condition.value());
        return sb;
    }
}
