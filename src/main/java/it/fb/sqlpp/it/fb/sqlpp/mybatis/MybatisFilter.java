package it.fb.sqlpp.it.fb.sqlpp.mybatis;

import it.fb.sqlpp.StatementLayout2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

class MybatisFilter extends XMLFilterImpl implements LexicalHandler {

    private final int lineWidth;
    private final int indentWidth;
    private final StringBuilder currentPath = new StringBuilder();
    private final StringBuilder toFormat = new StringBuilder();
    private boolean formatting;

    MybatisFilter(XMLReader parent, int lineWidth, int indentWidth) {
        super(parent);
        this.lineWidth = lineWidth;
        this.indentWidth = indentWidth;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        currentPath.append('/').append(localName);
        switch (currentPath.toString()) {
            case "/mapper/select":
            case "/mapper/insert":
            case "/mapper/update":
            case "/mapper/delete":
                startFormatting();
                break;
            default:
                cancelFormatting();
        }
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (formatting) {
            try {
                String formatted = StatementLayout2.format(lineWidth, indentWidth, toFormat.toString());
                super.characters("\n".toCharArray(), 0, 1);
                super.characters(formatted.toCharArray(), 0, formatted.length());
                super.characters("\n    ".toCharArray(), 0, 5);
            } catch (it.fb.sqlpp.ParseException ex) {
                // TODO: Logging?
                super.characters(toFormat.toString().toCharArray(), 0, toFormat.length());
            }
            toFormat.setLength(0);
        }
        super.endElement(uri, localName, qName);
        currentPath.setLength(currentPath.length() - localName.length() - 1);
    }

    @Override
    public void startEntity(String name) throws SAXException {
        cancelFormatting();
        if (getContentHandler() instanceof LexicalHandler) {
            ((LexicalHandler) getContentHandler()).startEntity(name);
        }
    }

    @Override
    public void endEntity(String name) throws SAXException {
        cancelFormatting();
        if (getContentHandler() instanceof LexicalHandler) {
            ((LexicalHandler) getContentHandler()).endEntity(name);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (length == 0) {
            return;
        }
        if (formatting) {
            toFormat.append(ch, start, length);
        } else {
            super.characters(ch, start, length);
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (!formatting) {
            if (getContentHandler() instanceof LexicalHandler) {
                ((LexicalHandler) getContentHandler()).startCDATA();
            }
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (!formatting) {
            if (getContentHandler() instanceof LexicalHandler) {
                ((LexicalHandler) getContentHandler()).endCDATA();
            }
        }
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        // No idea how to maintain the position of the comment after formatting took place
        if (!formatting) {
            if (getContentHandler() instanceof LexicalHandler) {
                ((LexicalHandler) getContentHandler()).comment(ch, start, length);
            }
        }
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        cancelFormatting();
        if (getContentHandler() instanceof LexicalHandler) {
            ((LexicalHandler) getContentHandler()).startDTD(name, publicId, systemId);
        }
    }

    @Override
    public void endDTD() throws SAXException {
        cancelFormatting();
        if (getContentHandler() instanceof LexicalHandler) {
            ((LexicalHandler) getContentHandler()).endDTD();
        }
    }

    private void cancelFormatting() throws SAXException {
        super.characters(toFormat.toString().toCharArray(), 0, toFormat.length());
        toFormat.setLength(0);
        formatting = false;
    }

    private void startFormatting() {
        formatting = true;
    }
}
