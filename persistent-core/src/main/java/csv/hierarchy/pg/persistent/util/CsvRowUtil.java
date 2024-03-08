package csv.hierarchy.pg.persistent.util;

import csv.hierarchy.pg.persistent.domain.CsvRowNode;
import csv.hierarchy.pg.persistent.domain.NoRetryableException;
import csv.hierarchy.pg.persistent.domain.SimpleTreeNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
@Log4j2
public class CsvRowUtil {
    List<String> basic = List.of("id", "name", "level", "parent_id");

    public CsvRowNode getCsvRowNode(Map<String, String> csvRow, Map<String, String> lookup) {
        String parentId = csvRow.get(lookup.get(basic.get(3)));
        var data = new HashMap<>(csvRow);
        lookup.keySet().forEach(data::remove); // todo should we keep level and name in json for unified search?
        return CsvRowNode.builder()
                .id(csvRow.get(lookup.get(basic.get(0))))
                .name(csvRow.get(lookup.get(basic.get(1))))
                .level(csvRow.get(lookup.get(basic.get(2))))
                .parentId(Objects.isNull(parentId) ? "" : parentId)
                .extData(data)
                .build();
    }

    public boolean canLoad(Map<String, String> lookup, List<String> csvHeader) {
        // id and parentId are required. they must in csvHeader
        return lookup.containsKey(basic.get(0)) && lookup.containsKey(basic.get(3)) &&
                csvHeader.contains(lookup.get(basic.get(3))) && csvHeader.contains(lookup.get(basic.get(0)));
    }

    public void validateAndSort(List<CsvRowNode> nodeList) {
        try {
            if (nodeList.isEmpty()) {
                return;
            }
            List<String> errors = SimpleTreeNode.validate(nodeList);
            nodeList.stream().map(CsvRowNode::getId).filter(CsvRowUtil::notValidId)
                    .forEach(id -> errors.add("Bad id: " + id));
            if (!errors.isEmpty()) {
                throw new NoRetryableException(errors);
            }
            SimpleTreeNode.sort(nodeList);

            log.debug("filling leaf and tree");

            Set<String> parentSet = nodeList.stream().map(CsvRowNode::getParentId).collect(Collectors.toSet());

            Map<String, CsvRowNode> nodeMap = nodeList.stream()
                    .collect(Collectors.toMap(CsvRowNode::getId, Function.identity()));
            nodeList.forEach(node -> {
                node.setLeaf(!parentSet.contains(node.getId()));
                // fill the tree
                ArrayList<String> list = new ArrayList<>();
                list.add(node.getId());
                CsvRowNode pNode = node;
                while (!pNode.getParentId().isEmpty()) {
                    list.add(pNode.getParentId());
                    pNode = nodeMap.get(pNode.getParentId());
                }
                Collections.reverse(list);
                node.setTree(String.join(".", list));
            });
        } catch (NoRetryableException e) {
            throw e;
        } catch (Exception ex) {
            throw new NoRetryableException(ex.getMessage(), ex.getCause());
        }
    }

    private boolean notValidId(String input) {
        // Regular expression to check if the string is alphanumeric or contains underscores
        String regex = "^[a-zA-Z0-9_]+$";
        // Check if the input matches the regular expression
        return !input.matches(regex);
    }

    @SneakyThrows
    public File toTemporallyFile(Object obj) {
        File file = File.createTempFile("obj", ".bin");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(obj);
        }
        return file;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T getObjectFromFile(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (T) ois.readObject();
        }
    }

}
