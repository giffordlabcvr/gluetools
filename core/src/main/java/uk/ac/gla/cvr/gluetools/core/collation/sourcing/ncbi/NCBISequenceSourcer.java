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
import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileUtils;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcer;
import uk.ac.gla.cvr.gluetools.core.collation.sourcing.SequenceSourcerException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class NCBISequenceSourcer implements SequenceSourcer {

	public static final String ELEM_NAME = "ncbiSequenceSourcer";
	
	private String eUtilsBaseURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	private String dbName;
	private String eSearchTerm = null;
	private int eSearchRetMax;
	// could make format configurable later.
	private CollatedSequenceFormat collatedSequenceFormat = CollatedSequenceFormat.GENBANK_FLAT_FILE;

	@Override
	public void configure(Element sequenceSourcerElem) {
		dbName = PluginUtils.configureString(sequenceSourcerElem, "database/text()", "nuccore");
		eSearchTerm = PluginUtils.configureString(sequenceSourcerElem, "eSearchTerm/text()", true);
		eSearchRetMax = PluginUtils.configureInt(sequenceSourcerElem, "eSearchRetMax/text()", 1000);
	}

	@Override
	public List<String> getSequenceIDs() {
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
		return ELEM_NAME+":"+dbName+":"+collatedSequenceFormat.name();
	}

	@Override
	public List<CollatedSequence> retrieveSequences(List<String> sequenceIDs)
			 {
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
		List<String> individualGBFiles = GenbankFlatFileUtils.divideConcatenatedGBFiles(eFetchResponseString);
		int i = 0;
		for(String sequenceID: sequenceIDs) {
			if(i >= individualGBFiles.size()) {
				throw new SequenceSourcerException(SequenceSourcerException.Code.INSUFFICIENT_SEQUENCES_RETURNED);
			}
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setFormat(collatedSequenceFormat);
			collatedSequence.setSourceUniqueID(getSourceUniqueID());
			collatedSequence.setSequenceSourceID(sequenceID);
			collatedSequence.setSequenceText(individualGBFiles.get(i));
			resultSequences.add(collatedSequence);
			i++;
		}
		return resultSequences;
	}
	
	// lists all the databases
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi
	
	// meta-data on database nuccore
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=nuccore&version=2.0


	private HttpUriRequest createEFetchRequest(List<String> idsToFetch)  {
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
			 {
		return (Document) runHttpRequestGetObject(requestName, httpRequest, httpClient, new DocumentConsumer());
	}

	private String runHttpRequestGetString(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient) 
			 {
		return (String) runHttpRequestGetObject(requestName, httpRequest, httpClient, new StringConsumer());
	}

	
	private Object runHttpRequestGetObject(String requestName, HttpUriRequest httpRequest, CloseableHttpClient httpClient, 
			EntityConsumer<?> entityConsumer) 
			 {
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
		public abstract P consumeEntity(String requestName, HttpEntity entity) ;
	}

	private static class DocumentConsumer extends EntityConsumer<Document> {
		@Override
		public Document consumeEntity(String requestName, HttpEntity entity)
				 {
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
				 {
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
			requestEntity = new StringEntity("term="+eSearchTerm.trim());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		requestEntity.setContentType("application/x-www-form-urlencoded");
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(requestEntity);
		return httpPost;
	}



	private void checkForESearchErrors(Document document)  {
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
