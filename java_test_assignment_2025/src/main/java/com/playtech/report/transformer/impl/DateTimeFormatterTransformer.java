package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId; // Для работы с временем
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor; // Общий тип для даты/времени
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DateTimeFormatterTransformer implements Transformer {
    public static final String NAME = "DateTimeFormatter";

    // Параметры конструктора
    private final Column input;
    private final String format;
    private final Column output;
    private final String inputFormat;

    // Constructor (can be overloaded or changed for taking inputFormat)
    public DateTimeFormatterTransformer(Column input, String format, Column output) {
        // inputFormat can be null
        this(input, format, output, null);
    }


    public DateTimeFormatterTransformer(Column input, String format, Column output, String inputFormat) {
        this.input = Objects.requireNonNull(input, "Input column cannot be null");
        this.format = Objects.requireNonNull(format, "Output format cannot be null");
        this.output = Objects.requireNonNull(output, "Output column cannot be null");
        this.inputFormat = inputFormat;
        if (this.format.isEmpty()) {
            throw new IllegalArgumentException("Output format cannot be empty");
        }
    }


    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("DateTimeFormatterTransformer: No data rows to format.");
            return;
        }

        final String inputColumnName = this.input.getName();
        final String outputColumnName = this.output.getName();

        // Creating formatter for output
        DateTimeFormatter outputFormatter;
        try {
            outputFormatter = DateTimeFormatter.ofPattern(this.format);
        } catch (IllegalArgumentException e) {
            System.err.println("DateTimeFormatterTransformer: Invalid output format pattern '" + this.format + "'. Skipping transformation.");
            // throw new IllegalArgumentException("Invalid output format pattern: " + this.format, e);
            return;
        }

        // Creating optional formatter for input
        DateTimeFormatter inputParser = (this.inputFormat != null && !this.inputFormat.isEmpty())
                ? DateTimeFormatter.ofPattern(this.inputFormat)
                : null; // If null, we will use standard forms

        System.out.println("DateTimeFormatterTransformer: Formatting column '" + inputColumnName + "' into '" + outputColumnName + "' using output format '" + this.format + "'.");

        // Iteration and modification rows by place
        for (Map<String, Object> rowMap : rows) {
            Object rawValue = rowMap.get(inputColumnName);

            if (rawValue == null) {
                // IF row null writing null
                rowMap.put(outputColumnName, null); // or "N/A"
                continue;
            }

            String valueStr = rawValue.toString();
            if (valueStr.isEmpty()) {
                rowMap.put(outputColumnName, "");
                continue;
            }

            try {
                TemporalAccessor temporalAccessor;

                // Trying to parse input
                if (inputParser != null) {
                    // If the inputed format has been set using him
                    temporalAccessor = parseWithInputFormat(valueStr, inputParser);
                } else {
                    // Or trying standarted formats
                    temporalAccessor = parseFlexible(valueStr);
                }

                // If parsing is succeeded , formatting for output
                String formattedValue = outputFormatter.format(temporalAccessor);
                rowMap.put(outputColumnName, formattedValue);

            } catch (DateTimeParseException e) {
                System.err.println("Warning (DateTimeFormatter): Could not parse date/time string '" + valueStr + "' in row " + rowMap + ". Error: " + e.getMessage());
                rowMap.put(outputColumnName, "Invalid Date!"); // Marks of errors
            } catch (Exception e) {
                // Catching errors
                System.err.println("Error formatting date/time for value '" + valueStr + "' in row " + rowMap + ". Error: " + e.getMessage());
                rowMap.put(outputColumnName, "Format Error!");
            }
        }
        System.out.println("DateTimeFormatterTransformer: Formatting complete.");
    }

    // Additional method for parsing with formattor
    // (can throw DateTimeParseException)
    private TemporalAccessor parseWithInputFormat(String value, DateTimeFormatter parser) {
        // Trying diffrent types if formatter is not specified
        try {
            return ZonedDateTime.parse(value, parser);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(value, parser);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(value, parser);
                } catch (DateTimeParseException e3) {
                    try {
                        return YearMonth.parse(value, parser);
                    } catch (DateTimeParseException e4) {
                        // If nothing fits, throwing last error
                        throw e4;
                    }
                }
            }
        }
    }

    // Additional method for flexibile parsing
    // (can thorw DateTimeParseException)
    private TemporalAccessor parseFlexible(String value) {
        // Trying ISO formats
        try {
            return ZonedDateTime.parse(value);
        } catch (DateTimeParseException e1) { // ISO with zone
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e2) { // ISO without zone
                try {
                    return LocalDate.parse(value);
                } catch (DateTimeParseException e3) { // ISO data
                    // try { return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy/MM/dd")); } catch ...
                    throw new DateTimeParseException("Failed to parse date/time using standard formats", value, 0, e3); // Throwing error
                }
            }
        }
    }
}