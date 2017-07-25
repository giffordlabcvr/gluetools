package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.importing.SequenceImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.DeleteSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.GenbankXmlSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.HttpUtils;

@PluginClass(elemName="ncbiImporter")
public class NcbiImporter extends SequenceImporter<NcbiImporter> {

	// name of a VARCHAR field on sequence table which will be used to cache GI number
	private static final String GI_NUMBER_FIELD_NAME = "giNumberFieldName";
	// maximum downloaded in a single import step.
	private static final String MAX_DOWNLOADED = "maxDownloaded";
	// which GenBank XML field is used as the sequence ID
	private static final String SEQUENCE_ID_FIELD = "sequenceIdField";

	// setting left in for backwards compatibility only: pretty much has to be GENBANK_XML
	private static final String SEQUENCE_FORMAT = "sequenceFormat";
	
	// number of sequences to try to retrieve with each search
	private static final String E_FETCH_BATCH_SIZE = "eFetchBatchSize";
	
	// max number of sequences to retrieve with each search
	private static final String E_SEARCH_RET_MAX = "eSearchRetMax";
	
	// string in eSearch syntax to use for search.
	private static final String E_SEARCH_TERM = "eSearchTerm";
	
	// source where the sequences will be stored
	private static final String SOURCE_NAME = "sourceName";
	
	// NCBI database: should normally be nuccore 
	private static final String DATABASE = "database";
	
	private static String eUtilsBaseURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";

	public enum SequenceIdField {
		GI_NUMBER,
		PRIMARY_ACCESSION,
		ACCESSION_VERSION
	}
	
	private String sourceName;
	private String database;
	private String eSearchTerm = null;
	private int eSearchRetMax;
	private int eFetchBatchSize;
	private List<String> specificGiNumbers;
	private List<String> specificPrimaryAccessions;
	private List<String> specificAccessionVersions;
	private SequenceIdField sequenceIdField;
	private String giNumberFieldName;
	private Integer maxDownloaded = null;
	
	public NcbiImporter() {
		super();
		addModulePluginCmdClass(NcbiImporterSyncCommand.class);
		addModulePluginCmdClass(NcbiImporterImportCommand.class);
		addModulePluginCmdClass(NcbiImporterPreviewCommand.class);
		addSimplePropertyName(DATABASE);
		addSimplePropertyName(E_FETCH_BATCH_SIZE);
		addSimplePropertyName(E_SEARCH_RET_MAX);
		addSimplePropertyName(E_SEARCH_TERM);
		addSimplePropertyName(GI_NUMBER_FIELD_NAME);
		addSimplePropertyName(SOURCE_NAME);
		addSimplePropertyName(SEQUENCE_ID_FIELD);
		addSimplePropertyName(SEQUENCE_FORMAT);
		addSimplePropertyName(MAX_DOWNLOADED);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element ncbiImporterElem) {
		super.configure(pluginConfigContext, ncbiImporterElem);
		database = PluginUtils.configureStringProperty(ncbiImporterElem, DATABASE, "nuccore");
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(ncbiImporterElem, SOURCE_NAME, false)).orElse("ncbi-"+database);
		eSearchTerm = PluginUtils.configureStringProperty(ncbiImporterElem, E_SEARCH_TERM, false);
		specificGiNumbers = PluginUtils.configureStrings(ncbiImporterElem, "specificGiNumbers/giNumber/text()", false);
		specificPrimaryAccessions = PluginUtils.configureStrings(ncbiImporterElem, "specificPrimaryAccessions/primaryAccession/text()", false);
		specificAccessionVersions = PluginUtils.configureStrings(ncbiImporterElem, "specificAccessionVersions/accessionVersion/text()", false);
		eSearchRetMax = PluginUtils.configureIntProperty(ncbiImporterElem, E_SEARCH_RET_MAX, 4000);
		eFetchBatchSize = PluginUtils.configureIntProperty(ncbiImporterElem, E_FETCH_BATCH_SIZE, 200);
		PluginUtils.configureStringProperty(ncbiImporterElem, SEQUENCE_FORMAT, Arrays.asList("GENBANK_XML"), false);
		sequenceIdField = Optional.ofNullable(PluginUtils.configureEnumProperty(SequenceIdField.class, 
				ncbiImporterElem, SEQUENCE_ID_FIELD, false)).orElse(SequenceIdField.GI_NUMBER);
		maxDownloaded = PluginUtils.configureIntProperty(ncbiImporterElem, MAX_DOWNLOADED, false);
		giNumberFieldName = PluginUtils.configureStringProperty(ncbiImporterElem, GI_NUMBER_FIELD_NAME, "gb_gi_number");
		
