package csv.hierarchy.pg.persistent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import csv.hierarchy.pg.persistent.domain.CsvRowNode;
import csv.hierarchy.pg.persistent.domain.NoRetryableException;
import csv.hierarchy.pg.persistent.entity.Node;
import csv.hierarchy.pg.persistent.repository.DefinitionRepository;
import csv.hierarchy.pg.persistent.repository.NodeRepository;
import csv.hierarchy.pg.persistent.util.CsvRowUtil;
import csv.hierarchy.pg.persistent.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class NodeService {

    private final NodeRepository nodeRepository;
    private final DefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    public List<CsvRowNode> getAndValidate(Map<String, String> lookupMap, File file) {
        if (!FileUtil.validate(file, lookupMap)) {
            throw new NoRetryableException("The file and mapping does not match");
        }
        log.info("preparing file {}", file);
        try {
            List<CsvRowNode> rowNodes = new LinkedList<>();
            FileUtil.processCsv(file, rowMap -> rowNodes.add(CsvRowUtil.getCsvRowNode(rowMap, lookupMap)));
            log.info("file read. start validation row: {}", rowNodes.size());
            CsvRowUtil.validateAndSort(rowNodes);
            return rowNodes;
        } catch (NoRetryableException e) {
            throw e;
        } catch (Exception e) {
            throw new NoRetryableException(e.getMessage(), e.getCause());
        }
    }

    @SneakyThrows
    public Long save(Long clientId, Long hierarchyId, Instant effectiveFrom, Map<String, String> lookupMap, List<CsvRowNode> rowNodes) {
        definitionRepository.saveDef(clientId, hierarchyId, Timestamp.from(effectiveFrom), objectMapper.writeValueAsString(lookupMap));
        Long definitionId = definitionRepository.getLastInsertId();
        log.info("New version created for hierarchy {}, definition {}", hierarchyId, definitionId);

        List<Node> nodes = rowNodes.stream().map(row -> {
            Node aNode = new Node();
            BeanUtils.copyProperties(row, aNode);
            aNode.setDefinition(definitionId);
            aNode.setExtData(objectMapper.valueToTree(row.getExtData()));
            return aNode;
        }).toList();
        nodeRepository.saveAll(nodes);
        log.info("{} nodes saved for hierarchy {}, definition {}", nodes.size(), hierarchyId, definitionId);
        return definitionId;
    }
}
