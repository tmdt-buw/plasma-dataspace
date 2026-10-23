package de.buw.tmdt.plasma.converter.csv;

import de.buw.tmdt.plasma.converter.ConversionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CSVConverterTest {

    @Test
    public void testConvertCommaSeperated() throws IOException, ConversionException {
        String prefix = "csv/singleHeader/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, 1);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testConvertTabularSeperated() throws IOException, ConversionException {
        String prefix = "csv/singleHeaderTabular/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, '\t', 1);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testMultiHeader() throws IOException, ConversionException {
        String prefix = "csv/multiHeader/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, ',', 3);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testWithoutHeader() throws IOException, ConversionException {
        String prefix = "csv/withoutHeader/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, 0);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testInvalidDataRows() throws IOException, ConversionException {
        String prefix = "csv/invalidDataRows/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, 1);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    @Test
    public void testUnequalHeader() throws IOException, ConversionException {
        String prefix = "csv/unequalHeader/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, 5);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.json");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }


}