		String overwriteExisting = PluginUtils.configureStringProperty(ncbiImporterElem, "overwriteExisting", false);
		if(overwriteExisting != null) {
			log(Level.WARNING, "The <overwriteExisting> element is deprecated. Please remove it.");
		}
		
		if(!(
				(eSearchTerm != null && specificGiNumbers.isEmpty() && specificPrimaryAccessions.isEmpty() && specificAccessionVersions.isEmpty()) ||
				(eSearchTerm == null && !specificGiNumbers.isEmpty() && specificPrimaryAccessions.isEmpty() && specificAccessionVersions.isEmpty()) ||
				(eSearchTerm == null && specificGiNumbers.isEmpty() && !specificPrimaryAccessions.isEmpty() && specificAccessionVersions.isEmpty()) ||
				(eSearchTerm == null && specificGiNumbers.isEmpty() && specificPrimaryAccessions.isEmpty() && !specificAccessionVersions.isEmpty())
			)) {
			searchTermConfigError();
		}
		if(!specificPrimaryAccessions.isEmpty()) {
			eSearchTerm = primaryAccessionsToESearchTerm(specificPrimaryAccessions);
		} else if(!specificAccessionVersions.isEmpty()) {
			eSearchTerm = accessionVersionsToESearchTerm(specificAccessionVersions);
		} 
	}

	private String primaryAccessionsToESearchTerm(List<String> primaryAccessions) {
		List<String> disjuncts = primaryAccessions.stream()
				.map(primaryAcc -> "\""+primaryAcc+"\"[Primary Accession]")
				.collect(Collectors.toList());
		return String.join(" OR ", disjuncts);
	}

	private String accessionVersionsToESearchTerm(List<String> accessionVersions) {
		List<String> disjuncts = accessionVersions.stream()
				.map(accessionVersion -> "\""+accessionVersion+"\"[Accession Version]")
				.collect(Collectors.toList());
		return String.join(" OR ", disjuncts);
	}
	
	private void searchTermConfigError() {
		throw new NcbiImporterException(Code.CONFIG_ERROR, "Exactly one of <eSearchTerm>, <specificGiNumbers>, <specificPrimaryAccessions> or <specificAccessionVersions> must be specified.");
	}

	// Return the set of GI numbers for sequences which match the eSearchTerm, or the specific GiNumbers list if applicable.
	private Set<String> getGiNumbersMatching(CommandContext cmdContext, String eSearchTerm, List<String> specificGiNumbers) {
		Set<String> giNumbersMatching = new LinkedHashSet<String>();
		if(specificGiNumbers != null && !specificGiNumbers.isEmpty()) {
			giNumbersMatching.addAll(specificGiNumbers);
		} else {
			try(CloseableHttpClient httpClient = createHttpClient(cmdContext)) {
				HttpUriRequest eSearchHttpRequest = createESearchRequest(eSearchTerm);
				log("Sending eSearch request to NCBI");
				Document eSearchResponseDoc = runHttpRequestGetDocument("eSearch", eSearchHttpRequest, httpClient);
				log("NCBI eSearch response received");
				checkForESearchErrors(eSearchResponseDoc);
				giNumbersMatching.addAll(GlueXmlUtils.getXPathStrings(eSearchResponseDoc, "/eSearchResult/IdList/Id/text()"));
				log(giNumbersMatching.size()+" GI numbers returned in eSearch response");
			} catch (IOException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eSearch", e.getLocalizedMessage());
			}
		}
		log("NCBI sequences matching specification: "+giNumbersMatching.size());
		return giNumbersMatching;
	}
	
	// Return a map of GI number to sequenceID for sequences that already exist in the source.
	private Map<String, String> getGiNumbersExisting(CommandContext cmdContext) {
		Map<String, String> giNumbersExisting = new LinkedHashMap<String, String>();
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), true);
		if(source == null) {
			return giNumbersExisting;
		}
		log("Finding sequences in source "+sourceName);
		SelectQuery selectQuery = new SelectQuery(Sequence.class, ExpressionFactory.matchExp(Sequence.SOURCE_NAME_PATH, sourceName));
		int numSequences = GlueDataObject.count(cmdContext, selectQuery);
		log("Found "+numSequences+" sequences.");
		int updates = 0;
		int foundInField = 0; 
		int foundInDocument = 0;
		int batchSize = 1000;
		selectQuery.setFetchLimit(batchSize); 
		selectQuery.setPageSize(batchSize);
		log("Checking for GI numbers in sequences in source \""+sourceName+"\"");
		int startIndex = 0;
		while(startIndex < numSequences) {
			selectQuery.setFetchOffset(startIndex);
			List<Sequence> sequences = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			for(Sequence sequence: sequences) {
				if(!sequence.getFormat().equals(SequenceFormat.GENBANK_XML.name())) {
					continue;
				}
				String giNumber = null;
				Object giNumberObj = sequence.readProperty(giNumberFieldName);
				if(giNumberObj != null) {
					giNumber = giNumberObj.toString();
					foundInField++;
				}
				Document gbDocument = null;
				if(giNumber == null) {
					gbDocument = ((GenbankXmlSequenceObject) sequence.getSequenceObject()).getDocument();
					giNumber = giNumberFromDocument(gbDocument);
					if(giNumber != null) {
						foundInDocument++;
						sequence.writeProperty(giNumberFieldName, giNumber);
					}
				}
				if(giNumber != null) {
					updates++;
					giNumbersExisting.put(giNumber, sequence.getSequenceID());
				}
			}
			log("Existing sequences found: "+giNumbersExisting.size());
			log(foundInField+" GI numbers in field, "+foundInDocument+" in document");
			if(updates > 0) {
				cmdContext.commit();
			}
			cmdContext.newObjectContext();
			updates = 0;
			startIndex = startIndex + batchSize;
		}
		return giNumbersExisting;
	}

	private CloseableHttpClient createHttpClient(CommandContext cmdContext) {
		// ignore cookies, in order to prevent a log warning resulting from NCBI's incorrect
		// implementation of the spec.
		
		Builder globalConfigBuilder = RequestConfig.custom();
		globalConfigBuilder.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
		RequestConfig globalConfig = globalConfigBuilder.build();
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(globalConfig);
		setProxyIfSpecified(cmdContext, httpClientBuilder);
		CloseableHttpClient httpClient = httpClientBuilder.build();
		return httpClient;
	}

	private void setProxyIfSpecified(CommandContext cmdContext,
			HttpClientBuilder httpClientBuilder) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		if(propertiesConfiguration.getPropertyValue(HttpUtils.HTTP_PROXY_ENABLED, "false").equals("true")) {
			String proxyHostName = propertiesConfiguration.getPropertyValue(HttpUtils.HTTP_PROXY_HOST);
			if(proxyHostName == null) {
				throw new NcbiImporterException(Code.PROXY_ERROR, "Engine property "+HttpUtils.HTTP_PROXY_HOST+" was null");
			}
			String proxyPortString = propertiesConfiguration.getPropertyValue(HttpUtils.HTTP_PROXY_PORT);
			if(proxyPortString == null) {
				throw new NcbiImporterException(Code.PROXY_ERROR, "Engine property "+HttpUtils.HTTP_PROXY_PORT+" was null");
			}
			int proxyPort = 0;
			try {
				proxyPort = Integer.parseInt(proxyPortString);
			} catch(NumberFormatException nfe) {
				throw new NcbiImporterException(nfe, Code.PROXY_ERROR, "Engine property "+HttpUtils.HTTP_PROXY_PORT+" was not an integer");
			}
			HttpHost proxyHttpHost = new HttpHost(proxyHostName, proxyPort, "http");
			httpClientBuilder.setProxy(proxyHttpHost);
		}
	}

	private List<RetrievedSequence> fetchBatch(CommandContext cmdContext, List<String> giNumbers) {
		List<RetrievedSequence> retrievedSequences = new ArrayList<RetrievedSequence>();
		if(giNumbers.isEmpty()) {
			return retrievedSequences;
		}
		Object eFetchResponseObject;
		try(CloseableHttpClient httpClient = createHttpClient(cmdContext)) {
			HttpUriRequest eFetchRequest = createEFetchRequest(giNumbers);
				log("Requesting "+giNumbers.size()+" sequences from NCBI via eFetch");
				eFetchResponseObject = runHttpRequestGetDocument("eFetch", eFetchRequest, httpClient);
				log("NCBI eFetch response received");
		} catch (IOException e) {
			throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eFetch", e.getLocalizedMessage());
		}
		List<Document> individualGBFiles = divideDocuments((Document) eFetchResponseObject);
		for(Document individualFile: individualGBFiles) {
			RetrievedSequence retrievedSequence = new RetrievedSequence();
			retrievedSequence.format = SequenceFormat.GENBANK_XML;
			retrievedSequence.sequenceID = null;
			Document individualDocument = (Document) individualFile;
			String giNumber = giNumberFromDocument(individualDocument);
			retrievedSequence.giNumber = giNumber;
			retrievedSequence.data = GlueXmlUtils.prettyPrint(individualDocument);
			if(sequenceIdField == SequenceIdField.PRIMARY_ACCESSION) {
				retrievedSequence.sequenceID = primaryAccessionFromDocument(individualDocument);
			} else if(sequenceIdField == SequenceIdField.GI_NUMBER) {
				retrievedSequence.sequenceID = giNumber;
			} else if(sequenceIdField == SequenceIdField.ACCESSION_VERSION) {
				retrievedSequence.sequenceID = accessionVersionFromDocument(individualDocument);
			}
			if(retrievedSequence.sequenceID == null) {
				throw new NcbiImporterException(NcbiImporterException.Code.NULL_SEQUENCE_ID, new String(retrievedSequence.data));
			}
			retrievedSequences.add(retrievedSequence);
		}
		return retrievedSequences;
	}

	private String primaryAccessionFromDocument(Document individualDocument) {
		return GlueXmlUtils.getXPathString(individualDocument, "/GBSeq/GBSeq_primary-accession/text()");
	}

	private String accessionVersionFromDocument(Document individualDocument) {
		return GlueXmlUtils.getXPathString(individualDocument, "/GBSeq/GBSeq_accession-version/text()");
	}

	private String giNumberFromDocument(Document document) {
		List<String> seqIds = GlueXmlUtils.getXPathStrings(document, "/GBSeq/GBSeq_other-seqids/GBSeqid/text()");
		for(String seqId: seqIds) {
			if(seqId.startsWith("gi|")) {
				return seqId.replace("gi|", "");
			}
		}
		return null;
	}


	
	// lists all the databases
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi
	
	// meta-data on database nuccore
	// http://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi?db=nuccore&version=2.0



	private List<Document> divideDocuments(Document parentDocument) {
		List<Element> elems = GlueXmlUtils.getXPathElements(parentDocument, "/*/*");
		return elems.stream().map(elem -> {
			Document subDoc = GlueXmlUtils.newDocument();
			subDoc.appendChild(subDoc.importNode(elem, true));
			return subDoc;
		}).collect(Collectors.toList());
	}

	private HttpUriRequest createEFetchRequest(List<String> giNumbers)  {
		String giNumbersString = String.join(",", giNumbers.toArray(new String[]{}));


		// rettype=gb and retmode=xml means retrieve GenBank XML files.
		// Other formats are possible.
		// http://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/?report=objectonly
		
		String rettype = "gb";
		String retmode = "xml";

		String url = eUtilsBaseURL+"/efetch.fcgi?db="+database+"&rettype="+rettype+"&retmode="+retmode;
		HttpPost httpPost = new HttpPost(url);

		StringEntity requestEntity;
		try {
			requestEntity = new StringEntity("id="+giNumbersString);
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

	@SuppressWarnings("unused")
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
				throw new NcbiImporterException(NcbiImporterException.Code.PROTOCOL_ERROR, requestName, response.getStatusLine().toString());
			}

			HttpEntity entity = response.getEntity();
			result = entityConsumer.consumeEntity(requestName, entity);
			
			// ensure it is fully consumed
			try {
				EntityUtils.consume(entity);
			} catch (IOException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
		} catch (ClientProtocolException cpe) {
			throw new NcbiImporterException(cpe, NcbiImporterException.Code.PROTOCOL_ERROR, requestName, cpe.getLocalizedMessage());
		} catch (IOException ioe) {
			throw new NcbiImporterException(ioe, NcbiImporterException.Code.IO_ERROR, requestName, ioe.getLocalizedMessage());
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
				return GlueXmlUtils.documentFromStream(entity.getContent());
			} catch (SAXException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.FORMATTING_ERROR, requestName, e.getLocalizedMessage());
			} catch (IOException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
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
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
			try {
				return IOUtils.toString(inputStream, charset.name());
			} catch (IOException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, requestName, e.getLocalizedMessage());
			}
			
		}
	}

	
	private HttpUriRequest createESearchRequest(String eSearchTerm) {
		
		String url = eUtilsBaseURL+"/esearch.fcgi?db="+database+"&retmax="+eSearchRetMax;

		StringEntity requestEntity;
		try {
			String termOnOneLine = eSearchTerm.trim().replaceAll("[\\n\\r\\t]", "");
			requestEntity = new StringEntity("term="+termOnOneLine);
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
				throw new NcbiImporterException(NcbiImporterException.Code.SEARCH_ERROR, mainError.getTextContent());
			}
			String queryErrorXpathExpression = "/eSearchResult/ErrorList/*";
			Node queryError = (Node) xpath.evaluate(queryErrorXpathExpression, document, XPathConstants.NODE);
			if(queryError != null) {
				throw new NcbiImporterException(NcbiImporterException.Code.SEARCH_ERROR, queryError.getNodeName()+": "+queryError.getTextContent());
			}
			String queryWarningXpathExpression = "/eSearchResult/WarningList/*";
			Node queryWarning = (Node) xpath.evaluate(queryWarningXpathExpression, document, XPathConstants.NODE);
			if(queryWarning != null) {
				throw new NcbiImporterException(NcbiImporterException.Code.SEARCH_ERROR, queryWarning.getNodeName()+": "+queryWarning.getTextContent());
			}
		} catch(XPathException xpe) {
			throw new RuntimeException(xpe);
		}
	}

	public NcbiImporterStatus doPreview(CommandContext cmdContext) {
		
		// the set of GI Numbers matching the search spec
		Set<String> matchingGiNumbers = getGiNumbersMatching(cmdContext, eSearchTerm, specificGiNumbers);

		// the set of GI Numbers existing in the source
		Map<String, String> existingGiNumbers = getGiNumbersExisting(cmdContext);

		// the set of GI Numbers which both match the search spec and exist in the source.
		Map<String, String> presentGiNumbers = new LinkedHashMap<String, String>();
		existingGiNumbers.forEach((k, v) -> {if(matchingGiNumbers.contains(k)) { presentGiNumbers.put(k, v); } });

		// the set of GI Numbers which match the search spec but are not in the source
		Set<String> missingGiNumbers = new LinkedHashSet<String>(matchingGiNumbers);
		missingGiNumbers.removeAll(existingGiNumbers.keySet());

		// the set of GI Numbers which should be deleted from the source
		Map<String, String> surplusGiNumbers = new LinkedHashMap<String, String>(existingGiNumbers);
		matchingGiNumbers.forEach(key -> surplusGiNumbers.remove(key));

		int recordsFound = matchingGiNumbers.size();
		return new NcbiImporterStatus(recordsFound, presentGiNumbers, missingGiNumbers, surplusGiNumbers, 
				Collections.emptyMap(), Collections.emptySet());
	}


	
	public NcbiImporterStatus doImport(CommandContext cmdContext) {
		NcbiImporterStatus ncbiImporterStatus = doPreview(cmdContext);
		downloadMissing(cmdContext, ncbiImporterStatus);
		return ncbiImporterStatus;
	}

	private void downloadMissing(CommandContext cmdContext,
			NcbiImporterStatus ncbiImporterStatus) {
		List<String> giNumbersToDownload = new ArrayList<String>(ncbiImporterStatus.getMissingGiNumbers());
		if(maxDownloaded != null && giNumbersToDownload.size() > maxDownloaded) {
			giNumbersToDownload = giNumbersToDownload.subList(0, maxDownloaded);
		}
		log("Total number of to be downloaded in this run: "+giNumbersToDownload.size());
		int batchStart = 0;
		int batchEnd;
		Map<String, String> giNumbersDownloaded = new LinkedHashMap<String, String>();
		ensureSourceExists(cmdContext, sourceName);
		do {
			batchEnd = Math.min(batchStart+eFetchBatchSize, giNumbersToDownload.size());
			List<RetrievedSequence> batchSequences = fetchBatch(cmdContext, giNumbersToDownload.subList(batchStart, batchEnd));
			for(RetrievedSequence sequence: batchSequences) {
				String sequenceID = sequence.sequenceID;
				SequenceFormat format = sequence.format;
				byte[] sequenceData = sequence.data;
				Sequence existing = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
				if(existing != null) {
					// shouldn't really happen but sometimes does.
					GlueLogger.getGlueLogger().warning("Source "+sourceName+", Sequence "+sequenceID+" already exists: not updated.");
					continue;
				}
				Sequence newSequence = null;
				Document gbXmlDocument = null;
				createSequence(cmdContext, sourceName, sequenceID, format, sequenceData);
				newSequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
				gbXmlDocument = ((GenbankXmlSequenceObject) newSequence.getSequenceObject()).getDocument();
				newSequence.writeProperty(giNumberFieldName, giNumberFromDocument(gbXmlDocument));
				cmdContext.commit();
				giNumbersDownloaded.put(sequence.giNumber, sequenceID);
			}
			cmdContext.newObjectContext();
			log("Sequences downloaded: "+giNumbersDownloaded.size());
			batchStart = batchEnd;
		} while(batchEnd < giNumbersToDownload.size());
		ncbiImporterStatus.setDownloadedGiNumbers(giNumbersDownloaded);
	}

	class RetrievedSequence {
		public String sequenceID;
		String giNumber;
		SequenceFormat format;
		byte[] data;
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.sequence.name(), giNumberFieldName, FieldType.VARCHAR, true);
	}

	public NcbiImporterStatus doSync(CommandContext cmdContext) {
		NcbiImporterStatus ncbiImporterStatus = doPreview(cmdContext);
		// deleteSurplus must happen before downloadMissing because
		// a surplus sequence and its replacement may have the same sequence ID if PRIMARY_ACCESSION is used.
		deleteSurplus(cmdContext, ncbiImporterStatus);
		downloadMissing(cmdContext, ncbiImporterStatus);
		return ncbiImporterStatus;
	}

	private void deleteSurplus(CommandContext cmdContext,
			NcbiImporterStatus ncbiImporterStatus) {
		int batchSize = 50;
		int numDeleted = 0;
		List<Map.Entry<String, String>> surplusGiNumbers = new ArrayList<Map.Entry<String, String>>(ncbiImporterStatus.getSurplusGiNumbers().entrySet());
		Set<String> deletedGiNumbers = new LinkedHashSet<String>();
		int startIndex = 0;
		while(startIndex < surplusGiNumbers.size()) {
			List<String> sequenceIdBatch = new ArrayList<String>();
			for(int i = startIndex; i < Math.min(startIndex+batchSize, surplusGiNumbers.size()); i++) {
				Map.Entry<String, String> entry = surplusGiNumbers.get(i);
				deletedGiNumbers.add(entry.getKey());
				sequenceIdBatch.add(entry.getValue());
			}
			SelectQuery selectQuery = new SelectQuery(Sequence.class, ExpressionFactory.inExp(Sequence.SEQUENCE_ID_PROPERTY, sequenceIdBatch));
			List<Sequence> seqsToDelete = GlueDataObject.query(cmdContext, Sequence.class, selectQuery);
			int deletedThisBatch = DeleteSequenceCommand.deleteSequences(cmdContext, seqsToDelete);
			log("Deleted "+deletedThisBatch+" sequences.");
			numDeleted += deletedThisBatch;
			startIndex += batchSize;
		}
		log("In total, deleted "+numDeleted+" sequences.");
		
		ncbiImporterStatus.setDeletedGiNumbers(deletedGiNumbers);
	}

}
