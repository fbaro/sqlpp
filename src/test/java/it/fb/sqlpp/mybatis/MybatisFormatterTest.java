package it.fb.sqlpp.mybatis;

import it.fb.sqlpp.it.fb.sqlpp.mybatis.MybatisFormatter;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class MybatisFormatterTest {

    @Test
    public void test() throws ParserConfigurationException, XMLStreamException, SAXException, IOException {
        try (StringWriter out = new StringWriter()) {
            try (InputStream input = MybatisFormatterTest.class.getResourceAsStream("/test.xml");
                 WriterOutputStream out2 = new WriterOutputStream(out, StandardCharsets.UTF_8)) {
                MybatisFormatter.run(input, out2);
            }
            System.out.println(out.toString());
        }
    }

}