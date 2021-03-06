package it.fb.sqlpp.it.fb.sqlpp.mybatis;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class MybatisFormatter {

    private static final String MYBATIS_DTD_PUBLIC_ID = "-//mybatis.org//DTD Mapper 3.0//EN";
    private static final EntityResolver2 RESOLVER = new DefaultHandler2() {
        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
            if (MYBATIS_DTD_PUBLIC_ID.equals(publicId)) {
                return new InputSource(Resources.asByteSource(Resources.getResource("mybatis-3-mapper.dtd")).openStream());
            }
            return super.resolveEntity(name, publicId, baseURI, systemId);
        }
    };

    public static boolean isMapperFile(Path path) {
        if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
            return false;
        }
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        try {
            AtomicReference<Boolean> isMybatis = new AtomicReference<>(null);
            SAXParser saxParser = spf.newSAXParser();
            try (InputStream in = Files.newInputStream(path)) {
                XMLReader reader = saxParser.getXMLReader();
                reader.setEntityResolver(RESOLVER);
                reader.setContentHandler(new DefaultHandler() {
                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                        if (isMybatis.get() != null) {
                            return;
                        }
                        isMybatis.set(localName.equals("mapper"));
                    }
                });
                reader.parse(new InputSource(in));
            }
            return Optional.ofNullable(isMybatis.get()).orElse(false);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
    }

    public static void format(InputStream input, OutputStream output, int lineWidth, int indentWidth) throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        MybatisFilter filter = new MybatisFilter(saxParser.getXMLReader(), lineWidth, indentWidth);
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", filter);
        CopyHandler handler = new CopyHandler(output);
        filter.setContentHandler(handler);
        filter.setEntityResolver(RESOLVER);
        try {
            filter.parse(new InputSource(input));
        } catch (CopyHandler.RuntimeXMLStreamException ex) {
            throw ex.getCause();
        }
    }

    private static class CopyHandler extends DefaultHandler2 {

        private final XMLStreamWriter writer;
        private StringBuilder cdataBuffer = null;
        private DelayedWriteCommand delayedElement;

        CopyHandler(OutputStream output) throws XMLStreamException {
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            writer = outputFactory.createXMLStreamWriter(output, "UTF-8");
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
            wrap(() -> writer.writeStartDocument("UTF-8", "1.0"));
        }

        @Override
        public void endDocument() {
            wrap(writer::writeEndDocument);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            flushDelayedElement();
            // Delay writing the element until we know if it's empty or not
            // Must copy the attributes: the parser could reuse the same instance
            AttributesImpl fixedAttributes = new AttributesImpl(attributes);
            delayedElement = (XmlConsumer<String> tagWriter1, XmlBiConsumer<String, String> tagWriter2) -> {
                if (Strings.isNullOrEmpty(uri)) {
                    tagWriter1.accept(localName);
                } else {
                    tagWriter2.accept(uri, localName);
                }
                for (int i = 0; i < fixedAttributes.getLength(); i++) {
                    String attrUri = fixedAttributes.getURI(i);
                    if (Strings.isNullOrEmpty(attrUri)) {
                        writer.writeAttribute(fixedAttributes.getLocalName(i), fixedAttributes.getValue(i));
                    } else {
                        writer.writeAttribute(attrUri, fixedAttributes.getLocalName(i), fixedAttributes.getValue(i));
                    }
                }
            };
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (delayedElement != null) {
                wrap(() -> delayedElement.run(writer::writeEmptyElement, writer::writeEmptyElement));
                delayedElement = null;
            } else {
                wrap(writer::writeEndElement);
            }
        }

        private void flushDelayedElement() {
            if (delayedElement != null) {
                wrap(() -> delayedElement.run(writer::writeStartElement, writer::writeStartElement));
                delayedElement = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (length == 0) {
                return;
            }
            flushDelayedElement();
            if (cdataBuffer != null) {
                cdataBuffer.append(ch, start, length);
            } else {
                wrap(() -> writer.writeCharacters(ch, start, length));
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            flushDelayedElement();
            if (cdataBuffer != null) {
                cdataBuffer.append(ch, start, length);
            } else {
                wrap(() -> writer.writeCharacters(ch, start, length));
            }
        }

        @Override
        public void startCDATA() {
            flushDelayedElement();
            if (cdataBuffer != null) {
                throw new IllegalStateException();
            }
            cdataBuffer = new StringBuilder();
        }

        @Override
        public void endCDATA() {
            flushDelayedElement();
            if (cdataBuffer == null) {
                throw new IllegalStateException();
            }
            wrap(() -> writer.writeCData(cdataBuffer.toString()));
            cdataBuffer = null;
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
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

        @FunctionalInterface
        private interface DelayedWriteCommand {

            void run(XmlConsumer<String> tagWriter1, XmlBiConsumer<String, String> tagWriter2) throws XMLStreamException;
        }

        @FunctionalInterface
        private interface XmlConsumer<T> {

            void accept(T value) throws XMLStreamException;
        }

        @FunctionalInterface
        private interface XmlBiConsumer<S, T> {

            void accept(S value1, T value2) throws XMLStreamException;
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
