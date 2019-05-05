package it.fb.sqlpp.mybatis;

import com.google.common.io.Resources;
import it.fb.sqlpp.it.fb.sqlpp.mybatis.MybatisFormatter;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;

public class MybatisFormatterTest {

    @Test
    public void test() throws ParserConfigurationException, XMLStreamException, SAXException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (InputStream input = MybatisFormatterTest.class.getResourceAsStream("/test.xml")) {
                MybatisFormatter.run(input, out);
            }
            byte[] expected = Resources.toByteArray(Resources.getResource("test.xml"));
            assertArrayEquals(expected, out.toByteArray());
        }
    }

}