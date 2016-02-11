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
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
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

	private static final String IS_ASSEMBLY_FIELD_NAME = "isAssemblyFieldName";
	private static final String RECURSE_ON_CONTIGS = "recurseOnContigs";
	private static final String GI_NUMBER_FIELD_NAME = "giNumberFieldName";
	private static final String MAX_DOWNLOADED = "maxDownloaded";
	private static final String OVERWRITE_EXISTING = "overwriteExisting";
	private static final String SEQUENCE_ID_FIELD = "sequenceIdField";
	private static final String SEQUENCE_FORMAT = "sequenceFormat";
	private static final String E_FETCH_BATCH_SIZE = "eFetchBatchSize";
	private static final String E_SEARCH_RET_MAX = "eSearchRetMax";
	private static final String E_SEARCH_TERM = "eSearchTerm";
	private static final String SOURCE_NAME = "sourceName";
	private static final String DATABASE = "database";
	
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
	private boolean recurseOnContigs = false;
	private String isAssemblyFieldName;
	
	public NcbiImporter() {
		super();
		addModulePluginCmdClass(ImportCommand.class);
		addModulePluginCmdClass(PreviewCommand.class);
		addSimplePropertyName(DATABASE);
		addSimplePropertyName(E_FETCH_BATCH_SIZE);
		addSimplePropertyName(E_SEARCH_RET_MAX);
		addSimplePropertyName(E_SEARCH_TERM);
		addSimplePropertyName(GI_NUMBER_FIELD_NAME);
		addSimplePropertyName(SOURCE_NAME);
		addSimplePropertyName(SEQUENCE_ID_FIELD);
		addSimplePropertyName(SEQUENCE_FORMAT);
		addSimplePropertyName(IS_ASSEMBLY_FIELD_NAME);
		addSimplePropertyName(OVERWRITE_EXISTING);
		addSimplePropertyName(RECURSE_ON_CONTIGS);
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
		eSearchRetMax = PluginUtils.configureIntProperty(ncbiImporterElem, E_SEARCH_RET_MAX, 4000);
		eFetchBatchSize = PluginUtils.configureIntProperty(ncbiImporterElem, E_FETCH_BATCH_SIZE, 200);
		sequenceFormat = Optional.ofNullable(PluginUtils.configureEnumProperty(SequenceFormat.class, ncbiImporterElem, SEQUENCE_FORMAT, false)).
				orElse(SequenceFormat.GENBANK_XML);
		sequenceIdField = Optional.ofNullable(PluginUtils.configureEnumProperty(SequenceIdField.class, 
				ncbiImporterElem, SEQUENCE_ID_FIELD, false)).orElse(SequenceIdField.GI_NUMBER);
		overwriteExisting = Optional.ofNullable(PluginUtils.configureBooleanProperty(ncbiImporterElem, OVERWRITE_EXISTING, false)).orElse(false);
		maxDownloaded = PluginUtils.configureIntProperty(ncbiImporterElem, MAX_DOWNLOADED, false);
		giNumberFieldName = PluginUtils.configureStringProperty(ncbiImporterElem, GI_NUMBER_FIELD_NAME, "GB_GI_NUMBER");
		recurseOnContigs = Optional.ofNullable(PluginUtils.configureBooleanProperty(ncbiImporterElem, RECURSE_ON_CONTIGS, false)).orElse(false);
		isAssemblyFieldName = PluginUtils.configureStringProperty(ncbiImporterElem, IS_ASSEMBLY_FIELD_NAME, "GB_IS_ASSEMBLY");
		
		
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
			eSearchTerm = primaryAccessionsToESearchTerm(specificPrimaryAccessions);
		}
	}

	private String primaryAccessionsToESearchTerm(List<String> primaryAccessions) {
		List<String> disjuncts = primaryAccessions.stream()
				.map(primaryAcc -> "\""+primaryAcc+"\"[Primary Accession]")
				.collect(Collectors.toList());
		return String.join(" OR ", disjuncts);
	}

	
	private String contigIDsToESearchTerm(Set<String> contigIDs) {
		return String.join(" OR ", contigIDs);
	}

	
	private void searchTermConfigError() {
		throw new NcbiImporterException(Code.CONFIG_ERROR, "Exactly one of <eSearchTerm>, <specificGiNumbers> or <specificPrimaryAccessions> must be specified.");
	}

	// Return the set of GI numbers for sequences which match the eSearchTerm, or the specific GiNumbers list if applicable.
	private Set<String> getGiNumbersMatching(CommandContext cmdContext, String eSearchTerm, List<String> specificGiNumbers) {
		Set<String> giNumbersMatching = new LinkedHashSet<String>();
		if(specificGiNumbers != null && !specificGiNumbers.isEmpty()) {
			giNumbersMatching.addAll(specificGiNumbers);
		} else {
			try(CloseableHttpClient httpClient = createHttpClient()) {
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
		return giNumbersMatching;
	}
	
	// Return the set of GI numbers for sequences that already exist in the source.
	private Set<String> getGiNumbersExisting(CommandContext cmdContext, boolean cachedGiNumbers, 
			Set<String> matchingGiNumbers, Set<String> contigIDs) {
		Set<String> giNumbersExisting = new LinkedHashSet<String>();
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), true);
		if(source == null) {
			return giNumbersExisting;
		}
		log("Finding sequences in source "+sourceName);
		List<Map<String, String>> pkMaps = 
				GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName))
				.getSequences()
				.stream().map(seq -> seq.pkMap())
				.collect(Collectors.toList());
		log("Found "+pkMaps.size()+" sequences.");
		int updates = 0;
		int foundInField = 0; 
		int foundInDocument = 0;
		int numChecked = 0;
		log("Checking for GI numbers in sequences in source \""+sourceName+"\"");
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
			Document gbDocument = null;
			if(giNumber == null) {
				gbDocument = ((GenbankXmlSequenceObject) sequence.getSequenceObject()).getDocument();
				giNumber = giNumberFromDocument(gbDocument);
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
				log("Existing sequences found: "+giNumbersExisting.size());
				log(foundInField+" GI numbers in field, "+foundInDocument+" in document");
				if(updates > 0) {
					cmdContext.commit();
					cmdContext.newObjectContext();
				}
				updates = 0;
			}
			// collect contig primary accessions from any existing sequence which matches the search query.
			if(recurseOnContigs && giNumber != null && matchingGiNumbers.contains(giNumber)) {
				if(gbDocument == null) {
					gbDocument = ((GenbankXmlSequenceObject) sequence.getSequenceObject()).getDocument();
				}
				List<String> contigIdsFromDocument = contigIdsFromDocument(gbDocument);
				if(contigIdsFromDocument != null) {
					contigIDs.addAll(contigIdsFromDocument);
				}
			}
		}
		log("Existing sequences found: "+giNumbersExisting.size());
		log("Found "+foundInField+" GI numbers in field, "+foundInDocument+" in document");
		if(updates > 0) {
			cmdContext.commit();
			cmdContext.newObjectContext();
		}
		return giNumbersExisting;
	}

	private CloseableHttpClient createHttpClient() {
		// ignore cookies, in order to prevent a log warning resulting from NCBI's incorrect
		// implementation of the spec.
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		return httpClient;
	}

	private List<RetrievedSequence> fetchBatch(List<String> giNumbers) {
		List<RetrievedSequence> retrievedSequences = new ArrayList<RetrievedSequence>();
		if(giNumbers.isEmpty()) {
			return retrievedSequences;
		}
		Object eFetchResponseObject;
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eFetchRequest = createEFetchRequest(giNumbers);
			switch(sequenceFormat) {
			case GENBANK_XML:
				log("Requesting "+giNumbers.size()+" sequences from NCBI via eFetch");
				eFetchResponseObject = runHttpRequestGetDocument("eFetch", eFetchRequest, httpClient);
				log("NCBI eFetch response received");
				break;
			default:
				throw new NcbiImporterException(NcbiImporterException.Code.CANNOT_PROCESS_SEQUENCE_FORMAT, 
						sequenceFormat.name());
			}
		} catch (IOException e) {
			throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eFetch", e.getLocalizedMessage());
		}
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
	
	// example 1:
	// join(gap(100000),gap(10000),gap(2890000),gap(100000),
	// NT_039490.8:1..4401876,gap(74868),NT_039492.8:1..50286063,
	// gap(343058),NT_039500.8:1..72389128,gap(100000))
	//
	// example 2:
	// join(AC170751.2:1..105368,AC155323.9:31991..161467,
	// AC158593.8:15083..216621, complement(AC166081.3:1..115516),
	// AC153367.5:115523..204838,complement(AC155910.6:1..87555))
	private List<String> contigIdsFromDocument(Document document) {
		String contigsString = GlueXmlUtils.getXPathString(document, "/GBSeq/GBSeq_contig/text()");
		if(contigsString == null) {
			return null;
		}
		contigsString = contigsString.trim();
		List<String> contigIDs = new ArrayList<String>();
		parseContigIDsFromString(contigsString, contigIDs);
		return contigIDs;
	}


	private void parseContigIDsFromString(String contigsString,
			List<String> contigIDs) {
		if(contigsString.startsWith("join(") && contigsString.endsWith(")")) {
			String[] specifiers = contigsString.substring("join(".length(), contigsString.length()-1).split(",");
			for(String contigSpecifier: specifiers) {
				parseContigIDsFromString(contigSpecifier, contigIDs);
			}
		} else if(contigsString.startsWith("gap(") && contigsString.endsWith(")")) {
			return;
		} else if(contigsString.startsWith("complement(") && contigsString.endsWith(")")) {
			contigsString.substring("complement(".length(), contigsString.length()-1);
		} else if(contigsString.matches("[A-Z0-9_\\.]+:\\d+\\.\\.\\d+")) {
			String version = contigsString.substring(0, contigsString.indexOf(":"));
			contigIDs.add(version);
		} else {
			throw new NcbiImporterException(NcbiImporterException.Code.FORMATTING_ERROR, "eFetch", 
					"GBSeq_contig contains unknown contig specifier \""+contigsString+"\"");
		}
		
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

	public NcbiImporterResult doPreview(CommandContext cmdContext) {
		
		boolean cachedGiNumbers = initCachedGiNumbers(cmdContext);
		Set<String> contigIds = new LinkedHashSet<String>();
		
		Set<String> matchingGiNumbers = getGiNumbersMatching(cmdContext, eSearchTerm, specificGiNumbers);
		int recordsFound = matchingGiNumbers.size();
		Set<String> existingGiNumbers = getGiNumbersExisting(cmdContext, cachedGiNumbers, matchingGiNumbers, contigIds);
		matchingGiNumbers.removeAll(existingGiNumbers);
		int preExistingSkipped = recordsFound - matchingGiNumbers.size();
		return new NcbiImporterResult(recordsFound, preExistingSkipped, 0, 0,
				maxDownloaded, overwriteExisting, eSearchRetMax);
	}


	
	private NcbiImporterResult doImport(CommandContext cmdContext) {
		boolean cachedGiNumbers = initCachedGiNumbers(cmdContext);
		boolean storeIsAssembly = initStoreIsAssembly(cmdContext);
		Set<String> matchingGiNumbers = getGiNumbersMatching(cmdContext, eSearchTerm, specificGiNumbers);
		Set<String> contigIDs = new LinkedHashSet<String>();
		Set<String> existingGiNumbers = getGiNumbersExisting(cmdContext, cachedGiNumbers, matchingGiNumbers, contigIDs);
		NcbiImporterResult firstRoundResult = doRetrievalRound(cmdContext, 
				cachedGiNumbers, storeIsAssembly, 
				matchingGiNumbers, existingGiNumbers, contigIDs);
		if(!recurseOnContigs) {
			return firstRoundResult;
		}
		int numGenbankRecordsAdded = firstRoundResult.getNumGenbankRecordsAdded();
		int numGenbankRecordsFound = firstRoundResult.getNumGenbankRecordsFound();
		int numGenbankRecordsUpdated = firstRoundResult.getNumGenbankRecordsUpdated();
		int numGenbankRecordsPreExisting = firstRoundResult.getNumGenbankRecordsPreExisting();
		Set<String> existingContigIDs = new LinkedHashSet<String>();
		while(!contigIDs.isEmpty()) {
			log("Retrieved sequences reference "+contigIDs.size()+" additional sequences as contigs. These will now be retrieved.");
			String eSearchTermForRound = contigIDsToESearchTerm(contigIDs);
			matchingGiNumbers = getGiNumbersMatching(cmdContext, eSearchTermForRound, null);
			if(existingGiNumbers.containsAll(matchingGiNumbers)) {
				// if the last eSearch produced only existing GI numbers then the contigIDs which were searched for
				// do not need to be retrieved again. This prevents a loop.
				existingContigIDs.addAll(contigIDs);
			}
			contigIDs.clear();
			existingGiNumbers.addAll(getGiNumbersExisting(cmdContext, cachedGiNumbers, matchingGiNumbers, contigIDs));
			NcbiImporterResult roundResult = doRetrievalRound(cmdContext, 
					cachedGiNumbers, storeIsAssembly, 
					matchingGiNumbers, existingGiNumbers, contigIDs);
			contigIDs.removeAll(existingContigIDs);
			numGenbankRecordsAdded += roundResult.getNumGenbankRecordsAdded();
			numGenbankRecordsUpdated += roundResult.getNumGenbankRecordsUpdated();
		}
		return new NcbiImporterResult(numGenbankRecordsFound, numGenbankRecordsPreExisting, numGenbankRecordsAdded, numGenbankRecordsUpdated,
				maxDownloaded, overwriteExisting, eSearchRetMax);
	}

	private NcbiImporterResult doRetrievalRound(CommandContext cmdContext,
			boolean cachedGiNumbers, boolean storeIsAssembly, 
			Set<String> matchingGiNumbers, Set<String> existingGiNumbers, 
			Set<String> contigIds) {
		
		Set<String> retrieveSet = new LinkedHashSet<String>(matchingGiNumbers);
		log("NCBI sequences matching search query: "+retrieveSet.size());
		int fullRetrieveSetSize = retrieveSet.size();
		if(!overwriteExisting) {
			retrieveSet.removeAll(existingGiNumbers);
		}
		int minimalRetrieveSetSize = retrieveSet.size();

		List<String> retrieveList = new ArrayList<String>(retrieveSet);
		if(maxDownloaded != null && retrieveList.size() > maxDownloaded) {
			retrieveList = retrieveList.subList(0, maxDownloaded);
		}
		log("NCBI sequences to download: "+retrieveList.size());
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
				Sequence newSequence = null;
				Document gbXmlDocument = null;
				createSequence(cmdContext, sourceName, sequenceID, format, sequenceData);
				if(cachedGiNumbers && sequence.format == SequenceFormat.GENBANK_XML) {
					newSequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
					gbXmlDocument = ((GenbankXmlSequenceObject) newSequence.getSequenceObject()).getDocument();
					newSequence.writeProperty(giNumberFieldName, giNumberFromDocument(gbXmlDocument));
					cmdContext.commit();
				}
				if(recurseOnContigs && sequence.format == SequenceFormat.GENBANK_XML) {
					if(newSequence == null) {
						newSequence = GlueDataObject.lookup(cmdContext, Sequence.class, Sequence.pkMap(sourceName, sequenceID), true);
					}
					if(gbXmlDocument == null) {
						gbXmlDocument = ((GenbankXmlSequenceObject) newSequence.getSequenceObject()).getDocument();
					}
					List<String> contigIDsInDocument = contigIdsFromDocument(gbXmlDocument);
					if(storeIsAssembly) {
						if(contigIDsInDocument == null) {
							newSequence.writeProperty(isAssemblyFieldName, false);
						} else {
							newSequence.writeProperty(isAssemblyFieldName, true);
						}
						cmdContext.commit();
					}
					if(contigIDsInDocument != null) {
						contigIds.addAll(contigIDsInDocument);
					}
				}
				
				if(preExisting) {
					recordsUpdated++;
				} else {
					recordsAdded++;
				}
			}
			cmdContext.newObjectContext();
			log("Sequences updated: "+recordsUpdated+", sequences added: "+recordsAdded);
			batchStart = batchEnd;
		} while(batchEnd < giNumbers.size());
		return new NcbiImporterResult(matchingGiNumbers.size(), fullRetrieveSetSize-minimalRetrieveSetSize, recordsAdded, recordsUpdated,
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

	private boolean initStoreIsAssembly(CommandContext cmdContext) {
		boolean storeIsAssembly = true;
		List<String> customSequenceFieldNames = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject().getCustomSequenceFieldNames();
		if(!customSequenceFieldNames.contains(isAssemblyFieldName)) {
			storeIsAssembly = false;
			GlueLogger.getGlueLogger().warning("No sequence field \""+isAssemblyFieldName+"\" exists in the project. Importer will not record whether sequences are assemblies.");
		}
		return storeIsAssembly;
	}

	
	
	class RetrievedSequence {
		public String sequenceID;
		String giNumber;
		SequenceFormat format;
		byte[] data;
	}
	
	public static class NcbiImporterResult extends MapResult {

		public NcbiImporterResult(int numGenbankRecordsFound, 
				int numGenbankRecordsPreExistingSkipped, 
				int numGenbankRecordsAdded, 
				int numGenbankRecordsUpdated,
				Integer maxDownloadedSetting,
				Boolean overwriteExistingSetting,
				Integer eSearchRetMaxSetting) {
			super("ncbiImporterResult", mapBuilder()
					.put("numGenbankRecordsFound", numGenbankRecordsFound)
					.put("numGenbankRecordsPreExistingSkipped", numGenbankRecordsPreExistingSkipped)
					.put("numGenbankRecordsAdded", numGenbankRecordsAdded)
					.put("numGenbankRecordsUpdated", numGenbankRecordsUpdated)
					.put("maxDownloadedSetting", maxDownloadedSetting)
					.put("overwriteExistingSetting", overwriteExistingSetting)
					.put("eSearchRetMaxSetting", eSearchRetMaxSetting));
		}
		
		public int getNumGenbankRecordsFound() {
			return (Integer) super.asMap().get("numGenbankRecordsFound");
		}

		public int getNumGenbankRecordsPreExisting() {
			return (Integer) super.asMap().get("numGenbankRecordsPreExistingSkipped");
		}

		public int getNumGenbankRecordsAdded() {
			return (Integer) super.asMap().get("numGenbankRecordsAdded");
		}

		public int getNumGenbankRecordsUpdated() {
			return (Integer) super.asMap().get("numGenbankRecordsUpdated");
		}

		
	}
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={""},
			metaTags={CmdMeta.updatesDatabase},
			description="Import sequence data from NCBI into the project") 
	public static class ImportCommand extends ModulePluginCommand<NcbiImporterResult, NcbiImporter> implements ProvidedProjectModeCommand {
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
	public static class PreviewCommand extends ModulePluginCommand<NcbiImporterResult, NcbiImporter> implements ProvidedProjectModeCommand {
		@Override
		protected NcbiImporterResult execute(CommandContext cmdContext, NcbiImporter importerPlugin) {
			return importerPlugin.doPreview(cmdContext);
		}

	}

}
