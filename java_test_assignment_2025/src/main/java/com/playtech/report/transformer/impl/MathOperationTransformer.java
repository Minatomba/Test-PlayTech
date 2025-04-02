package com.playtech.report.transformer.impl;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.Transformer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MathOperationTransformer implements Transformer {
    public final static String NAME = "MathOperation";


    private final List<Column> inputs;
    private final MathOperation operation;
    private final Column output;

    // Constructor
    public MathOperationTransformer(List<Column> inputs, MathOperation operation, Column output) {
        this.inputs = Objects.requireNonNull(inputs, "Inputs cannot be null");
        this.operation = Objects.requireNonNull(operation, "Operation cannot be null");
        this.output = Objects.requireNonNull(output, "Output column cannot be null");

        if (this.inputs.size() < 2 && (this.operation == MathOperation.ADD || this.operation == MathOperation.SUBTRACT)) {
            throw new IllegalArgumentException("ADD/SUBTRACT operations require at least two input columns.");
        }
    }

    @Override
    public void transform(Report report, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("MathOperationTransformer: No data rows to process.");
            return;
        }

        final String outputColumnName = this.output.getName();
        // Getting names of inputs
        final String input1Name = this.inputs.get(0).getName();
        final String input2Name = this.inputs.size() > 1 ? this.inputs.get(1).getName() : null; // Second argument for binary operations

        System.out.println("MathOperationTransformer: Performing operation '" + this.operation + "' into column '" + outputColumnName + "'.");

        // Iterating and modification rows by place
        for (Map<String, Object> rowMap : rows) {
            try {
                // Getting and parsing new value
                Object rawVal1 = rowMap.get(input1Name);
                double value1 = parseDouble(rawVal1); // Using additional method

                double result;

                // Doing operation
                switch (this.operation) {
                    case ADD:
                    case SUBTRACT:
                        if (input2Name == null) {
                            throw new IllegalStateException(this.operation + " requires two input columns, but second input is missing.");
                        }
                        Object rawVal2 = rowMap.get(input2Name);
                        double value2 = parseDouble(rawVal2);
                        result = (this.operation == MathOperation.ADD) ? (value1 + value2) : (value1 - value2);
                        break;

                    // TODO: Добавить другие операции (MULTIPLY, DIVIDE, etc.)
                    // case MULTIPLY: ...
                    // case DIVIDE: if (value2 == 0.0) throw new ArithmeticException("Division by zero"); ...

                    default:
                        throw new UnsupportedOperationException("Unsupported math operation: " + this.operation);
                }

                // Writing down result (ex, as Double)
                rowMap.put(outputColumnName, result);

            } catch (NumberFormatException e) {
                System.err.println("Warning (MathOperation): Could not parse number in row " + rowMap + ". " + e.getMessage() + ". Setting output to 'Calc Error!'");
                rowMap.put(outputColumnName, "Calc Error!");
            } catch (ArithmeticException e) {
                System.err.println("Warning (MathOperation): Calculation error in row " + rowMap + ". " + e.getMessage() + ". Setting output to 'Calc Error!'");
                rowMap.put(outputColumnName, "Calc Error!");
            } catch (Exception e) {
                // Catching different errors
                System.err.println("Error processing row in MathOperationTransformer: " + rowMap + ". Error: " + e.getMessage());
                rowMap.put(outputColumnName, "Calc Error!");
            }
        }
        System.out.println("MathOperationTransformer: Calculation complete.");
    }

    /** Additional method for parsing Double, checking null and errors. */
    private double parseDouble(Object value) throws NumberFormatException {
        if (value == null) {
            // throw new NumberFormatException("Input value is null");
            return 0.0; // Null as 0
        }
        try {
            // Trying to cast standard types and row
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else {
                // Removing spaces and changing "," to "." for European format
                String strValue = value.toString().trim().replace(',', '.');
                if (strValue.isEmpty()) return 0.0; // Empty row counts as 0
                return Double.parseDouble(strValue);
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot parse '" + value + "' as Double");
        }
    }


    public enum MathOperation {
        ADD,
        SUBTRACT,
        // MULTIPLY, // Add if needed
        // DIVIDE
    }
}