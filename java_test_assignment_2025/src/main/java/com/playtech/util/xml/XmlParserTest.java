package com.playtech.util.xml;

import com.playtech.report.Report;
import com.playtech.report.column.Column;
import com.playtech.report.transformer.impl.DateTimeFormatterTransformer;
import com.playtech.report.transformer.impl.StringFormatterTransformer;
import jakarta.xml.bind.JAXBException;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class XmlParserTest {

    @Test
    public void parseReport_ValidFile_ShouldReturnReport() throws Exception {
        // Arrange
        URL resource = getClass().getClassLoader().getResource("test_report.xml");
        assertNotNull(String.valueOf(resource), "Test XML file not found in resources");
        String filePath = Paths.get(resource.toURI()).toString();

        // Act
        Report report = XmlParser.parseReport(filePath);

        // Assert
        assertNotNull(report);
        assertEquals("TestReport", report.getReportName());
        assertEquals(Report.FileFormat.JSONL, report.getOutputFormat());

        // Проверяем входные колонки
        assertNotNull(report.getInputs());

        assertTrue(report.getOutputs().stream().anyMatch(c -> "PlayerId".equals(c.getName())));
        assertTrue(report.getOutputs().stream().anyMatch(c -> "FormattedWinLoss".equals(c.getName())));
        assertTrue(report.getOutputs().stream().anyMatch(c -> "FormattedDate".equals(c.getName())));


        // Проверяем трансформеры
        assertNotNull(report.getTransformers());
        assertEquals(2, report.getTransformers().size());
        assertTrue(report.getTransformers().get(0) instanceof StringFormatterTransformer);
        assertTrue(report.getTransformers().get(1) instanceof DateTimeFormatterTransformer);

        // Более детальная проверка конфигурации трансформеров (если нужно)
        // Например, для StringFormatterTransformer
        StringFormatterTransformer sfTransformer = (StringFormatterTransformer) report.getTransformers().get(0);
        assertNotNull(sfTransformer.getInputs());
        assertEquals(1, sfTransformer.getInputs().size());
        assertEquals("WinLoss", sfTransformer.getInputs().get(0).getName()); // Проверяем имя связанной колонки
        assertEquals("\"WinLoss: %.2f\"", sfTransformer.getFormat());
        assertNotNull(sfTransformer.getOutput());
        assertEquals("FormattedWinLoss", sfTransformer.getOutput().getName()); // Проверяем имя связанной колонки
    }
    @Test
    public void parseReport_NonExistentFile_ShouldThrowException() {
        // Arrange
        String nonExistentPath = "path/to/non/existent/file.xml";

        // Act & Assert
        assertThrows(JAXBException.class, () -> XmlParser.parseReport(nonExistentPath));
    }

    @Test
    public void parseReport_InvalidXml_ShouldThrowException() throws Exception {
        // Arrange
        URL resource = getClass().getClassLoader().getResource("invalid-report.xml"); // Создайте файл с невалидным XML
        assertNotNull(String.valueOf(resource), "Invalid XML test file not found in resources");
        String filePath = Paths.get(resource.toURI()).toString();

        // Act & Assert
        assertThrows(JAXBException.class, () -> XmlParser.parseReport(filePath));

    }
}