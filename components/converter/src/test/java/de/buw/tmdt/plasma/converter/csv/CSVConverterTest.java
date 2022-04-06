package de.buw.tmdt.plasma.converter.csv;

import de.buw.tmdt.plasma.converter.ConversionException;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CSVConverterTest extends TestCase {

    public void testConvertCommaSeperated() throws IOException, ConversionException {
        String prefix = "csv/singleHeader/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, 1);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.csv");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }

    public void testConvertTabularSeperated() throws IOException, ConversionException {
        String prefix = "csv/singleHeaderTabular/";
        InputStream is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "test.csv");
        assertNotNull(is);
        String input = IOUtils.toString(is, StandardCharsets.UTF_8);
        String result = CSVConverter.convert(input, '\t', 1);
        is = CSVConverter.class.getClassLoader().getResourceAsStream(prefix + "result.csv");
        assertNotNull(is);
        String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
        assertEquals(expected, result);
    }


}