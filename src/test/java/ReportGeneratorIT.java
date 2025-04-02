import com.playtech.ReportGenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorIT {

    // JUnit 5 предоставляет временную директорию для теста
    @TempDir
    Path tempDir;

    String resourcePath = "input/test_data.csv";

    @Test
    @DisplayName("Should generate correct JSONL file for test configuration")
    void main_generatesCorrectFileEndToEnd() throws Exception {

        System.out.println("Attempting to load resource from classpath: '" + resourcePath + "'");
        java.net.URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
        System.out.println("Result of getResource: " + resourceUrl); //  null, if not found

        // Arrange
        // Getting path to the test resources
        // Using ClassLoader for safety
        Path csvPath = Paths.get(getClass().getClassLoader().getResource("input/test_data.csv").toURI());
        Path xmlPath = Paths.get(getClass().getClassLoader().getResource("config/test_config.xml").toURI());
        Path expectedOutputPath = Paths.get(getClass().getClassLoader().getResource("expected/test_output.jsonl").toURI());

        Path actualOutputPath = tempDir.resolve("actual_output.jsonl");

        // Collecting args for main
        String[] args = {
                csvPath.toString(),
                xmlPath.toString(),
                actualOutputPath.toString()
        };

        // Act
        // Starting the main method ReportGenerator
        // NB: If main calls System.exit(), that test can stop the JVM
        ReportGenerator.main(args);

        // Assert
        // Checking if outputPath exists
        assertThat(actualOutputPath).exists().isRegularFile();

        // Checking the values of real file and the predicted
        List<String> actualLines = Files.readAllLines(actualOutputPath);
        List<String> expectedLines = Files.readAllLines(expectedOutputPath);

        // Checking by lines
        assertThat(actualLines).containsExactlyElementsOf(expectedLines);
        // Or for more flexible comparison (ignoring the order of lines):
        // assertThat(actualLines).containsExactlyInAnyOrderElementsOf(expectedLines);
    }

    // TODO: Добавить больше интеграционных тестов для разных конфигураций XML и данных CSV.
}