package it.fb.sqlpp.it.fb.sqlpp.mybatis;

import it.fb.sqlpp.StatementLayout2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import java.util.ArrayList;
import java.util.List;

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
                String formatted = format(toFormat.toString());
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

    private String format(String toFormat) {
        List<String> parameters = new ArrayList<>(32);
        StringBuilder safeString = new StringBuilder();
        {
            int lastParamEndIdx = 0;
            int paramIdx;
            // TODO: Pericoloso, non sto facendo parse dell'SQL
            // TODO: Considerare anche i ${
            while (-1 != (paramIdx = toFormat.indexOf("#{", lastParamEndIdx))) {
                int closeParamIdx = toFormat.indexOf('}', paramIdx + 2);
                if (closeParamIdx == -1) {
                    throw new IllegalStateException("Unclosed parameter found");
                }
                safeString.append(toFormat, lastParamEndIdx, paramIdx);
                safeString.append('?');
                lastParamEndIdx = closeParamIdx + 1;
                parameters.add(toFormat.substring(paramIdx, closeParamIdx + 1));
            }
            safeString.append(toFormat, lastParamEndIdx, toFormat.length());
        }
        String formattedString = StatementLayout2.format(lineWidth, indentWidth, safeString.toString());
        StringBuilder expandedString = new StringBuilder();
        for (int i = 0; i < formattedString.length(); i++) {
            char ch = formattedString.charAt(i);
            // TODO: Pericoloso, non sto facendo parse dell'SQL
            if (ch == '?') {
                expandedString.append(parameters.remove(0));
            } else {
                expandedString.append(ch);
            }
        }
        return expandedString.toString();
    }
}
