package csv.hierarchy.pg.persistent.functional;

import csv.hierarchy.pg.persistent.domain.CsvRowNode;
import csv.hierarchy.pg.persistent.domain.NoRetryableException;
import csv.hierarchy.pg.persistent.util.CsvRowUtil;
import csv.hierarchy.pg.persistent.util.FileUtil;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Log4j2
class CsvValidatorTest {
    private List<CsvRowNode> loadAndValidate(String fileName) {
        File file = FileUtil.getFile(fileName);
        List<CsvRowNode> rowNodes = new LinkedList<>();
        Map<String, String> lookupMap = Map.of("id", "id",
                "parent_id", "parent_id");
        FileUtil.processCsv(file, rowMap -> rowNodes.add(CsvRowUtil.getCsvRowNode(rowMap, lookupMap)));
        CsvRowUtil.validateAndSort(rowNodes);
        return rowNodes;
    }

    @Test
    void testNoExists() {
        var ex = Assertions.assertThrows(FileNotFoundException.class,
                () -> loadAndValidate("not_exists"));
        log.info(ex);
    }

    @Test
    void testEmpty() {
        List<CsvRowNode> nodes = loadAndValidate("empty.csv");
        Assertions.assertTrue(nodes.isEmpty());
    }

    @Test
    void testHeaderOnly() {
        List<CsvRowNode> nodes = loadAndValidate("headerOnly.csv");
        Assertions.assertTrue(nodes.isEmpty());
    }

    @Test
    void testSingleRow() {
        List<CsvRowNode> nodes = loadAndValidate("singleRow.csv");
        Assertions.assertEquals(1, nodes.size());
    }

    @Test
    void testTwoRoots() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("twoRoots.csv"));
        log.info(ex);
    }

    @Test
    void testDuplicates() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("duplicates.csv"));
        log.info(ex);
    }

    @Test
    void testRecursive() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("recursive.csv"));
        log.info(ex);
    }

    @Test
    void testMissingParent() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("missingParent.csv"));
        log.info(ex);
    }

    @Test
    void testMissingId() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("missingId.csv"));
        log.info(ex);
    }

    @Test
    void testBadIds() {
        var ex = Assertions.assertThrows(NoRetryableException.class,
                () -> loadAndValidate("badIds.csv"));
        log.info(ex);
    }

    @Test
    void reMapping() {
        String fileName = "remap.csv";
        File file = FileUtil.getFile(fileName);
        List<CsvRowNode> rowNodes = new LinkedList<>();
        Map<String, String> lookupMap = Map.of("id", "iD",
                "parent_id", "parEnT", "level", "LeVEl", "name", "Unit naMe");
        FileUtil.validate(file, lookupMap);
        FileUtil.processCsv(file, rowMap -> rowNodes.add(CsvRowUtil.getCsvRowNode(rowMap, lookupMap)));
        Assertions.assertEquals(5, rowNodes.size());
    }
}
