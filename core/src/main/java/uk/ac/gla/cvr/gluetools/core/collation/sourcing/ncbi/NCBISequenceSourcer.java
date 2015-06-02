package uk.ac.gla.cvr.gluetools.core.collation.sourcing.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequenceFormat;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerConfigException;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerException;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class NCBISequenceSourcer implements SequenceSourcer {

	public static final String TYPE = "NCBISequenceSourcer";
	
	private String eUtilsBaseURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	private String dbName;
	private String eSearchTerm;
	private int eSearchRetMax;
	// could make format configurable later.
	private CollatedSequenceFormat collatedSequenceFormat = CollatedSequenceFormat.GENBANK_FLAT_FILE;

	@Override
	public void configure(Element sequenceSourcerElem) throws SequenceSourcerConfigException {
		dbName = XmlUtils.getXPathString(sequenceSourcerElem, "database/text()", "nuccore");
		eSearchTerm = XmlUtils.getXPathString(sequenceSourcerElem, "eSearchTerm/text()");
		if(eSearchTerm == null) {
			throw new SequenceSourcerConfigException(Code.REQUIRED_ELEMENT_MISSING, "eSearchTerm");
		}
		try {
			eSearchRetMax = Integer.parseInt(XmlUtils.getXPathString(sequenceSourcerElem, 
					"eSearchRetMax/text()", "1000"));
		} catch(NumberFormatException nfe) {
			throw new SequenceSourcerConfigException(Code.CONFIG_VALUE_FORMAT_ERROR, "eSearchRetMax");
		}
	}

	@Override
	public List<String> getSequenceIDs() throws SequenceSourcerException {
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eSearchHttpRequest = createESearchRequest();
			Document eSearchResponseDoc = runHttpRequestGetDocument("eSearch", eSearchHttpRequest, httpClient);
			checkForESearchErrors(eSearchResponseDoc);
			return XmlUtils.getXPathStrings(eSearchResponseDoc, "/eSearchResult/IdList/Id/text()");
		} catch (IOException e) {
			throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, "eSearch", e.getLocalizedMessage());
		}
	}

	private CloseableHttpClient createHttpClient() {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		return httpClient;
	}

	@Override
	public String getSourceUniqueID() {
		return TYPE+":"+dbName+":"+collatedSequenceFormat.name();
	}

	@Override
	public List<CollatedSequence> retrieveSequences(List<String> sequenceIDs)
			throws SequenceSourcerException {
		String eFetchResponseString;
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eFetchRequest = createEFetchRequest(sequenceIDs);
			eFetchResponseString = runHttpRequestGetString("eFetch", eFetchRequest, httpClient);
		} catch (IOException e) {
			throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, "eFetch", e.getLocalizedMessage());
		}
		if(collatedSequenceFormat != CollatedSequenceFormat.GENBANK_FLAT_FILE) {
			throw new SequenceSourcerException(SequenceSourcerException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
					collatedSequenceFormat.name());
		}
		List<CollatedSequence> resultSequences = new ArrayList<CollatedSequence>();
		int startIndex = 0;
		int endIndex;
		for(String sequenceID: sequenceIDs) {
			endIndex = eFetchResponseString.indexOf("//", startIndex) + 2;
			if(endIndex > eFetchResponseString.length()) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.INSUFFICIENT_SEQUENCES_RETURNED);
			}
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setFormat(collatedSequenceFormat);
			collatedSequence.setSourceUniqueID(getSourceUniqueID());
			collatedSequence.setSequenceSourceID(sequenceID);
			collatedSequence.setSequenceText(eFetchResponseString.substring(startIndex, endIndex));
			resultSequences.add(collatedSequence);
			startIndex = endIndex+1;
		}
		return resultSequences;
	}
	
	private String searchTerm = "\"Hepatitis C\"[Organism] AND 7000:10000[SLEN]";

	// lists all the databases
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi
	
	// meta-data on database nuccore
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=nuccore&version=2.0


	private HttpUriRequest createEFetchRequest(List<String> idsToFetch) throws SequenceSourcerException {
		String commaSeparatedIDs = String.join(",", idsToFetch.toArray(new String[]{}));


		// rettype=gb and retmode=text means retrieve GenBank flat files.
		// Other formats are possible.
		// http://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/?report=objectonly
		
		if(collatedSequenceFormat != CollatedSequenceFormat.GENBANK_FLAT_FILE) {
			throw new SequenceSourcerException(SequenceSourcerException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
					collatedSequenceFormat.name());
		}
		
		String url = eUtilsBaseURL+"/efetch.fcgi?db="+dbName+"&rettype=gb&retmode=text";
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



	

	

	private Document runHttpRequestGetDocument(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient) 
			throws SequenceSourcerException {
		return (Document) runHttpRequestGetObject(requestName, httpRequest, httpClient, new DocumentConsumer());
	}

	private String runHttpRequestGetString(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient) 
			throws SequenceSourcerException {
		return (String) runHttpRequestGetObject(requestName, httpRequest, httpClient, new StringConsumer());
	}

	
	private Object runHttpRequestGetObject(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient, 
			EntityConsumer<?> entityConsumer) 
			throws SequenceSourcerException {
		Object result;

		try(CloseableHttpResponse response = httpClient.execute(httpRequest);) {
			if(response.getStatusLine().getStatusCode() != 200) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.PROTOCOL_ERROR, requestName, response.getStatusLine().toString());
			}

			HttpEntity entity = response.getEntity();
			result = entityConsumer.consumeEntity(requestName, entity);
			
			// ensure it is fully consumed
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
		} catch (ClientProtocolException cpe) {
			throw new SequenceSourcerException(cpe, SequenceSourcerException.Code.PROTOCOL_ERROR, requestName, cpe.getLocalizedMessage());
		} catch (IOException ioe) {
			throw new SequenceSourcerException(ioe, SequenceSourcerException.Code.IO_ERROR, requestName);
		}
		return result;
	}

	
	
	private abstract static class EntityConsumer<P> {
		public abstract P consumeEntity(String requestName, HttpEntity entity) throws SequenceSourcerException;
	}

	private static class DocumentConsumer extends EntityConsumer<Document> {
		@Override
		public Document consumeEntity(String requestName, HttpEntity entity)
				throws SequenceSourcerException {
			try {
				return XmlUtils.documentFromStream(entity.getContent());
			} catch (SAXException e) {
				throw new SequenceSourcerException(e, SequenceSourcerException.Code.FORMATTING_ERROR, requestName, e.getLocalizedMessage());
			} catch (IOException e) {
				throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
		}
	}

	private static class StringConsumer extends EntityConsumer<String> {
		@Override
		public String consumeEntity(String requestName, HttpEntity entity)
				throws SequenceSourcerException {
			ContentType contentType = ContentType.getOrDefault(entity);
			Charset charset = contentType.getCharset();
			if(charset == null) { charset = Charset.forName("UTF-8"); }
			InputStream inputStream = null;
			try {
				inputStream = entity.getContent();
			} catch (IOException e) {
				throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
			try {
				return IOUtils.toString(inputStream, charset.name());
			} catch (IOException e) {
				throw new SequenceSourcerException(e, SequenceSourcerException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
			
		}
	}

	
	private HttpUriRequest createESearchRequest() {
		
		String url = eUtilsBaseURL+"/esearch.fcgi?db="+dbName+"&retmax="+eSearchRetMax;

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



	private void checkForESearchErrors(Document document) throws SequenceSourcerException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			String mainErrorXpathExpression = "/eSearchResult/ERROR/text()";
			Node mainError = (Node) xpath.evaluate(mainErrorXpathExpression, document, XPathConstants.NODE);
			if(mainError != null) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.SEARCH_ERROR, mainError.getTextContent());
			}
			String queryErrorXpathExpression = "/eSearchResult/ErrorList/*";
			Node queryError = (Node) xpath.evaluate(queryErrorXpathExpression, document, XPathConstants.NODE);
			if(queryError != null) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.SEARCH_ERROR, queryError.getNodeName()+": "+queryError.getTextContent());
			}
			String queryWarningXpathExpression = "/eSearchResult/WarningList/*";
			Node queryWarning = (Node) xpath.evaluate(queryWarningXpathExpression, document, XPathConstants.NODE);
			if(queryWarning != null) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.SEARCH_ERROR, queryWarning.getNodeName()+": "+queryWarning.getTextContent());
			}
		} catch(XPathException xpe) {
			throw new RuntimeException(xpe);
		}
	}
	
	

	
	
}
