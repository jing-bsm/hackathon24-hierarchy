package csv.hierarchy.pg.persistent.util;

import com.opencsv.CSVReader;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

@UtilityClass
public class FileUtil {
    @SneakyThrows
    public File getFile(String fileName) {
        URL is = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (is == null) {
            throw new FileNotFoundException(fileName);
        }
        return new File(is.toURI());
    }

    @SneakyThrows
    public void loadCsv(String filename, Consumer<String[]> consumer) {
        File file = getFile(filename);
        try (var fileReader = new FileReader(file); CSVReader reader = new CSVReader(fileReader)) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                consumer.accept(nextLine);
            }
        }
    }

    @SneakyThrows
    public void processCsv(File file, Consumer<Map<String, String>> consumer) {
        try (var fileReader = new FileReader(file); CSVReader reader = new CSVReader(fileReader)) {
            String[] headers = reader.readNext(); // Read the first row as headers
            if (headers == null) {
                return;
            }
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                Map<String, String> rowMap = new HashMap<>(headers.length);
                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i];
                    String value = nextLine[i];
                    rowMap.put(header, value);
                }
                consumer.accept(rowMap);
            }
        }
    }

    @SneakyThrows
    public boolean validate(File csvFile, Map<String, String> lookup) {
        List<String> headerList = Collections.emptyList();
        try (var fileReader = new FileReader(csvFile); CSVReader reader = new CSVReader(fileReader)) {
            String[] headers = reader.readNext(); // Read the first row as headers
            if (headers != null) {
                headerList = Arrays.asList(headers);
            }
        }
        return CsvRowUtil.canLoad(lookup, headerList);
    }
}