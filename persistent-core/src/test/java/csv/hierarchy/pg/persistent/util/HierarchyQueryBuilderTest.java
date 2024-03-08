package csv.hierarchy.pg.persistent.util;

import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.Operator;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

@Slf4j
class HierarchyQueryBuilderTest {

    @Test
    void queryCondition() {
        var condition = new Condition(Operator.EQ,
                true, "level", "STORE");
        var params = new HashMap<String, Object>();
        StringBuilder sb = HierarchyQueryBuilder.fromCondition(condition, 1, params);
        Assertions.assertEquals(" level = :value1 ", sb.toString());
        MatcherAssert.assertThat(params, Matchers.hasEntry("value1", "STORE"));

        condition = new Condition(Operator.IN,
                false, "city", new String[]{"Toronto", "Detroit"});
        params = new HashMap<String, Object>();
        sb = HierarchyQueryBuilder.fromCondition(condition, 3, params);
        Assertions.assertEquals(" ext_data ->> :label3 IN (:value3) ", sb.toString());
        MatcherAssert.assertThat(params, Matchers.hasEntry("value3", new String[]{"Toronto", "Detroit"}));
        MatcherAssert.assertThat(params, Matchers.hasEntry("label3", "city"));

        condition = new Condition(Operator.NE,
                false, "location", "Detroit");
        params = new HashMap<String, Object>();
        sb = HierarchyQueryBuilder.fromCondition(condition, 5, params);
        Assertions.assertEquals(" ext_data ->> :label5 != :value5 ", sb.toString());
        MatcherAssert.assertThat(params, Matchers.hasEntry("value5", "Detroit"));
        MatcherAssert.assertThat(params, Matchers.hasEntry("label5", "location"));

        condition = new Condition(Operator.EQ,
                true, "leaf", true);
        params = new HashMap<String, Object>();
        sb = HierarchyQueryBuilder.fromCondition(condition, 5, params);
        Assertions.assertEquals(" leaf = :value5 ", sb.toString());
        MatcherAssert.assertThat(params, Matchers.hasEntry("value5", true));
        Assertions.assertEquals(1, params.size());
    }
}
