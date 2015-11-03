package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
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

import uk.ac.gla.cvr.gluetools.core.collation.importing.SequenceImporter;
import uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi.NcbiImporterException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.GenbankXmlSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="ncbiImporter")
public class NcbiImporter extends SequenceImporter<NcbiImporter> {

	private static String eUtilsBaseURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";

	public enum SequenceIdField {
		GI_NUMBER,
		PRIMARY_ACCESSION
	}
	
	private String sourceName;
	private String database;
	private String eSearchTerm = null;
	private int eSearchRetMax;
	private int eFetchBatchSize;
	private List<String> specificGiNumbers;
	private List<String> specificPrimaryAccessions;
	private SequenceFormat sequenceFormat;
	private SequenceIdField sequenceIdField;
	private String giNumberFieldName;
	private boolean overwriteExisting = false;
	private Integer maxDownloaded = null;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element ncbiImporterElem) {
		database = PluginUtils.configureStringProperty(ncbiImporterElem, "database", "nuccore");
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(ncbiImporterElem, "sourceName", false)).orElse("ncbi-"+database);
		eSearchTerm = PluginUtils.configureStringProperty(ncbiImporterElem, "eSearchTerm", false);
		specificGiNumbers = PluginUtils.configureStrings(ncbiImporterElem, "specificGiNumbers/giNumber/text()", false);
		specificPrimaryAccessions = PluginUtils.configureStrings(ncbiImporterElem, "specificPrimaryAccessions/primaryAccession/text()", false);
		eSearchRetMax = PluginUtils.configureIntProperty(ncbiImporterElem, "eSearchRetMax", 4000);
		eFetchBatchSize = PluginUtils.configureIntProperty(ncbiImporterElem, "eFetchBatchSize", 200);
		sequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, ncbiImporterElem, "sequenceFormat", true);
		sequenceIdField = Optional.ofNullable(PluginUtils.configureEnumProperty(SequenceIdField.class, 
				ncbiImporterElem, "sequenceIdField", false)).orElse(SequenceIdField.GI_NUMBER);
		overwriteExisting = Optional.ofNullable(PluginUtils.configureBooleanProperty(ncbiImporterElem, "overwriteExisting", false)).orElse(false);
		maxDownloaded = PluginUtils.configureIntProperty(ncbiImporterElem, "maxDownloaded", false);
		giNumberFieldName = PluginUtils.configureStringProperty(ncbiImporterElem, "giNumberFieldName", "GB_GI_NUMBER");
		
		
		addProvidedCmdClass(ImportCommand.class);
		addProvidedCmdClass(PreviewCommand.class);
		addProvidedCmdClass(ShowImporterCommand.class);
		addProvidedCmdClass(ConfigureImporterCommand.class);
		if(!(
				(eSearchTerm != null && specificGiNumbers.isEmpty() && specificPrimaryAccessions.isEmpty()) ||
				(eSearchTerm == null && !specificGiNumbers.isEmpty() && specificPrimaryAccessions.isEmpty()) ||
				(eSearchTerm == null && specificGiNumbers.isEmpty() && !specificPrimaryAccessions.isEmpty())
			)) {
			searchTermConfigError();
		}
		if(sequenceFormat != SequenceFormat.GENBANK_XML && sequenceIdField != SequenceIdField.GI_NUMBER) {
			throw new NcbiImporterException(Code.CONFIG_ERROR, "Unless the sequence format is GENBANK_XML, the sequence ID field must be GI_NUMBER");
			
		}
		if(!specificPrimaryAccessions.isEmpty()) {
			List<String> disjuncts = specificPrimaryAccessions.stream()
					.map(primaryAcc -> "\""+primaryAcc+"\"[Primary Accession]")
					.collect(Collectors.toList());
			eSearchTerm = String.join(" OR ", disjuncts);
		}
	}

	private void searchTermConfigError() {
		throw new NcbiImporterException(Code.CONFIG_ERROR, "Exactly one of <eSearchTerm>, <specificGiNumbers> or <specificPrimaryAccessions> must be specified.");
	}

	private void getGiNumbersMatchingAndExisting(CommandContext cmdContext, 
			Set<String> giNumbersMatching, Set<String> giNumbersExisting, 
			boolean cachedGiNumbers) {
		if(!specificGiNumbers.isEmpty()) {
			giNumbersMatching.addAll(specificGiNumbers);
		} else {
			try(CloseableHttpClient httpClient = createHttpClient()) {
				HttpUriRequest eSearchHttpRequest = createESearchRequest();
				GlueLogger.getGlueLogger().finest("Sending eSearch request to NCBI");
				Document eSearchResponseDoc = runHttpRequestGetDocument("eSearch", eSearchHttpRequest, httpClient);
				GlueLogger.getGlueLogger().finest("NCBI eSearch response received");
				checkForESearchErrors(eSearchResponseDoc);
				giNumbersMatching.addAll(GlueXmlUtils.getXPathStrings(eSearchResponseDoc, "/eSearchResult/IdList/Id/text()"));
				GlueLogger.getGlueLogger().finest(giNumbersMatching.size()+" GI numbers returned in eSearch response");
			} catch (IOException e) {
				throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eSearch", e.getLocalizedMessage());
			}
		}
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), true);
		if(source == null) {
			return;
		}

		GlueLogger.getGlueLogger().fine("Finding sequences in source "+sourceName);
		List<Map<String, String>> pkMaps = 
				GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName))
				.getSequences()
				.stream().map(seq -> seq.pkMap())
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" sequences.");
		int updates = 0;
		int foundInField = 0; 
		int foundInDocument = 0;
		int numChecked = 0;
		GlueLogger.getGlueLogger().finest("Checking for GI numbers in sequences in source \""+sourceName+"\"");
		for(Map<String, String> pkMap: pkMaps) {
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, pkMap);
			numChecked++;
			if(!sequence.getFormat().equals(SequenceFormat.GENBANK_XML.name())) {
				continue;
			}
			String giNumber = null;
			if(cachedGiNumbers) {
				Object giNumberObj = sequence.readProperty(giNumberFieldName);
				if(giNumberObj != null) {
					giNumber = giNumberObj.toString();
					foundInField++;
				}
			}
			if(giNumber == null) {
				giNumber = giNumberFromDocument(((GenbankXmlSequenceObject) sequence.getSequenceObject()).getDocument());
				if(giNumber != null) {
					foundInDocument++;
					if(cachedGiNumbers) {
						sequence.writeProperty(giNumberFieldName, giNumber);
						updates++;
					}
				}
			}
			if(giNumber != null) {
				giNumbersExisting.add(giNumber);
			}
			if(numChecked % eFetchBatchSize == 0) {
				GlueLogger.getGlueLogger().fine("Existing sequences found: "+giNumbersExisting.size());
				GlueLogger.getGlueLogger().finest(foundInField+" GI numbers in field, "+foundInDocument+" in document");
				if(updates > 0) {
					cmdContext.commit();
					cmdContext.newObjectContext();
				}
				updates = 0;
			}
		}
		GlueLogger.getGlueLogger().fine("Existing sequences found: "+giNumbersExisting.size());
		GlueLogger.getGlueLogger().finest("Found "+foundInField+" GI numbers in field, "+foundInDocument+" in document");
		if(updates > 0) {
			cmdContext.commit();
			cmdContext.newObjectContext();
		}
	}

	private CloseableHttpClient createHttpClient() {
		// ignore cookies, in order to prevent a log warning resulting from NCBI's incorrect
		// implementation of the spec.
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		return httpClient;
	}

	String getSourceUniqueID() {
		return "ncbiImporter:"+database+":"+sequenceFormat.name();
	}

	List<RetrievedSequence> retrieveSequences(List<String> giNumbers) {
		List<RetrievedSequence> retrievedSequences = new ArrayList<RetrievedSequence>();
		int batchStart = 0;
		int batchEnd;
		do {
			batchEnd = Math.min(batchStart+eFetchBatchSize, giNumbers.size());
			retrievedSequences.addAll(fetchBatch(giNumbers.subList(batchStart, batchEnd)));
			batchStart = batchEnd;
		} while(batchEnd < giNumbers.size());
		return retrievedSequences;
	}

	private List<RetrievedSequence> fetchBatch(List<String> giNumbers) {
		Object eFetchResponseObject;
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eFetchRequest = createEFetchRequest(giNumbers);
			switch(sequenceFormat) {
			case GENBANK_XML:
				GlueLogger.getGlueLogger().finest("Requesting "+giNumbers.size()+" sequences from NCBI via eFetch");
				eFetchResponseObject = runHttpRequestGetDocument("eFetch", eFetchRequest, httpClient);
				GlueLogger.getGlueLogger().finest("NCBI eFetch response received");
				break;
			default:
				throw new NcbiImporterException(NcbiImporterException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
						sequenceFormat.name());
			}
		} catch (IOException e) {
			throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eFetch", e.getLocalizedMessage());
		}
		List<RetrievedSequence> retrievedSequences = new ArrayList<RetrievedSequence>();
		List<Object> individualGBFiles = null;
		switch(sequenceFormat) {
		case GENBANK_XML:
			individualGBFiles = divideDocuments((Document) eFetchResponseObject);
			break;
		default:
			throw new NcbiImporterException(NcbiImporterException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
					sequenceFormat.name());
		}
		
		for(Object individualFile: individualGBFiles) {
			RetrievedSequence retrievedSequence = new RetrievedSequence();
			retrievedSequence.format = sequenceFormat;
			retrievedSequence.sequenceID = null;
			if(individualFile instanceof Document) {
				Document individualDocument = (Document) individualFile;
				retrievedSequence.data = GlueXmlUtils.prettyPrint(individualDocument);
				if(sequenceIdField == SequenceIdField.PRIMARY_ACCESSION) {
					retrievedSequence.sequenceID = primaryAccessionFromDocument(individualDocument);
				} else if(sequenceIdField == SequenceIdField.GI_NUMBER) {
					retrievedSequence.sequenceID = giNumberFromDocument(individualDocument);
				} 
				if(retrievedSequence.sequenceID == null) {
					throw new NcbiImporterException(NcbiImporterException.Code.NULL_SEQUENCE_ID, retrievedSequence.data);
				}
			}
			retrievedSequences.add(retrievedSequence);
		}
		return retrievedSequences;
	}

	private String primaryAccessionFromDocument(Document individualDocument) {
		return GlueXmlUtils.getXPathString(individualDocument, "/GBSeq/GBSeq_primary-accession/text()");
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


	private List<Object> divideDocuments(Document parentDocument) {
		List<Element> elems = GlueXmlUtils.getXPathElements(parentDocument, "/*/*");
		return elems.stream().map(elem -> {
			Document subDoc = GlueXmlUtils.newDocument();
			subDoc.appendChild(subDoc.importNode(elem, true));
//			XmlUtils.prettyPrint(subDoc, System.out);
//			System.out.println("--------------------------------------");
			return subDoc;
		}).collect(Collectors.toList());
	}

	private HttpUriRequest createEFetchRequest(List<String> giNumbers)  {
		String giNumbersString = String.join(",", giNumbers.toArray(new String[]{}));


		// rettype=gb and retmode=xml means retrieve GenBank XML files.
		// Other formats are possible.
		// http://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/?report=objectonly
		
		String rettype;
		String retmode;
		switch(sequenceFormat) {
		case GENBANK_XML:
			rettype="gb";
			retmode="xml";
			break;
			default:
				throw new NcbiImporterException(NcbiImporterException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
						sequenceFormat.name());
		}
		
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
			throw new NcbiImporterException(ioe, NcbiImporterException.Code.IO_ERROR, requestName);
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

	
	private HttpUriRequest createESearchRequest() {
		
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

	public NcbiImporterResult doPreview(CommandContext cmdContext) {
		
		Set<String> matchingGiNumbers = new LinkedHashSet<String>();
		Set<String> existingGiNumbers = new LinkedHashSet<String>();
		boolean cachedGiNumbers = initCachedGiNumbers(cmdContext);
		getGiNumbersMatchingAndExisting(cmdContext, matchingGiNumbers, existingGiNumbers, cachedGiNumbers);
		return new NcbiImporterResult(matchingGiNumbers.size(), existingGiNumbers.size(), 0, 0,
				maxDownloaded, overwriteExisting, eSearchRetMax);
	}


	
	private NcbiImporterResult doImport(CommandContext cmdContext) {
		Set<String> matchingGiNumbers = new LinkedHashSet<String>();
		Set<String> existingGiNumbers = new LinkedHashSet<String>();
		boolean cachedGiNumbers = initCachedGiNumbers(cmdContext);
		getGiNumbersMatchingAndExisting(cmdContext, matchingGiNumbers, existingGiNumbers, cachedGiNumbers);
		Set<String> retrieveSet = new LinkedHashSet<String>(matchingGiNumbers);
		GlueLogger.getGlueLogger().fine("NCBI sequences matching search query: "+retrieveSet.size());
		if(!overwriteExisting) {
			retrieveSet.removeAll(existingGiNumbers);
		}

		List<String> retrieveList = new ArrayList<String>(retrieveSet);
		if(maxDownloaded != null && retrieveList.size() > maxDownloaded) {
			retrieveList = retrieveList.subList(0, maxDownloaded);
		}
		GlueLogger.getGlueLogger().fine("NCBI sequences to download: "+retrieveList.size());
		List<String> giNumbers = new ArrayList<String>(retrieveList);
		int batchStart = 0;
		int batchEnd;
		int recordsAdded = 0;
		int recordsUpdated = 0;
		ensureSourceExists(cmdContext, sourceName);
		do {
			batchEnd = Math.min(batchStart+eFetchBatchSize, giNumbers.size());
			List<RetrievedSequence> batchSequences = fetchBatch(giNumbers.subList(batchStart, batchEnd));
			for(RetrievedSequence sequence: batchSequences) {
				String sequenceID = sequence.sequenceID;
				SequenceFormat format = sequence.format;
				byte[] sequenceData = sequence.data;
				boolean preExisting = false;
				if(overwriteExisting) {
					DeleteResult deleteResult = GlueDataObject.delete(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
					cmdContext.commit();
					if(deleteResult.getNumber() == 1) {
						preExisting = true;
					}
				} else {
					Sequence existing = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
					if(existing != null) {
						// shouldn't really happen but sometimes does.
						GlueLogger.getGlueLogger().warning("Source "+sourceName+", Sequence "+sequenceID+" already exists: not updated.");
						continue;
					}
				}
				createSequence(cmdContext, sourceName, sequenceID, format, sequenceData);
				if(cachedGiNumbers && sequence.format == SequenceFormat.GENBANK_XML) {
					Sequence newSequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
					newSequence.writeProperty(giNumberFieldName, 
							giNumberFromDocument(((GenbankXmlSequenceObject) newSequence.getSequenceObject()).getDocument()));
					cmdContext.commit();
				}
				
				if(preExisting) {
					recordsUpdated++;
				} else {
					recordsAdded++;
				}
			}
			cmdContext.newObjectContext();
			GlueLogger.getGlueLogger().fine("Sequences updated: "+recordsUpdated+", sequences added: "+recordsAdded);
			batchStart = batchEnd;
		} while(batchEnd < giNumbers.size());
		return new NcbiImporterResult(matchingGiNumbers.size(), existingGiNumbers.size(), recordsAdded, recordsUpdated,
				maxDownloaded, overwriteExisting, eSearchRetMax);
	}

	private boolean initCachedGiNumbers(CommandContext cmdContext) {
		boolean cachedGiNumbers = true;
		List<String> customSequenceFieldNames = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject().getCustomSequenceFieldNames();
		if(!customSequenceFieldNames.contains(giNumberFieldName)) {
			cachedGiNumbers = false;
			GlueLogger.getGlueLogger().warning("No sequence field \""+giNumberFieldName+"\" exists in the project. Importer performance will be impeded as a result.");
		}
		return cachedGiNumbers;
	}
	
	
	class RetrievedSequence {
		public String sequenceID;
		String giNumber;
		SequenceFormat format;
		byte[] data;
	}
	
	public static class NcbiImporterResult extends MapResult {

		public NcbiImporterResult(int numGenbankRecordsFound, 
				int numGenbankRecordsPreExisting, 
				int numGenbankRecordsAdded, 
				int numGenbankRecordsUpdated,
				Integer maxDownloadedSetting,
				Boolean overwriteExistingSetting,
				Integer eSearchRetMaxSetting) {
			super("ncbiImporterResult", mapBuilder()
					.put("numGenbankRecordsFound", numGenbankRecordsFound)
					.put("numGenbankRecordsPreExisting", numGenbankRecordsPreExisting)
					.put("numGenbankRecordsAdded", numGenbankRecordsAdded)
					.put("numGenbankRecordsUpdated", numGenbankRecordsUpdated)
					.put("maxDownloadedSetting", maxDownloadedSetting)
					.put("overwriteExistingSetting", overwriteExistingSetting)
					.put("eSearchRetMaxSetting", eSearchRetMaxSetting));
		}
		
	}
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={""},
			metaTags={CmdMeta.updatesDatabase},
			description="Import sequence data from NCBI into the project") 
	public static class ImportCommand extends ModuleProvidedCommand<NcbiImporterResult, NcbiImporter> implements ProvidedProjectModeCommand {
		@Override
		protected NcbiImporterResult execute(CommandContext cmdContext, NcbiImporter importerPlugin) {
			return importerPlugin.doImport(cmdContext);
		}
	}

	
	@CommandClass( 
			commandWords={"preview"}, 
			docoptUsages={""},
			metaTags={},
			description="Preview the NCBI results") 
	public static class PreviewCommand extends ModuleProvidedCommand<NcbiImporterResult, NcbiImporter> implements ProvidedProjectModeCommand {
		@Override
		protected NcbiImporterResult execute(CommandContext cmdContext, NcbiImporter importerPlugin) {
			return importerPlugin.doPreview(cmdContext);
		}
	}

	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this importer") 
	public static class ShowImporterCommand extends ShowConfigCommand<NcbiImporter> {}

	@SimpleConfigureCommandClass(
			propertyNames={"sourceName", "database", "eSearchTerm", 
					"sequenceFormat", "eSearchRetMax", "eFetchBatchSize", 
					"overwriteExisting", "maxDownloaded"}
	)
	public static class ConfigureImporterCommand extends SimpleConfigureCommand<NcbiImporter> {}



}
