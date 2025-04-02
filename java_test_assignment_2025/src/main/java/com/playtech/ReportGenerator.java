package com.playtech;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;
import com.playtech.util.xml.XmlParser; // Используем наш XmlParser

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGenerator {

    // --- Main Logic ---
    public static void main(String[] args) {
        System.out.println("Received arguments: " + Arrays.toString(args));
        try {
            // 1. Processing args (3 args)
            CommandLineArgs cmdArgs = parseArguments(args);

            // 2. Loading configuration from XML
            Report report = loadReportConfiguration(cmdArgs.xmlPath());

            // 3. Loading and primary parsing CSV
            InitialData initialCsvData = loadAndParseCsvData(cmdArgs.csvPath());

            // 4. Transformation data in format List<Map<String, Object>>
            List<Map<String, Object>> dataRowsAsMapList = convertToMapList(
                    initialCsvData.rows(), initialCsvData.headers()
            );

            // 5. Applying transformers (now the can modify dataRowsAsMapList)
            applyTransformations(report, dataRowsAsMapList);

            // 6. Generating output file (using dataRowsAsMapList after transformation)
            generateOutput(dataRowsAsMapList, cmdArgs.outputPath());

            System.out.println("Report generated successfully!");

        } catch (ReportGenerationException e) {
            System.err.println("Report generation failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace(); // Printing StackTrace as reason for diagnose
            } else {
                e.printStackTrace();
            }
            System.exit(1);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        // finally { clearContext(); } // Clearing of context is not needed anymore
    }

    // --- Additional methods ---

    /** Processes 3 args of command line. */
    private static CommandLineArgs parseArguments(String[] args) throws ReportGenerationException {
        if (args == null || args.length != 3) {
            throw new ReportGenerationException("Usage: java com.playtech.ReportGenerator <input.csv> <config.xml> <output.jsnol>");
        }
        return new CommandLineArgs(args[0], args[1], args[2]);
    }

    /** Loading configuration report out XML file. */
    private static Report loadReportConfiguration(String xmlPath) throws ReportGenerationException {
        try {
            System.out.println("Loading report configuration from: " + xmlPath);
            // Fixed: Using the right method parseReport
            Report report = XmlParser.parseReport(xmlPath);
            if (report == null) {
                throw new ReportGenerationException("Failed to parse report configuration: result is null.");
            }
            System.out.println("Report configuration loaded successfully.");
            return report;
        } catch (Exception e) { // Catching JAXBException and other possible errors
            throw new ReportGenerationException("Failed to load or parse report configuration XML: " + xmlPath, e);
        }
    }

    /** Loading data out of CSV and returning headers and rows. */
    private static InitialData loadAndParseCsvData(String csvFilePath) throws ReportGenerationException {
        System.out.println("Loading initial data from CSV: " + csvFilePath);
        Path inputPath = validateAndGetPath(csvFilePath);

        List<String> lines;
        try {
            lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
            System.out.println("Read " + lines.size() + " lines from CSV.");
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to read CSV file: " + csvFilePath, e);
        }

        if (lines.isEmpty()) {
            System.out.println("CSV file is empty. Returning empty data.");
            return new InitialData(new ArrayList<>(), new ArrayList<>()); // Returning empty lists
        }

        List<String> headers = Arrays.asList(lines.get(0).split(","));
        System.out.println("CSV Headers: " + headers);

        List<List<Object>> initialDataRows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",", -1);
            initialDataRows.add(new ArrayList<>(Arrays.asList(values))); // Keep as rows
        }
        System.out.println("Parsed " + initialDataRows.size() + " data rows.");
        return new InitialData(headers, initialDataRows); // Returning result
    }

    /** Checking path to file and returning object Path. */
    private static Path validateAndGetPath(String filePath) throws ReportGenerationException {
        try {
            Path path = Paths.get(filePath);
            return path;
        } catch (InvalidPathException e) {
            throw new ReportGenerationException("Invalid file path: " + filePath, e);
        }
    }

    /** Formating data out of list list into map list (row). */
    private static List<Map<String, Object>> convertToMapList(List<List<Object>> dataRows, List<String> headers) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (headers == null || headers.isEmpty() || dataRows == null) { // Checking dataRows
            System.err.println("Warning: Cannot convert data to map list - headers or data are missing/empty.");
            return mapList;
        }
        int numColumns = headers.size();
        int rowNum = 0;
        for (List<Object> rowList : dataRows) {
            rowNum++;
            if (rowList != null && rowList.size() == numColumns) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < numColumns; i++) {
                    rowMap.put(headers.get(i), rowList.get(i)); // Using Header as key
                }
                mapList.add(rowMap);
            } else {
                System.err.printf("Warning: Skipping row %d during initial conversion due to inconsistent column count (expected %d, found %d).%n",
                        rowNum, numColumns, (rowList == null ? 0 : rowList.size()));
            }
        }
        System.out.println("Converted " + mapList.size() + " rows to List<Map<String, Object>> format.");
        return mapList;
    }

    /** Applying transformers to data (by map list). */
    private static void applyTransformations(Report report, List<Map<String, Object>> dataRows) throws ReportGenerationException {
        // Принимает List<Map<String, Object>>
        if (report.getTransformers() == null || report.getTransformers().isEmpty()) {
            System.out.println("No transformers defined. Skipping transformation phase.");
            return;
        }
        System.out.println("Applying " + report.getTransformers().size() + " transformers...");
        int count = 0;
        for (Transformer transformer : report.getTransformers()) {
            count++;
            System.out.printf("Applying transformer %d: %s%n", count, transformer.getClass().getSimpleName());
            try {
                transformer.transform(report, dataRows);
            } catch (Exception e) {
                System.err.println("Error applying transformer #" + count + " (" + transformer.getClass().getName() + "): " + e.getMessage());
                if(e.getCause() != null) { e.getCause().printStackTrace(); } else { e.printStackTrace(); }
                throw new ReportGenerationException("Failed during transformation #" + count + " (" + transformer.getClass().getName() + ")", e);
            }
        }
        System.out.println("All transformers applied successfully.");
    }

    /** Calls generation of output file. */
    private static void generateOutput(List<Map<String, Object>> finalDataRows, String outputFilePath) throws ReportGenerationException {
        // Getting final list of maps
        System.out.println("Preparing to generate output to: " + outputFilePath);
        try {
            // Pushing final data straight
            generateJsonl(finalDataRows, outputFilePath);
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to write output file: " + outputFilePath, e);
        }
    }

    /** Generating file report in format JSON Lines (jsonl). */
    private static void generateJsonl(List<Map<String, Object>> jsonDataRows, String outputFilePath) throws IOException {
        System.out.println("Generating JSON Lines report to: " + outputFilePath);
        Path outputPath = null; // Using validator of path
        try {
            outputPath = validateAndGetPath(outputFilePath);
        } catch (ReportGenerationException e) {
            throw new RuntimeException(e);
        }
        Path parentDir = outputPath.getParent();

        if (jsonDataRows == null || jsonDataRows.isEmpty()) {
            System.out.println("No data provided to write. Generating empty file.");
            if (parentDir != null) { Files.createDirectories(parentDir); }
            Files.writeString(outputPath, "", StandardCharsets.UTF_8);
            return;
        }

        if (parentDir != null) { Files.createDirectories(parentDir); }

        ObjectMapper objectMapper = new ObjectMapper();
        int rowCount = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            for (Map<String, Object> rowMap : jsonDataRows) {
                if (rowMap != null && !rowMap.isEmpty()) {
                    String jsonLine = objectMapper.writeValueAsString(rowMap);
                    writer.write(jsonLine);
                    writer.newLine();
                    rowCount++;
                } else {
                    System.err.printf("Warning: Skipping empty or null row map at index %d.%n", rowCount);
                }
            }
            System.out.printf("Successfully wrote %d rows to JSON Lines report: %s%n", rowCount, outputFilePath);
        }
    }

    // --- Additional classes ---
    private record CommandLineArgs(String csvPath, String xmlPath, String outputPath) {}
    // New record for returned data out of CSV parser
    private record InitialData(List<String> headers, List<List<Object>> rows) {}
    private static class ReportGenerationException extends Exception {
        public ReportGenerationException(String message) { super(message); }
        public ReportGenerationException(String message, Throwable cause) { super(message, cause); }
    }
}