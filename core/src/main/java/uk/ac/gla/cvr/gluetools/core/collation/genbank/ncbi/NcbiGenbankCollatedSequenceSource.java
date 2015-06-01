package uk.ac.gla.cvr.gluetools.core.collation.genbank.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.CollatedSequenceSource;
import uk.ac.gla.cvr.gluetools.core.collation.SequenceCollationException;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.genbank.ParsedGenbank;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class NcbiGenbankCollatedSequenceSource implements CollatedSequenceSource<ParsedGenbank> {

	private static final int eSearchRetMax = 10;
	private static final int eFetchBatchSize = 3;

	private String dbName = "nuccore";
	
	private String eUtilsBase = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	
	
	private String searchTerm = "\"Hepatitis C\"[Organism] AND 7000:10000[SLEN]";

	// lists all the databases
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi
	
	// meta-data on database nuccore
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=nuccore&version=2.0

	
	@Override
	public void updateSequences() throws SequenceCollationException {
			CloseableHttpClient httpClient = HttpClients.createDefault();

			HttpUriRequest eSearchHttpRequest = createESearchRequest();
			Document eSearchResponseDoc = runHttpRequestGetDocument("eSearch", eSearchHttpRequest, httpClient);
			
			XmlUtils.prettyPrint(eSearchResponseDoc, System.out);
			
			checkForESearchErrors(eSearchResponseDoc);

			LinkedList<String> responseIDs = new LinkedList<String>(extractESearchResponseIDs(eSearchResponseDoc));

			List<String> idsOfNextBatchToFetch = new LinkedList<String>();
			while(!responseIDs.isEmpty()) {
				String responseID = responseIDs.remove(0);
				if(!collatedSequencePresent(responseID)) {
					idsOfNextBatchToFetch.add(responseID);
					if(idsOfNextBatchToFetch.size() == eFetchBatchSize || responseIDs.isEmpty()) {
						if(!idsOfNextBatchToFetch.isEmpty()) {
							HttpUriRequest eFetchRequest = createEFetchRequest(idsOfNextBatchToFetch);
							idsOfNextBatchToFetch.clear();
							String eFetchResponseString = runHttpRequestGetString("eFetch", eFetchRequest, httpClient);
							System.out.print(eFetchResponseString);
						}
					}
				}
			}
			
			
			
			
		
	}

	private boolean collatedSequencePresent(String sequenceId) {
		return false;
	}

	private HttpUriRequest createEFetchRequest(List<String> idsToFetch) {
		String commaSeparatedIDs = String.join(",", idsToFetch.toArray(new String[]{}));


		// rettype=gb and retmode=text means retrieve GenBank flat files.
		// Other formats are possible.
		// http://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/?report=objectonly
		
		String url = eUtilsBase+"/efetch.fcgi?db="+dbName+"&rettype=gb&retmode=text";
		HttpPost httpPost = new HttpPost(url);

		StringEntity requestEntity;
		try {
			requestEntity = new StringEntity("id="+commaSeparatedIDs);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		requestEntity.setContentType("application/x-www-form-urlencoded");
		httpPost.setEntity(requestEntity);
		return httpPost;
	}



	private List<String> extractESearchResponseIDs(Document eSearchResponseDoc) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		String idTextXpathExpression = "/eSearchResult/IdList/Id/text()";
		NodeList idNodeTextList;
		try {
			idNodeTextList = (NodeList) xpath.evaluate(idTextXpathExpression, eSearchResponseDoc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		List<String> responseIDs = new ArrayList<String>();
		for(int i = 0; i < idNodeTextList.getLength(); i++) {
			Node idNodeText = idNodeTextList.item(i);
			if(idNodeText instanceof Text) {
				responseIDs.add(((Text) idNodeText).getWholeText());
			}
		}
		return responseIDs;
	}


	private String extractESearchWebEnv(Document eSearchResponseDoc) throws SequenceCollationException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		String webenvTextXpathExpression = "/eSearchResult/WebEnv/text()";
		Node webenvTextNode;
		try {
			webenvTextNode = (Node) xpath.evaluate(webenvTextXpathExpression, eSearchResponseDoc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if(webenvTextNode == null) {
			throw new SequenceCollationException("eSearch response document did not contain WebEnv node.");
		}
		return (((Text) webenvTextNode).getWholeText());
	}

	private int extractESearchCount(Document eSearchResponseDoc) throws SequenceCollationException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		String countTextXpathExpress = "/eSearchResult/Count/text()";
		Node countNode;
		try {
			countNode = (Node) xpath.evaluate(countTextXpathExpress, eSearchResponseDoc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if(countNode == null) {
			throw new SequenceCollationException("eSearch response document did not contain Count node.");
		}
		String countText = ((Text) countNode).getWholeText();
		try {
			return Integer.parseInt(countText);
		} catch(NumberFormatException nfe) {
			throw new SequenceCollationException("eSearch response document Count node content was not an integer.");
		}
	}

	private int extractESearchQueryKey(Document eSearchResponseDoc) throws SequenceCollationException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		String queryKeyTextXpathExpress = "/eSearchResult/QueryKey/text()";
		Node queryKeyNode;
		try {
			queryKeyNode = (Node) xpath.evaluate(queryKeyTextXpathExpress, eSearchResponseDoc, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
		if(queryKeyNode == null) {
			throw new SequenceCollationException("eSearch response document did not contain QueryKey node.");
		}
		String queryKeyText = ((Text) queryKeyNode).getWholeText();
		try {
			return Integer.parseInt(queryKeyText);
		} catch(NumberFormatException nfe) {
			throw new SequenceCollationException("eSearch response document QueryKey node content was not an integer.");
		}
	}


	private Document runHttpRequestGetDocument(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient) 
			throws SequenceCollationException {
		return (Document) runHttpRequestGetObject(requestName, httpRequest, httpClient, new DocumentConsumer());
	}

	private String runHttpRequestGetString(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient) 
			throws SequenceCollationException {
		return (String) runHttpRequestGetObject(requestName, httpRequest, httpClient, new StringConsumer());
	}

	
	private Object runHttpRequestGetObject(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient, 
			EntityConsumer<?> entityConsumer) 
			throws SequenceCollationException {
		Object result;

		try(CloseableHttpResponse response = httpClient.execute(httpRequest);) {
			if(response.getStatusLine().getStatusCode() != 200) {
				throw new SequenceCollationException(requestName+" HTTP request failed: "+response.getStatusLine().toString());
			}

			HttpEntity entity = response.getEntity();
			result = entityConsumer.consumeEntity(requestName, entity);
			
			// ensure it is fully consumed
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				throw new SequenceCollationException("IOException during consume of "+requestName+" response: "+e.getMessage(), e);
			}
		} catch (ClientProtocolException cpe) {
			throw new SequenceCollationException("ClientProtocolException during "+requestName+" HTTP request: "+cpe.getMessage(), cpe);
		} catch (IOException ioe) {
			throw new SequenceCollationException("IOException during "+requestName+" response close: "+ioe.getMessage(), ioe);
		}
		return result;
	}

	
	
	private abstract static class EntityConsumer<P> {
		public abstract P consumeEntity(String requestName, HttpEntity entity) throws SequenceCollationException;
	}

	private static class DocumentConsumer extends EntityConsumer<Document> {
		@Override
		public Document consumeEntity(String requestName, HttpEntity entity)
				throws SequenceCollationException {
			try {
				return XmlUtils.documentFromStream(entity.getContent());
			} catch (SAXException e) {
				throw new SequenceCollationException("SAXException during "+requestName+" response XML document parsing: "+e.getMessage(), e);
			} catch (IOException e) {
				throw new SequenceCollationException("IOException during "+requestName+" response XML document parsing: "+e.getMessage(), e);
			}
		}
	}

	private static class StringConsumer extends EntityConsumer<String> {
		@Override
		public String consumeEntity(String requestName, HttpEntity entity)
				throws SequenceCollationException {
			ContentType contentType = ContentType.getOrDefault(entity);
			Charset charset = contentType.getCharset();
			if(charset == null) { charset = Charset.forName("UTF-8"); }
			InputStream inputStream = null;
			try {
				inputStream = entity.getContent();
			} catch (IOException e) {
				throw new SequenceCollationException("IOException during "+requestName+" response getContent(): "+e.getMessage(), e);
			}
			try {
				return IOUtils.toString(inputStream, charset.name());
			} catch (IOException e) {
				throw new SequenceCollationException("IOException during "+requestName+" response conversion to string: "+e.getMessage(), e);
			}
			
		}
	}

	
	private HttpUriRequest createESearchRequest() {
		
		String url = eUtilsBase+"/esearch.fcgi?db="+dbName+"&retmax="+eSearchRetMax;

		StringEntity requestEntity;
		try {
			requestEntity = new StringEntity("term="+searchTerm);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		requestEntity.setContentType("application/x-www-form-urlencoded");
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(requestEntity);
		return httpPost;
	}



	private void checkForESearchErrors(Document document) throws SequenceCollationException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			String mainErrorXpathExpression = "/eSearchResult/ERROR/text()";
			Node mainError = (Node) xpath.evaluate(mainErrorXpathExpression, document, XPathConstants.NODE);
			if(mainError != null) {
				throw new SequenceCollationException("NCBI eSearch error: "+mainError.getTextContent());
			}
			String queryErrorXpathExpression = "/eSearchResult/ErrorList/*";
			Node queryError = (Node) xpath.evaluate(queryErrorXpathExpression, document, XPathConstants.NODE);
			if(queryError != null) {
				throw new SequenceCollationException("NCBI eSearch query error: "+
						queryError.getNodeName()+": "+queryError.getTextContent());
			}
			String queryWarningXpathExpression = "/eSearchResult/WarningList/*";
			Node queryWarning = (Node) xpath.evaluate(queryWarningXpathExpression, document, XPathConstants.NODE);
			if(queryWarning != null) {
				throw new SequenceCollationException("NCBI eSearch query warning: "+
						queryWarning.getNodeName()+": "+queryWarning.getTextContent());
			}
		} catch(XPathException xpe) {
			throw new RuntimeException(xpe);
		}
	}



	@Override
	public List<ParsedGenbank> getSequences() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
}
