package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlIDREF;

import java.util.*;
import java.util.stream.Collectors;

public class AggregatorTransformer implements Transformer {
    public static final String NAME = "Aggregator";

    // Parameters of constructor
    private final Column groupByColumn; // Column for grouping
    private final List<AggregateBy> aggregateColumns; // List of operations of aggregation

    // Constructor
    public AggregatorTransformer(Column groupByColumn, List<AggregateBy> aggregateColumns) {
        this.groupByColumn = Objects.requireNonNull(groupByColumn, "Group By column cannot be null");
        this.aggregateColumns = Objects.requireNonNull(aggregateColumns, "Aggregate By list cannot be null");
        if (this.aggregateColumns.isEmpty()) {
            throw new IllegalArgumentException("At least one AggregateBy definition is required.");
        }
    }

    @Override
    // FIXED
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("AggregatorTransformer: No data rows to aggregate.");
            return;
        }

        final String groupByKey = this.groupByColumn.getName();

        // Check for the presence of a column for grouping
        if (!rows.get(0).containsKey(groupByKey)) {
            System.err.println("AggregatorTransformer: Group By column '" + groupByKey + "' not found in data map keys. Skipping aggregation.");
            return;
        }

        System.out.println("AggregatorTransformer: Aggregating data grouped by '" + groupByKey + "'.");

        // Preform grouping and aggregation
        Map<Object, Map<String, Object>> aggregatedResults = rows.stream()
                .filter(row -> row != null && row.get(groupByKey) != null) // Ignoring rows with null key of grouping
                .collect(Collectors.groupingBy(
                        row -> row.get(groupByKey), // Key for group
                        Collectors.collectingAndThen(Collectors.toList(), // Collecting rows in group
                                group -> calculateAggregates(group, this.aggregateColumns) // Calculate aggregate for groups
                        )
                ));

        // Forming new list rows out of result of aggregation
        List<Map<String, Object>> aggregatedRows = new ArrayList<>();
        aggregatedResults.forEach((groupKeyValue, aggregateValues) -> {
            // Creating new row for group
            Map<String, Object> newRow = new LinkedHashMap<>(); // LinkedHashMap for order
            // Adding key of grouping
            newRow.put(groupByKey, groupKeyValue);
            // Adding calculated aggregations
            newRow.putAll(aggregateValues);
            aggregatedRows.add(newRow);
        });

        // Changing content of original list `rows` with aggregated data
        System.out.println("AggregatorTransformer: Aggregation complete. Replacing original " + rows.size() + " rows with " + aggregatedRows.size() + " aggregated rows.");
        rows.clear();
        rows.addAll(aggregatedRows);


    }

    /** Calculate aggregations for group of rows. */
    private Map<String, Object> calculateAggregates(List<Map<String, Object>> group, List<AggregateBy> definitions) {
        Map<String, Object> results = new HashMap<>();
        for (AggregateBy def : definitions) {
            String inputColName = def.getInput().getName();
            String outputColName = def.getOutput().getName();
            Method method = def.getMethod();

            // Getting thread of values for aggregation trying to cast into Double
            // Ignoring null and errors of parsing
            List<Double> values = group.stream()
                    .map(row -> row.get(inputColName))
                    .filter(Objects::nonNull)
                    .map(this::tryParseDouble)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            if (values.isEmpty() && method != Method.COUNT) { // Count can be ZERO
                results.put(outputColName, 0.0); // Or null depends of requirements
                continue;
            }

            double resultValue = switch (method) {
                case SUM -> values.stream().mapToDouble(Double::doubleValue).sum();
                case AVG -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                case MIN -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                case MAX -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                case COUNT -> (double) group.stream() // Counting all not "null" values in origin column
                        .map(row -> row.get(inputColName))
                        .filter(Objects::nonNull)
                        .count();
                default -> {
                    System.err.println("Unsupported aggregation method: " + method);
                    yield 0.0; // Or null or throw exception
                }
            };
            results.put(outputColName, resultValue);
        }
        return results;
    }

    /** Try parsing Object in Double, returns Optional. */
    private Optional<Double> tryParseDouble(Object value) {
        if (value == null) return Optional.empty();
        try {
            if (value instanceof Number) {
                return Optional.of(((Number) value).doubleValue());
            } else {
                String strValue = value.toString().trim().replace(',', '.');
                if (strValue.isEmpty()) return Optional.empty();
                return Optional.of(Double.parseDouble(strValue));
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning (Aggregator): Could not parse value '" + value + "' as Double for aggregation.");
            return Optional.empty();
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AggregateBy {
        @XmlIDREF private Column input;
        private Method method;
        @XmlIDREF private Column output;

        public Column getInput() { return input; }
        public Column getOutput() { return output; }
        public Method getMethod() { return method; }
    }

    public enum Method { SUM, AVG, MIN, MAX, COUNT } // Added  MIN, MAX, COUNT
}