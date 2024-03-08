package csv.hierarchy.pg.persistent.functional;

import csv.hierarchy.pg.persistent.domain.CsvRowNode;
import csv.hierarchy.pg.persistent.util.CsvRowUtil;
import csv.hierarchy.pg.persistent.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class SaveToFile {

    @Test
    void reMapping() {
        String fileName = "TestGraybarTimeout.csv";
        File file = FileUtil.getFile(fileName);
        List<CsvRowNode> rowNodes = new LinkedList<>();
        Map<String, String> lookupMap = Map.of("id", "iD",
                "parent_id", "parEnT", "level", "LeVEl", "name", "Unit naMe");
        FileUtil.validate(file, lookupMap);
        FileUtil.processCsv(file, rowMap -> rowNodes.add(CsvRowUtil.getCsvRowNode(rowMap, lookupMap)));
        File file1 = CsvRowUtil.toTemporallyFile(rowNodes);
        System.out.println(file1.getAbsoluteFile());
    }
}
