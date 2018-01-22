/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
import java.util.LinkedList;
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

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtilsException.Code;

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
	
	public static Element appendElementNS(Element parentElem, String namespace, String elemName) {
		return (Element) parentElem.appendChild(parentElem.getOwnerDocument().createElementNS(namespace, elemName));
	}

	public static Node appendElementWithText(Element parentElem, String elemName, String text) {
		Element childElem = appendElement(parentElem, elemName);
		Text textNode = parentElem.getOwnerDocument().createTextNode(text);
		childElem.appendChild(textNode);
		return textNode;
	}
	
	public static Element appendElement(Element parentElem, String elemName) {
		Element elem = (Element) parentElem.appendChild(parentElem.getOwnerDocument().createElement(elemName));
		return elem;
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

	
	public static byte[] documentToStream(Document document, int indent) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		documentToStream(document, baos, indent);
		return baos.toByteArray();
	}

	public static void documentToStream(Document document, OutputStream outputStream, int indent) {
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
		documentToStream(document, out, 4);
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

	public static List<Element> getXPathElements(Node startNode, XPathExpression xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		return nodeListToElems(resultNodeList);
	}
	
	public static List<Element> getXPathElements(Node startNode, String xPathExpression) {
		NodeList resultNodeList = (NodeList) runXPath(startNode, xPathExpression, XPathConstants.NODESET);
		return nodeListToElems(resultNodeList);
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

	public static Element getXPathElement(Node startNode, XPathExpression xPathExpression) {
		return (Element) runXPath(startNode, xPathExpression, XPathConstants.NODE);
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

	
	public static Integer getXPathInt(Node startNode, XPathExpression xPathExpression) {
		String xPathString = getXPathString(startNode, xPathExpression);
		if(xPathString == null) {
			return null;
		}
		return Integer.parseInt(xPathString);
	}

	public static Double getXPathDouble(Node startNode, XPathExpression xPathExpression) {
		String xPathString = getXPathString(startNode, xPathExpression);
		if(xPathString == null) {
			return null;
		}
		return Double.parseDouble(xPathString);
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

	
	public static XPathExpression createXPathExpression(XPath xPathEngine, String xPathString) {
		XPathExpression searchResultExpression;
		try {
			searchResultExpression = xPathEngine.compile(xPathString);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		return searchResultExpression;
	}
	
	public static XPath createXPathEngine() {
		XPath xpath = XPathFactory.newInstance().newXPath();
		return xpath;
	}

	// http://stackoverflow.com/questions/4079565/xpath-selecting-multiple-elements-with-predicates
	public static String alternateElemsXPath(Collection<String> elementNames) {
		return "*["+String.join("|", elementNames.stream().map(s -> "self::"+s).collect(Collectors.toList()))+"]";
	}

	public static Document documentFromBytes(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			return documentFromStream(bais);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new GlueXmlUtilsException(e, Code.XML_PARSE_EXCEPTION, e.getLocalizedMessage());
		}
	}

	// WARNING! this could get confused if the XML start pattern was embedded e.g. in a CDATA section.
	public static List<Document> documentsFromBytes(byte[] bytes) {
		List<Document> documents = new ArrayList<Document>();
		byte[] xmlStartPattern = "<?xml".getBytes();
		int startIndex = 0;
		int nextStartIndex;
		do {
			nextStartIndex = ByteScanningUtils.indexOf(bytes, xmlStartPattern, startIndex+1);
			int length;
			if(nextStartIndex == -1) { 
				length = bytes.length - startIndex;
			} else {
				length = nextStartIndex - startIndex;
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes, startIndex, length);
			try {
				documents.add(documentFromStream(bais));
			} catch (SAXException e) {
				throw new GlueXmlUtilsException(e, Code.XML_PARSE_EXCEPTION, e.getLocalizedMessage());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			startIndex = nextStartIndex;
		} while(nextStartIndex != -1);
		return documents;
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
	
	public static void stripWhitespace(Node node) {
		NodeList childNodes = node.getChildNodes();
		// check if this node has any children which are elements.
		boolean anyChildElements = false;
		for(int i = 0; i< childNodes.getLength(); i++) {
			if(childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				anyChildElements = true;
				break;
			}
		}
		List<Node> nodesToRemove = new LinkedList<Node>();
		if(anyChildElements) {
			for(int i = 0; i< childNodes.getLength(); i++) {
				Node childNode = childNodes.item(i);
				if(childNode.getNodeType() == Node.ELEMENT_NODE) {
					stripWhitespace(childNode);
				} else if(childNode.getNodeType() == Node.TEXT_NODE) {
					Text childText = (Text) childNode;
					if(childText.getNodeValue().trim().isEmpty()) {
						nodesToRemove.add(childNode);
					}
				}
			}
			for(Node childNode: nodesToRemove) {
				node.removeChild(childNode);
			}
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

