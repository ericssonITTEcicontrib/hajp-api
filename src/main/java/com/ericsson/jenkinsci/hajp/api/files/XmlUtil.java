package com.ericsson.jenkinsci.hajp.api.files;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Set;

/**
 * Xml utility class for changing/merging xml documents, providing methods to convert preserved
 * fields to/from String and to merge config xml while preserving fields.
 */
public class XmlUtil {

    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    private static XPath xPath = XPathFactory.newInstance().newXPath();

    /**
     * Save the PreservedFields object into an xml
     *
     * @param preservedFields the PreservedFields object
     * @throws Exception if any
     */
    public static String saveToXml(PreservedFields preservedFields) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PreservedFields.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        OutputStream os = new ByteArrayOutputStream();
        jaxbMarshaller.marshal(preservedFields, os);
        String xml = os.toString();
        os.close();

        return xml;
    }

    /**
     * Load the PreservedFields object from the xml
     *
     * @param xml the preservedFields xml
     * @return the PreservedFields object converted from the xml
     * @throws Exception if any
     */
    public static PreservedFields xmlToPreservedFields(String xml) throws Exception {
        InputStream is = new ByteArrayInputStream(xml.getBytes());
        JAXBContext jaxbContext = JAXBContext.newInstance(PreservedFields.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        PreservedFields preservedFields = (PreservedFields) jaxbUnmarshaller.unmarshal(is);
        is.close();

        return preservedFields;
    }

    /**
     * Merge 2 xml documents into one by preserving some fields from the original xml
     * while updating other fields from the incoming xml xml. The resulting document is save into
     * the result xml.
     *
     * @param origXml         the original xml
     * @param incomingXml     the incoming xml
     * @param preservedFields a set of preserved fields
     * @throws Exception if any
     */
    public static String mergeXmlByPreservingField(String origXml, String incomingXml,
        Set<String> preservedFields) throws Exception {
        Document origDoc = xmlToDocument(origXml);
        Document incomingDoc = xmlToDocument(incomingXml);
        Document mergedDoc = mergeXmlByPreservingField(origDoc, incomingDoc, preservedFields);

        return docToString(mergedDoc);
    }

    private static Document mergeXmlByPreservingField(Document origDoc, Document incomingDoc,
        Set<String> preservedFields) throws Exception {
        for (String preservedField : preservedFields) {
            NodeList origNode = findNodeByXpath(origDoc, preservedField);
            NodeList incomingNode = findNodeByXpath(incomingDoc, preservedField);
            if (origNode != null && origNode != null) {
                for (int i = 0; i < incomingNode.getLength(); i++) {
                    incomingNode.item(i).getFirstChild()
                        .setNodeValue(origNode.item(i).getFirstChild().getNodeValue());
                }
            }
        }
        return incomingDoc;
    }

    /**
     * Marshall the xml to a document
     *
     * @param xml the xml
     * @return the xml document parsed from the
     * @throws Exception if any
     */
    public static Document xmlToDocument(String xml) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputStream incomingIs = new ByteArrayInputStream(xml.getBytes());
        Document doc = db.parse(incomingIs);
        incomingIs.close();

        return doc;
    }

    /**
     * Find the node matching the xpath in the xml document
     *
     * @param doc   the xml document
     * @param xpath the xpath expression
     * @return the node list matching the xpath, null if not found
     */
    public static NodeList findNodeByXpath(Document doc, String xpath) {
        try {
            return (NodeList) xPath.compile(xpath).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private static String docToString(Document doc) throws Exception {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);

        return result.getWriter().toString();
    }
}
