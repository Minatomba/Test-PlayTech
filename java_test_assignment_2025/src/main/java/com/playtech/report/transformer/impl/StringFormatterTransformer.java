package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.List;
import java.util.Map;
import java.util.Objects; // Для проверки на null
import java.util.stream.Collectors;

public class StringFormatterTransformer implements Transformer {
    public final static String NAME = "StringFormatter";

    // Parameters we are getting (by TransformerAdapter)
    private final List<Column> inputs;
    private final String format;
    private final Column output;

    public StringFormatterTransformer(List<Column> inputs, String format, Column output) {
        this.inputs = inputs;
        this.format = format;
        this.output = output;

        // Validation of constructor parameters
        if (this.inputs == null || this.inputs.isEmpty()) {
            throw new IllegalArgumentException("StringFormatterTransformer requires at least one input column.");
        }
        if (this.format == null || this.format.isEmpty()) {
            throw new IllegalArgumentException("StringFormatterTransformer requires a non-empty format string.");
        }
        if (this.output == null) {
            throw new IllegalArgumentException("StringFormatterTransformer requires an output column.");
        }
    }

    // Getters, if the needed
    public List<Column> getInputs() { return inputs; }
    public String getFormat() { return format; }
    public Column getOutput() { return output; }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        // Most common check
        if (rows == null || rows.isEmpty()) {
            System.out.println("StringFormatterTransformer: No data rows to format.");
            return;
        }

        final String outputColumnName = this.output.getName();
        final List<String> inputColumnNames = this.inputs.stream()
                .map(Column::getName)
                .collect(Collectors.toList());

        // Check the presence of all input columns in the data
        if (!rows.isEmpty() && !rows.get(0).keySet().containsAll(inputColumnNames)) {
            System.err.println("Warning: StringFormatterTransformer - not all input columns (" + inputColumnNames + ") found in data keys. Formatting might fail.");
        }

        System.out.println("StringFormatterTransformer: Formatting data using format '" + this.format + "' into column '" + outputColumnName + "'.");

        // Iterating by rows and modification by place
        for (Map<String, Object> rowMap : rows) {
            try {
                // Collecting args for formating out of current row
                Object[] args = inputColumnNames.stream()
                        .map(colName -> rowMap.getOrDefault(colName, "")) // Using empty row if key is gone
                        .toArray();

                // --- Better logic of convertation args ---
                // Trying to convert args to types expected by formatted row.
                // EX, if format "%.2f EUR", expected number.
                Object[] convertedArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    // Trying to convert into Double if it is possible
                    if (args[i] != null) {
                        try {
                            // Using toString() for flexibility
                            convertedArgs[i] = Double.parseDouble(args[i].toString());
                        } catch (NumberFormatException e) {
                            // If not parsing as Double, leave them as they are
                            // System.err.println("Warning (StringFormatter): Could not parse '" + args[i] + "' as Double for formatting. Using original value.");
                            convertedArgs[i] = args[i]; // as it was
                        }
                    } else {
                        convertedArgs[i] = null; // Saving null
                    }
                }
                // --- The end  ---

                // Applying formatting
                String formattedValue = String.format(this.format, convertedArgs);

                // Writing down the result in output column in current session
                rowMap.put(outputColumnName, formattedValue);

            } catch (Exception e) {
                // Catching errors of formating
                System.err.println("Error formatting row in StringFormatterTransformer: " + rowMap + " - Error: " + e.getMessage());
                rowMap.put(outputColumnName, "Format Error!");
            }
        }
        System.out.println("StringFormatterTransformer: Formatting complete.");
    }
}