# Test-PlayTech

# Configurable Report Generator (java_test_assignment_2025)

[![Java Version](https://img.shields.io/badge/Java-21+-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-red.svg)](https://maven.apache.org/)
[![Output Format](https://img.shields.io/badge/Output-JSONLines-orange.svg)](https://jsonlines.org/)

## 1. Overview

This Java application generates reports based on data from CSV files. Its core feature is the flexible configuration of the data processing pipeline and report structure using an XML configuration file. The application reads source data, applies a sequence of configurable transformations (aggregation, formatting, sorting, etc.), and writes the result to an output file in **JSON Lines (.jsonl)** format.

## 2. Prerequisites

* **Java Development Kit (JDK):** Version 21 or higher is required.
* **Maven:** Required for building the project and managing dependencies.

## 3. Project Structure

java_test_assignment_2025/
├── input/                      # Directory for input data
│   ├── casino_gaming_results.csv  # Example input CSV
│   └── DailyBetWinLossReport.xml  # Example configuration XML
├── output/                     # Directory for output reports (created automatically if needed)
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/playtech/
│   │           ├── ReportGenerator.java   # Main application class
│   │           ├── report/                # Classes related to the report (XML model, transformers)
│   │           │   ├── Report.java
│   │           │   ├── column/
│   │           │   │   └── Column.java
│   │           │   └── transformer/
│   │           │       ├── Transformer.java  # Transformer interface
│   │           │       └── impl/             # Transformer implementations
│   │           │           ├── AggregatorTransformer.java
│   │           │           ├── DateTimeFormatterTransformer.java
│   │           │           ├── MathOperationTransformer.java
│   │           │           ├── OrderingTransformer.java
│   │           │           └── StringFormatterTransformer.java
│   │           └── util/                  # Utility classes
│   │               └── xml/                 # XML related utilities (parser, adapters, helpers)
│   │                   ├── XmlParser.java
│   │                   ├── adapters/
│   │                   ├── helpers/
│   │                   └── refs/
│   └── test/                     # Unit tests (if available)
├── pom.xml                     # Maven configuration file
└── README.md                   # This documentation file
## 4. Dependencies

The project uses Maven for dependency management. Key dependencies (defined in `pom.xml`):
* **OpenCSV:** Used for work with CSV files!
* **Jackson Databind:** (`com.fasterxml.jackson.core:jackson-databind`) Used for generating the output data in JSON Lines format.
* **Jakarta XML Binding (JAXB):** (`jakarta.xml.bind:jakarta.xml.bind-api` and an implementation like `org.glassfish.jaxb:jaxb-runtime`) Used for parsing the XML configuration file into Java objects.
* **Java Compiler Level:** The `pom.xml` specifies Java 17 (`<maven.compiler.source>17</maven.compiler.source>`). While Java 17 is the minimum required by the current build configuration, ensure your runtime environment is Java 21+ as requested. You can update the `maven.compiler.source` and `maven.compiler.target` properties in `pom.xml` to `21` if needed.

## 5. Configuration (Report XML File)

The report generator's behavior is entirely controlled by an XML configuration file (e.g., `input/DailyBetWinLossReport.xml`). This file is passed as the second command-line argument when running the application.

**XML Structure:**

* **`<report>`:** The root element.
    * **`<reportName>` (Optional):** Name of the report (not actively used in the current implementation).
    * **`<outputFormat>` (Optional):** Output format (the current implementation is fixed to **JSONL**).
    * **`<inputs>`:** Defines the structure and types of input columns expected from the CSV. Primarily for reference (current CSV parsing is basic).
        * **`<input name="..." type="..."/>`:** Defines an input column. `name` should match the CSV header, `type` (STRING, INTEGER, DOUBLE, DATE, DATETIME) indicates the expected data type.
    * **`<outputs>`:** Defines the columns expected in the **final** output report (after all transformations).
        * **`<output name="..." type="..."/>`:** Defines an output column. The `name` is used as the key in the output JSONL objects.
    * **`<transformers>`:** Defines the sequence of operations to process the data.
        * **`<transformer name="..." >`:** Defines a single transformation step. The `name` must match the `NAME` constant in the corresponding transformer implementation class.
            * **`<parameters>`:** Contains parameters specific to this transformer (see `Parameters.java` and transformer constructors). Uses column references (`<input name="..."/>`, `<output name="..."/>`) and other values (`<format>`, `<order>`, `<operation>`, etc.).

**Example `<transformers>` Section:**

```xml
    <transformers>
        <transformer name="DateTimeFormatter">
            <parameters>
                <input name="StartDateTime"/> <output name="StartDate"/>   <format>yyyy-MM-dd</format> </parameters>
        </transformer>
        <transformer name="Aggregator">
            <parameters>
                <groupBy name="StartDate"/> <aggregateBys> <aggregateBy method="SUM">
                        <input name="BetAmount"/>
                        <output name="BetAmountSum"/>
                    </aggregateBy>
                    </aggregateBys>
            </parameters>
        </transformer>
        </transformers>

## 6. Transformers
Transformers are classes implementing the Transformer interface that perform specific operations on the report data. They are executed sequentially in the order defined in the XML configuration.

Available Implementations (impl package):

DateTimeFormatterTransformer: Formats date/time values from an input column to an output column using a specified pattern.
Parameters: input (column), output (column), format (output format string), inputFormat (optional input format string).
AggregatorTransformer: Aggregates data (SUM, AVG, MIN, MAX, COUNT) based on grouping by a specified column. This significantly changes the data structure (reduces row count).
Parameters: groupBy (column), aggregateBys (list of: input, output, method).
MathOperationTransformer: Performs mathematical operations (ADD, SUBTRACT) on two input columns, writing the result to an output column.
Parameters: inputs (list of 2 columns), output (column), operation (ADD/SUBTRACT).
OrderingTransformer: Sorts the report rows based on the values in a specified column.
Parameters: input (column to sort by), order (ASC/DESC).
StringFormatterTransformer: Formats values from one or more input columns into a string according to a specified pattern (String.format) and writes the result to an output column.
Parameters: inputs (list of columns), output (column), format (format string).
Note: All transformers operate on the data represented as a List<Map<String, Object>> and modify this list in place.

## 7. Data Flow
Launch: ReportGenerator.main() receives 3 command-line arguments: CSV path, XML path, Output JSONL path.
Parse XML: XmlParser.parseReport() reads the XML and creates a Report object containing the full configuration (including instantiated Transformer objects with their parameters, thanks to TransformerAdapter).
Load & Parse CSV: loadAndParseCsvData() reads the specified CSV file, extracts headers, and reads data rows.
Convert Data: convertToMapList() converts the raw CSV data (List<List<Object>>) into the List<Map<String, Object>> format, using headers as map keys.
Apply Transformers: applyTransformations() iterates through the Transformer list from the Report object. For each transformer, it calls the transform(report, dataRowsAsMapList) method. The transformer modifies the dataRowsAsMapList in place.
Generate Output: generateOutput() calls generateJsonl(), passing it the final transformed dataRowsAsMapList and the desired output file path.
Write JSON Lines: generateJsonl() creates necessary directories, iterates through the list of maps, converts each map to a JSON string using Jackson ObjectMapper, and writes it as a line to the output file, followed by a newline.
## 8. Usage
Building the Project:

Use Maven to build the project (compile code and package it, typically into a JAR file):

Bash

# Navigate to the project's root directory (where pom.xml is located)
cd /path/to/java_test_assignment_2025

# Build the project
mvn clean package
Running the Application:

Run the main class com.playtech.ReportGenerator from the command line (or the IntelliJ IDEA terminal), providing the three required arguments:

Bash

java <classpath_options> com.playtech.ReportGenerator <path_to_input_csv> <path_to_config_xml> <path_to_output_jsonl>
Example (running from the project root, using relative paths):

Bash

# <classpath_options> depends on how you built the project 
# (e.g., -cp target/your-jar-name.jar)
# If running from an IDE, it usually handles the classpath.

java <classpath_options> com.playtech.ReportGenerator input/casino_gaming_results.csv input/DailyBetWinLossReport.xml output/my_generated_report.jsonl
<classpath_options>: Replace with the correct classpath for your compiled project/JAR.
input/casino_gaming_results.csv: Path to your input CSV file.
input/DailyBetWinLossReport.xml: Path to your XML configuration file.
output/my_generated_report.jsonl: Full path (including filename) where the output JSON Lines file will be created. Use relative paths (without a leading / or \) to create the file within your project structure.
Expected Result:

A file will be created at the specified output path (e.g., output/my_generated_report.jsonl in the example) containing the processed data in JSON Lines format. The console will display logs indicating the progress and success or failure.

## 9. Code Overview (Key Classes)
com.playtech.ReportGenerator: Orchestrates the entire process. Contains the main method and helper static methods for each stage (parsing args, loading config, loading CSV, applying transforms, generating output).
com.playtech.report.Report: POJO class representing the XML configuration structure (used by JAXB).
com.playtech.report.column.Column: POJO class describing a column (used by JAXB).
com.playtech.report.transformer.Transformer: Interface defining the contract for all transformers (method transform(Report, List<Map<String, Object>>)).
com.playtech.report.transformer.impl.*: Package containing the concrete implementations of the Transformer interface.
com.playtech.util.xml.XmlParser: Utility for parsing XML using JAXB.
com.playtech.util.xml.adapters.*: JAXB adapters (TransformerAdapter, ColumnAdapter) used implicitly by JAXB to customize XML parsing.
com.playtech.util.xml.helpers.*: Helper classes (Parameters, TransformerWrapper) to facilitate XML structure and JAXB processing.
10. Extending (Optional)
To add a new type of transformer:

Create a new class implementing the com.playtech.report.transformer.Transformer interface in the impl package.
Implement the transform(Report report, List<Map<String, Object>> rows) method with your custom logic, modifying the rows list.
Define a public static final String NAME = "your_transformer_name";.
Add parameters needed by your transformer to the com.playtech.util.xml.helpers.Parameters class (with JAXB annotations).
Create a constructor in your new transformer class that accepts these parameters.
Update the switch statement in com.playtech.util.xml.adapters.TransformerAdapter.unmarshal() to instantiate your new transformer when its NAME is encountered in the XML.
You can now use <transformer name="your_transformer_name">...</transformer> in your XML configurations.
