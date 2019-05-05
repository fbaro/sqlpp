package it.fb.sqlpp.it.fb.sqlpp.mybatis;

import com.google.common.base.Strings;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MybatisFormatter {

    public static void run(InputStream input, OutputStream output) throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
//        XMLReader xmlReader = saxParser.getXMLReader();
//        xmlReader.setContentHandler(new CopyHandler());
//        xmlReader.parse(new InputSource(input));
        CopyHandler handler = new CopyHandler(output);
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        try {
            saxParser.parse(input, handler);
        } catch (CopyHandler.RuntimeXMLStreamException ex) {
            throw ex.getCause();
        }
    }

    private static class CopyHandler extends DefaultHandler2 {

        private final XMLStreamWriter writer;
        private StringBuilder cdataBuffer = null;

        CopyHandler(OutputStream output) throws XMLStreamException {
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            writer = outputFactory.createXMLStreamWriter(output);
        }

        private void wrap(WriteCommand cmd) {
            try {
                cmd.run();
            } catch (XMLStreamException e) {
                throw new RuntimeXMLStreamException(e);
            }
        }

        @Override
        public void startDocument() {
            wrap(writer::writeStartDocument);
        }

        @Override
        public void endDocument() {
            wrap(writer::writeEndDocument);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            wrap(() -> {
                if (Strings.isNullOrEmpty(uri)) {
                    writer.writeStartElement(localName);
                } else {
                    writer.writeStartElement(uri, localName);
                }
                for (int i = 0; i < attributes.getLength(); i++) {
                    String attrUri = attributes.getURI(i);
                    if (Strings.isNullOrEmpty(attrUri)) {
                        writer.writeAttribute(attributes.getLocalName(i), attributes.getValue(i));
                    } else {
                        writer.writeAttribute(attrUri, attributes.getLocalName(i), attributes.getValue(i));
                    }
                }
            });
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            wrap(writer::writeEndElement);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (cdataBuffer != null) {
                cdataBuffer.append(ch, start, length);
            } else {
                wrap(() -> writer.writeCharacters(ch, start, length));
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            if (cdataBuffer != null) {
                cdataBuffer.append(ch, start, length);
            } else {
                wrap(() -> writer.writeCharacters(ch, start, length));
            }
        }

        @Override
        public void startCDATA() throws SAXException {
            if (cdataBuffer != null) {
                throw new IllegalStateException();
            }
            cdataBuffer = new StringBuilder();
        }

        @Override
        public void endCDATA() throws SAXException {
            if (cdataBuffer == null) {
                throw new IllegalStateException();
            }
            wrap(() -> writer.writeCData(cdataBuffer.toString()));
            cdataBuffer = null;
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            wrap(() -> {
                writer.writeCharacters("\n");
                if (!Strings.isNullOrEmpty(publicId)) {
                    writer.writeDTD(String.format("<!DOCTYPE %s PUBLIC \"%s\" \"%s\">", name, publicId, systemId));
                } else {
                    writer.writeDTD(String.format("<!DOCTYPE %s  \"%s\">", name, systemId));
                }
                writer.writeCharacters("\n");
            });
        }

        @Override
        public void endDTD() throws SAXException {
            super.endDTD();
        }

        @FunctionalInterface
        private interface WriteCommand {
            void run() throws XMLStreamException;
        }

        private static final class RuntimeXMLStreamException extends RuntimeException {
            RuntimeXMLStreamException(XMLStreamException cause) {
                super(cause);
            }

            @Override
            public XMLStreamException getCause() {
                return (XMLStreamException) super.getCause();
            }
        }
    }
}