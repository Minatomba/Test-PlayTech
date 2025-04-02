package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Для сравнения

public class OrderingTransformer implements Transformer {

    public static final String NAME = "order";

    private final Column orderByColumn;
    private final Order orderDirection;

    public OrderingTransformer(Column orderByColumn, Order orderDirection) {
        this.orderByColumn = orderByColumn;
        this.orderDirection = orderDirection != null ? orderDirection : Order.ASC;
        if (this.orderByColumn == null) {
            throw new IllegalArgumentException("OrderingTransformer requires 'orderByColumn' parameter.");
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        //  `report` can be needed for getting metha-data if needed
        // Second `rows` - is our data, that needs to be sorted

        // if empty
        if (rows == null || rows.isEmpty()) {
            System.out.println("OrderingTransformer: No data rows to order.");
            return; // Then nothing
        }

        // Получаем имя колонки для сортировки из параметра конструктора
        final String sortColumnName = this.orderByColumn.getName();

        // Does column exist in first row
        if (!rows.get(0).containsKey(sortColumnName)) {
            System.err.println("OrderingTransformer: Column '" + sortColumnName + "' not found in data map keys. Skipping ordering.");
            // throw new IllegalStateException("Ordering column '" + sortColumnName + "' not found.");
            return;
        }

        // Creating comparator for Map<String, Object>
        Comparator<Map<String, Object>> comparator = (rowMap1, rowMap2) -> {
            if (rowMap1 == null || rowMap2 == null) return 0;

            Object val1 = rowMap1.get(sortColumnName);
            Object val2 = rowMap2.get(sortColumnName);

            // Better comparison (ex.)
            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return this.orderDirection == Order.ASC ? -1 : 1; // nulls first
            if (val2 == null) return this.orderDirection == Order.ASC ? 1 : -1;

            // Trying to compare as numbers if possible
            if (val1 instanceof Number && val2 instanceof Number) {
                int cmp = Double.compare(((Number) val1).doubleValue(), ((Number) val2).doubleValue());
                return cmp;
            }

            // Comparing as String
            return val1.toString().compareTo(val2.toString());
        };

        // Applying direction of sorting
        if (this.orderDirection == Order.DESC) {
            comparator = comparator.reversed();
        }

        // Sorting list `rows` by place
        rows.sort(comparator);

        System.out.println("OrderingTransformer: Data rows ordered by column '" + sortColumnName + "' (" + this.orderDirection + ").");

    }

    public enum Order { ASC, DESC }
}