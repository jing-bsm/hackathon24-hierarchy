package csv.hierarchy.pg.persistent.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import csv.hierarchy.pg.persistent.TestUtils;
import csv.hierarchy.pg.persistent.domain.NodeAccess;
import csv.hierarchy.pg.persistent.domain.UserScope;
import csv.hierarchy.pg.persistent.dto.Condition;
import csv.hierarchy.pg.persistent.dto.Operator;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.repository.DefinitionRepository;
import csv.hierarchy.pg.persistent.repository.NodeRepository;
import csv.hierarchy.pg.persistent.repository.UserRepository;
import csv.hierarchy.pg.persistent.service.*;
import csv.hierarchy.pg.persistent.util.FileUtil;
import csv.hierarchy.pg.persistent.util.NodeConverter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
@Log4j2
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CsvNodeUserTest {
    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DefinitionRepository definitionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NodeConditionService nodeConditionService;

    @Autowired
    private UserConditionService userConditionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WrapService wrapService;

    private final Long clientId = 5L;
    private final Long hierarchyId = 4L;
    private final Long hierarchyId2 = 5L;
    private final Long userId = 105L;
    private final Long userId2 = userId + 1;
    private final Long userId3 = userId + 2;
    private final Long userId4 = userId + 3;

    @BeforeAll
    static void beforeAll() {
        TestUtils.initialDocker();
    }

    private static final Map<String, String> lookupMap = Map.of("id", "id",
            "level", "level",
            "name", "store_name",
            "parent_id", "parent_id");

    private Long save2Db(Long hierarchyId) {
        return wrapService.transactionSupplier(() ->
                nodeService.save(clientId, hierarchyId, Instant.now(), lookupMap,
                        nodeService.getAndValidate(lookupMap, FileUtil.getFile("simple.csv"))));
    }

    @Test
    @Order(1)
    void nodeSaveANewVersion() {
        Long definitionId = save2Db(hierarchyId);
        Assertions.assertEquals(1L, definitionId);
    }

    @SneakyThrows
    @Order(4)
    @Test
    void nodeQuery() {
        var definitionId = definitionRepository.getDefinitionId(hierarchyId, new Timestamp(System.currentTimeMillis()));
        List<Node> nodes = nodeRepository.findAncestors(definitionId, "3");
        Assertions.assertEquals(3, nodes.size());

        nodes = nodeRepository.findDescendants(definitionId, "R_4");
        Assertions.assertEquals(3, nodes.size());

        nodes = nodeRepository.findNodesByDefinition(definitionId);
        Assertions.assertEquals(11L, nodes.size());

        nodes = nodeRepository.findLeaves(definitionId);
        Assertions.assertEquals(8, nodes.size());

        Map<String, String> map = Map.of("email", "abc@123.com", "store_name", "Central Stores");
        nodes = nodeRepository.findByJsonMap(definitionId, objectMapper.writeValueAsString(map));
        Assertions.assertEquals(1L, nodes.size());
    }

    @Test
    @Order(10)
    void userSaveAndQuery() {
        Set<String> cascadeSet = Set.of("R_4", "14");
        userService.save(userId, hierarchyId, UserScope.SPECIFIC, cascadeSet, Set.of("1", "4"));

        Assertions.assertEquals(NodeAccess.NONE, userService.userNodeAccess(userId, hierarchyId, "JATN"));
        Assertions.assertEquals(NodeAccess.CASCADE, userService.userNodeAccess(userId, hierarchyId, "14"));
        Assertions.assertEquals(NodeAccess.EXPLICIT, userService.userNodeAccess(userId, hierarchyId, "4"));
        Assertions.assertEquals(NodeAccess.NONE, userService.userNodeAccess(userId, hierarchyId, "C_1"));
        Assertions.assertEquals(NodeAccess.CASCADE, userService.userNodeAccess(userId, hierarchyId, "1"));


        userService.save(userId2, hierarchyId, UserScope.ALL, Collections.emptySet(), cascadeSet);
        Assertions.assertEquals(NodeAccess.CASCADE, userService.userNodeAccess(userId2, hierarchyId, "14"));
        Assertions.assertEquals(NodeAccess.CASCADE, userService.userNodeAccess(userId2, hierarchyId, "R_2"));
        Assertions.assertEquals(NodeAccess.CASCADE, userService.userNodeAccess(userId2, hierarchyId, "C_1"));
        Assertions.assertEquals(NodeAccess.NONE, userService.userNodeAccess(userId2, hierarchyId, "JATN")); // not in the tree

        userService.save(userId3, hierarchyId, UserScope.NONE, cascadeSet, cascadeSet);
        Assertions.assertEquals(NodeAccess.NONE, userService.userNodeAccess(userId3, hierarchyId, "14"));
        Assertions.assertEquals(NodeAccess.NONE, userService.userNodeAccess(userId3, hierarchyId, "R_4"));

    }

    @Test
    @Order(20)
    void nodeConditionQuery() {
        MatcherAssert.assertThat(nodeConditionService.getDescendantsByHierarchy(hierarchyId, "C_1", Collections.emptyList()),
                Matchers.containsInAnyOrder(nodeConditionService.getDescendantsByHierarchy(hierarchyId, "C_1").toArray()));

        MatcherAssert.assertThat(nodeConditionService.getDescendants(1L, "R_4", Collections.emptyList()),
                Matchers.containsInAnyOrder(nodeConditionService.getDescendantsByHierarchy(hierarchyId, "R_4").toArray()));

        var leaves1 = nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.EQ, true, "level", "STORE")));
        Assertions.assertEquals(8, leaves1.size());

        var leaves2 = nodeConditionService.getLeaves(1L, "C_1", Collections.emptyList());
        MatcherAssert.assertThat(leaves1, Matchers.containsInAnyOrder(leaves2.toArray()));


        Assertions.assertEquals(0, nodeConditionService.getLeaves(1L, "C_1",
                List.of(new Condition(Operator.EQ, true, "level", "REGION"))).size());

        var region1 = nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.EQ, true, "level", "REGION")));
        Assertions.assertEquals(2, region1.size());
        var region2 = nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.EQ, true, "depth", 1)));
        Assertions.assertIterableEquals(region1, region2);

        Assertions.assertEquals(3, nodeConditionService.getLeaves(1L, "C_1",
                List.of(new Condition(Operator.EQ, false, "store_zip", "19466"))).size());

        Assertions.assertEquals(2, nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.IN, false, "store_city", List.of("Aventura", "Phoenix")))).size());

        Assertions.assertEquals(2, nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.IN, false, "store_city", List.of("Phoenix", "Houston")),
                        new Condition(Operator.EQ, false, "store_mgr_name", "Chris Matulovich"))).size());

        Assertions.assertEquals(2, nodeConditionService.getDescendants(1L, "C_1",
                List.of(new Condition(Operator.IN, false, "store_city", List.of("Phoenix", "Houston")),
                        new Condition(Operator.NE, false, "store_mgr_name", "Chris Matulovich"))).size());

        Assertions.assertEquals(1L, nodeConditionService.getDescendants(1L, "R_2",
                List.of(new Condition(Operator.EQ, false, "store_city", "Chicago"))).size());
    }

    @Test
    @Order(30)
    void userNodeConditionQuery() {
        List<Node> nodes = userConditionService.allUserNodes(userId, hierarchyId);
        Assertions.assertEquals(5, nodes.size());
        nodes = userConditionService.allUserNodes(userId2, hierarchyId);
        Assertions.assertEquals(11L, nodes.size());
        nodes = userConditionService.allUserNodes(userId3, hierarchyId);
        Assertions.assertEquals(0, nodes.size());

        Assertions.assertTrue(userConditionService.getDescendantsWithConditions(userId, hierarchyId,
                "R_2", Collections.emptyList()).isEmpty());
        Assertions.assertEquals(3, userConditionService.getDescendantsWithConditions(userId, hierarchyId,
                "R_4", Collections.emptyList()).size());
        Assertions.assertTrue(userConditionService.getDescendantsWithConditions(userId, hierarchyId,
                "C_1", Collections.emptyList()).isEmpty());
        Assertions.assertEquals(1L, userConditionService.getDescendantsWithConditions(userId, hierarchyId,
                "14", Collections.emptyList()).size());

        Assertions.assertEquals(2, userConditionService.getDescendantsWithConditions(userId, hierarchyId,
                "R_4", List.of(new Condition(Operator.NE, true, "depth", 1))).size());

        Assertions.assertEquals(9, userConditionService.getDescendantsWithConditions(userId2, hierarchyId,
                "C_1", List.of(new Condition(Operator.NE, true, "depth", 1))).size());

        Assertions.assertEquals(3, userConditionService.getDescendantsWithConditions(userId2, hierarchyId,
                "R_2", List.of(new Condition(Operator.EQ, false, "store_city", "McLean"))).size());

        Assertions.assertEquals(2, userConditionService.getDescendantsWithConditions(userId2, hierarchyId,
                "R_2", List.of(new Condition(Operator.EQ, false, "store_city", "McLean"),
                        new Condition(Operator.EQ, false, "email", "abc@123.com"),
                        new Condition(Operator.EQ, true, "depth", 2))).size());

        Assertions.assertTrue(userConditionService.getDescendantsWithConditions(userId3, hierarchyId,
                "14", Collections.emptyList()).isEmpty());

        log.info("No start Node");
        Assertions.assertTrue(userConditionService.allUserNodesWithCondition(userId3, hierarchyId,
                Collections.emptyList()).isEmpty());

        Assertions.assertEquals(1L, userConditionService.allUserNodesWithCondition(userId2, hierarchyId,
                Collections.emptyList()).size());
        List<List<Node>> lists = userConditionService.allUserNodesWithCondition(userId, hierarchyId, Collections.emptyList());
        Assertions.assertEquals(3, lists.size());
        Assertions.assertIterableEquals(lists.stream().map(List::size).toList(), List.of(3, 1, 1));

        var treeNodes = NodeConverter.toTreeNodesComplex(lists);
        Assertions.assertEquals(3, NodeConverter.toTreeNodesComplex(lists).size());

        log.info("This filter makes orphan nodes");
        lists = userConditionService.allUserNodesWithCondition(userId, hierarchyId,
                List.of(new Condition(Operator.EQ, false, "store_type", "Retail")));
        Assertions.assertIterableEquals(lists.stream().map(List::size).toList(), List.of(2, 1, 1));
        Assertions.assertEquals(4, NodeConverter.toTreeNodesComplex(lists).size());

        lists = userConditionService.allUserNodesWithCondition(userId2, hierarchyId,
                List.of(new Condition(Operator.EQ, true, "parent_id", "R_2")));
        Assertions.assertIterableEquals(lists.stream().map(List::size).toList(), List.of(6));
    }

    @Test
    @Order(40)
    void multiVersions() {
        save2Db(hierarchyId2);
        Set<String> cascadeSet = Set.of("R_2");
        userService.save(userId4, hierarchyId2, UserScope.SPECIFIC, cascadeSet, Set.of("1", "4", "14"));
        Assertions.assertTrue(userRepository.findAll().size() >= 4);
        Assertions.assertEquals(8, userConditionService.allUserNodes(userId4, hierarchyId2).size());

        var definitionId = save2Db(hierarchyId2);
        Assertions.assertTrue(definitionId >= 2);
        Assertions.assertTrue(definitionRepository.findAll().size() >= 2);
        cascadeSet = Set.of("R_4", "15");
        userService.save(userId4, hierarchyId2, UserScope.SPECIFIC, cascadeSet, Set.of("1", "4"));
        Assertions.assertEquals(5, userConditionService.allUserNodes(userId4, hierarchyId2).size());
    }
}
