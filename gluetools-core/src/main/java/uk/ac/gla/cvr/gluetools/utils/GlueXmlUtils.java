package uk.ac.gla.cvr.gluetools.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class GlueXmlUtils {

	public static Document documentFromStream(InputStream is) throws SAXException, IOException  {
		DocumentBuilder dBuilder = getDocumentBuilder();
		Document doc = dBuilder.parse(is);
		return doc;
	}

	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		return dBuilder;
	}
		
	public static Document newDocument() {
		return getDocumentBuilder().newDocument();
	}
	
	public static Element documentWithElement(String elemName) {
		Document document = newDocument();
		return (Element) document.appendChild(document.createElement(elemName));
	}
	
	public static Element appendElement(Element parentElem, String elemName) {
		return appendElement(parentElem, elemName, null);
	}

	public static Element appendElementNS(Element parentElem, String namespace, String elemName) {
		return (Element) parentElem.appendChild(parentElem.getOwnerDocument().createElementNS(namespace, elemName));
	}

	public static Node appendElementWithText(Element parentElem, String elemName, String text, JsonType jsonType) {
		Element childElem = appendElement(parentElem, elemName, jsonType);
		Text textNode = parentElem.getOwnerDocument().createTextNode(text);
		childElem.appendChild(textNode);
		return textNode;
	}
	
	public static Element appendElement(Element parentElem, String elemName, JsonType jsonType) {
		Element elem = (Element) parentElem.appendChild(parentElem.getOwnerDocument().createElement(elemName));
		if(jsonType != null) { JsonUtils.setJsonType(elem, jsonType, false); };
		return elem;
	}

	public static Node appendElementWithText(Element parentElem, String elemName, String text) {
		return appendElementWithText(parentElem, elemName, text, null);
	}
	
	public static List<Element> findChildElements(Element parentElement, String elemName) {
		return findChildElements(parentElement).stream().
				filter(e -> e.getNodeName().equals(elemName)).collect(Collectors.toList());
	}

	
	public static List<Element> findChildElements(Element parentElement) {
		List<Element> childElems = new ArrayList<Element>();
		NodeList childNodes = parentElement.getChildNodes();
		for(int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if(node instanceof Element) {
				childElems.add((Element) node);
			}
		}
		return childElems;
	}

	
	public static void prettyPrint(Document document, OutputStream outputStream, int indent) {
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    if (indent > 0) {
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
	    }
	    Result result = new StreamResult(outputStream);
	    Source source = new DOMSource(document);
	    try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] prettyPrint(Document document) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		prettyPrint(document, baos);
		return baos.toByteArray();
	}

	public static void prettyPrint(Document document, OutputStream out) {
		prettyPrint(document, out, 4);
	}
	
	public static List<Element> getXPathElements(Node startNode, String xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		return nodeListToElems(resultNodeList);
	}

	public static List<Element> nodeListToElems(NodeList resultNodeList) {
		List<Element> elems = new ArrayList<Element>();
		for(int i = 0; i < resultNodeList.getLength(); i++) {
			elems.add((Element) resultNodeList.item(i));
		}
		return elems;
	}

	public static List<Node> getXPathNodes(Node startNode, XPathExpression xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		return nodeListToNodes(resultNodeList);
	}
	
	public static List<Node> getXPathNodes(Node startNode, String xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		return nodeListToNodes(resultNodeList);
	}

	private static List<Node> nodeListToNodes(NodeList resultNodeList) {
		List<Node> nodes = new ArrayList<Node>();
		for(int i = 0; i < resultNodeList.getLength(); i++) {
			nodes.add(resultNodeList.item(i));
		}
		return nodes;
	}

	public static List<String> getXPathStrings(Node startNode, String xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		List<String> strings = new ArrayList<String>();
		for(int i = 0; i < resultNodeList.getLength(); i++) {
			Node node = resultNodeList.item(i);
			strings.add(((Text) node).getWholeText());
		}
		return strings;
	}

	public static Element getXPathElement(Node startNode, String xPathExpression) {
		return (Element) runXPath(startNode, xPathExpression, XPathConstants.NODE);
	}

	public static Node getXPathNode(Node startNode, String xPathExpression) {
		return (Node) runXPath(startNode, xPathExpression, XPathConstants.NODE);
	}

	public static Node getXPathNode(Node startNode, XPathExpression xPathExpression) {
		return (Node) runXPath(startNode, xPathExpression, XPathConstants.NODE);
	}

	public static String getXPathString(Node startNode, String xPathExpression) {
		Node xPathResult = (Node) runXPath(startNode, xPathExpression, XPathConstants.NODE);
		if(xPathResult == null) { 
			return null; 
		}
		return getNodeText(xPathResult);
	}

	public static String getNodeText(Node node) {
		if(node instanceof Text) {
			return ((Text) node).getWholeText();
		} else if(node instanceof Attr) {
			return ((Attr) node).getTextContent();
		} else {
			throw new RuntimeException("Unable to get text value from Node of type: "+node.getClass().getCanonicalName());
		}
	}

	public static String getXPathString(Node startNode, XPathExpression xPathExpression) {
		Object xPathResult = runXPath(startNode, xPathExpression, XPathConstants.NODE);
		if(xPathResult == null) { 
			return null; 
		}
		return ((Text) xPathResult).getWholeText();
	}
	
	public static String getXPathString(Node startNode, String xPathExpression, String defaultValue) {
		String xPathResult = getXPathString(startNode, xPathExpression);
		if(xPathResult == null) {
			return defaultValue;
		}
		return xPathResult;
	}	

	private static Object runXPath(Node startNode, XPathExpression xPathExpression, QName name) {
		try {
			return xPathExpression.evaluate(startNode, name);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	
	private static Object runXPath(Node startNode, String xPathExpression, QName name) {
		XPath xpath = createXPathEngine();
		try {
			return xpath.evaluate(xPathExpression, startNode, name);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	public static XPath createXPathEngine() {
		XPath xpath = XPathFactory.newInstance().newXPath();
		return xpath;
	}

	// http://stackoverflow.com/questions/4079565/xpath-selecting-multiple-elements-with-predicates
	public static String alternateElemsXPath(Collection<String> elementNames) {
		return "*["+String.join("|", elementNames.stream().map(s -> "self::"+s).collect(Collectors.toList()))+"]";
	}

	public static Document documentFromBytes(byte[] bytes) throws SAXException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			return documentFromStream(bais);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static XPathExpression compileXPathExpression(XPath xPath, String expressionString) {
		try {
			return xPath.compile(expressionString);
		} catch(XPathExpressionException xpee) {
			throw new RuntimeException(xpee);
		}
	}
	
	public static class XmlNamespaceContext implements NamespaceContext {
		private Map<String, String> prefixToNamespaceUri = new LinkedHashMap<String, String>();
		private Map<String, String> namespaceUriToPrefix = new LinkedHashMap<String, String>();
		

		@Override
		public String getNamespaceURI(String prefix) {
			return prefixToNamespaceUri.get(prefix);
		}

		@Override
		public String getPrefix(String namespaceURI) {
			return namespaceUriToPrefix.get(namespaceURI);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Iterator getPrefixes(String namespaceURI) {
			return prefixToNamespaceUri.keySet().iterator();
		}
		
		public void addNamespace(String prefix, String namespaceURI) {
			prefixToNamespaceUri.put(prefix, namespaceURI);
			namespaceUriToPrefix.put(namespaceURI, prefix);
		}
		
	}
	
	
	
		/*
		XPath xpath = XPathFactory.newInstance().newXPath();
		String expression = "/genotype_result/sequence";
		NodeList sequenceNodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);

		System.out.println("sequenceId\tconclusionTaxon\tconclusionTaxonSupport\tblastClusterConclusion\tblastIdentityScore\talignmentFile\tlogFile\ttreeFile");

		for(int i = 0; i < sequenceNodes.getLength(); i++) {
			String sequenceId = xpath.evaluate("@name", sequenceNodes.item(i));
			String conclusionTaxon = xpath.evaluate("conclusion[@type='simple']/assigned/id/text()", sequenceNodes.item(i));
			String conclusionTaxonSupport = xpath.evaluate("conclusion[@type='simple']/assigned/support/text()", sequenceNodes.item(i));
			String blastClusterConclusion = xpath.evaluate("result[@id='blast']/cluster/concluded-name/text()", sequenceNodes.item(i));
			String blastIdentityScore = xpath.evaluate("result[@id='blast']/identity/text()", sequenceNodes.item(i));
			String alignmentFile = xpath.evaluate("result[@id='pure']/alignment/text()", sequenceNodes.item(i));
			String logFile = xpath.evaluate("result[@id='pure']/log/text()", sequenceNodes.item(i));
			String treeFile = xpath.evaluate("result[@id='pure']/tree/text()", sequenceNodes.item(i));
			System.out.println(sequenceId+"\t"+conclusionTaxon+"\t"+conclusionTaxonSupport+"\t"+blastClusterConclusion+
					"\t"+blastIdentityScore+"\t"+alignmentFile+"\t"+logFile+"\t"+treeFile);
		} */
		
	}
