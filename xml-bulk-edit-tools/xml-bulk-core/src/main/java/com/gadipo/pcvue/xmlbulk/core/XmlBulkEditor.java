package com.gadipo.pcvue.xmlbulk.core;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class XmlBulkEditor {

    public EditResult edit(String xml, EditRequest request) {
        try {
            Document document = parseXml(xml);
            SearchCriteria criteria = request.criteria() == null
                    ? new SearchCriteria(null, null, null, null, null, null, null, null)
                    : request.criteria();

            List<Element> candidates = collectMatchingElements(document, criteria);
            List<ChangeRecord> changes = new ArrayList<>();

            for (Element element : candidates) {
                applyOperation(document, element, request, changes);
            }

            String outputXml = request.dryRun() ? xml : toXml(document);
            return new EditResult(outputXml, candidates.size(), changes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to edit XML: " + ex.getMessage(), ex);
        }
    }

    private List<Element> collectMatchingElements(Document document, SearchCriteria criteria) {
        List<Element> matches = new ArrayList<>();
        Pattern textRegex = compileRegex(criteria.textRegex());
        NodeList allElements = document.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            if (matchesCriteria(element, criteria, textRegex)) {
                matches.add(element);
            }
        }
        return matches;
    }

    private boolean matchesCriteria(Element element, SearchCriteria criteria, Pattern textRegex) {
        if (!isBlank(criteria.tagName()) && !Objects.equals(criteria.tagName(), element.getTagName())) {
            return false;
        }

        if (!isBlank(criteria.attributeName())) {
            if (!element.hasAttribute(criteria.attributeName())) {
                return false;
            }
            if (!isBlank(criteria.attributeValueContains())) {
                String value = element.getAttribute(criteria.attributeName());
                if (!value.contains(criteria.attributeValueContains())) {
                    return false;
                }
            }
        } else if (!isBlank(criteria.attributeValueContains())) {
            boolean found = false;
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                if (element.getAttributes().item(i).getNodeValue().contains(criteria.attributeValueContains())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (!isBlank(criteria.textContains()) && !element.getTextContent().contains(criteria.textContains())) {
            return false;
        }

        if (textRegex != null && !textRegex.matcher(element.getTextContent()).find()) {
            return false;
        }

        String path = getPath(element);
        if (!isBlank(criteria.xpathContains()) && !path.contains(criteria.xpathContains())) {
            return false;
        }

        if (!matchesAncestorCriteria(element, criteria.ancestorTagName(), criteria.ancestorTextContains())) {
            return false;
        }

        return true;
    }

    private boolean matchesAncestorCriteria(Element element, String ancestorTagName, String ancestorTextContains) {
        if (isBlank(ancestorTagName) && isBlank(ancestorTextContains)) {
            return true;
        }
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element ancestor = (Element) current;
            if (!isBlank(ancestorTagName) && !ancestorTagName.equals(ancestor.getTagName())) {
                current = current.getParentNode();
                continue;
            }
            if (isBlank(ancestorTextContains) || ancestor.getTextContent().contains(ancestorTextContains)) {
                return true;
            }
            current = current.getParentNode();
        }
        return false;
    }

    private Pattern compileRegex(String regex) {
        if (isBlank(regex)) {
            return null;
        }
        if (regex.length() > 256) {
            throw new IllegalArgumentException("textRegex is too long. Maximum length is 256 characters.");
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid textRegex pattern: " + ex.getMessage(), ex);
        }
    }

    private void applyOperation(Document document, Element element, EditRequest request, List<ChangeRecord> changes) throws Exception {
        String path = getPath(element);
        switch (request.operation()) {
            case REPLACE_TEXT -> {
                String before = element.getTextContent();
                String after = notNull(request.replacement());
                if (!Objects.equals(before, after)) {
                    changes.add(new ChangeRecord(path, "replace-text", before, after));
                    if (!request.dryRun()) {
                        element.setTextContent(after);
                    }
                }
            }
            case REPLACE_ATTRIBUTE_VALUE -> {
                String attributeName = firstNonBlank(request.operationAttributeName(), request.criteria() == null ? null : request.criteria().attributeName());
                if (isBlank(attributeName)) {
                    throw new IllegalArgumentException("Operation REPLACE_ATTRIBUTE_VALUE requires operationAttributeName or criteria.attributeName");
                }
                String before = element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
                String after = notNull(request.replacement());
                if (!Objects.equals(before, after)) {
                    changes.add(new ChangeRecord(path + "/@" + attributeName, "replace-attribute", String.valueOf(before), after));
                    if (!request.dryRun()) {
                        element.setAttribute(attributeName, after);
                    }
                }
            }
            case ADD_ATTRIBUTE -> {
                String attributeName = request.operationAttributeName();
                if (isBlank(attributeName)) {
                    throw new IllegalArgumentException("Operation ADD_ATTRIBUTE requires operationAttributeName");
                }
                String before = element.hasAttribute(attributeName) ? element.getAttribute(attributeName) : null;
                String after = notNull(request.operationAttributeValue());
                if (!Objects.equals(before, after)) {
                    changes.add(new ChangeRecord(path + "/@" + attributeName, "add-attribute", String.valueOf(before), after));
                    if (!request.dryRun()) {
                        element.setAttribute(attributeName, after);
                    }
                }
            }
            case REMOVE_ATTRIBUTE -> {
                String attributeName = request.operationAttributeName();
                if (isBlank(attributeName)) {
                    throw new IllegalArgumentException("Operation REMOVE_ATTRIBUTE requires operationAttributeName");
                }
                if (element.hasAttribute(attributeName)) {
                    String before = element.getAttribute(attributeName);
                    changes.add(new ChangeRecord(path + "/@" + attributeName, "remove-attribute", before, null));
                    if (!request.dryRun()) {
                        element.removeAttribute(attributeName);
                    }
                }
            }
            case ADD_CHILD_XML -> {
                if (isBlank(request.nodeXml())) {
                    throw new IllegalArgumentException("Operation ADD_CHILD_XML requires nodeXml");
                }
                List<Node> children = parseFragment(document, request.nodeXml());
                for (Node child : children) {
                    changes.add(new ChangeRecord(path, "add-node", null, nodeSummary(child)));
                    if (!request.dryRun()) {
                        element.appendChild(child.cloneNode(true));
                    }
                }
            }
            case REMOVE_NODE -> {
                Node parent = element.getParentNode();
                if (parent != null) {
                    changes.add(new ChangeRecord(path, "remove-node", nodeSummary(element), null));
                    if (!request.dryRun()) {
                        parent.removeChild(element);
                    }
                }
            }
        }
    }

    private List<Node> parseFragment(Document document, String fragment) throws Exception {
        String wrapped = "<wrapper>" + fragment + "</wrapper>";
        Document fragmentDocument = parseXml(wrapped);
        NodeList childNodes = fragmentDocument.getDocumentElement().getChildNodes();
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.TEXT_NODE) {
                children.add(document.importNode(node, true));
            }
        }
        return children;
    }

    private Document parseXml(String xml) throws Exception {
        rejectUnsafeXmlConstructs(xml);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private void rejectUnsafeXmlConstructs(String xml) throws SAXException {
        String lower = xml.toLowerCase(Locale.ROOT);
        if (lower.contains("<!doctype") || lower.contains("<!entity")) {
            throw new SAXException("DOCTYPE and ENTITY declarations are not allowed.");
        }
    }

    private String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private String getPath(Node node) {
        List<String> parts = new ArrayList<>();
        Node current = node;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            parts.add(0, current.getNodeName());
            current = current.getParentNode();
        }
        return "/" + String.join("/", parts);
    }

    private String nodeSummary(Node node) {
        return node.getNodeName() + ":" + node.getTextContent();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String notNull(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        return isBlank(second) ? null : second;
    }
}
