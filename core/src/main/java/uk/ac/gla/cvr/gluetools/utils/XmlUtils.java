package uk.ac.gla.cvr.gluetools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlUtils {

	public static Document documentFromStream(InputStream is) throws SAXException, IOException {
		DocumentBuilder dBuilder = getDocumentBuilder();
		Document doc = dBuilder.parse(is);
		return doc;
	}

	private static DocumentBuilder getDocumentBuilder() {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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

	public static void prettyPrint(Document document, OutputStream out) {
		prettyPrint(document, out, 4);
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

