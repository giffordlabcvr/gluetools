package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

import uk.ac.gla.cvr.gluetools.core.collation.importing.SequenceImporter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

// TODO importer plugin should only fetch sequence the source does not already have.
@PluginClass(elemName="ncbiImporter")
public class NcbiImporter extends SequenceImporter<NcbiImporter> {

	
	private String sourceName;
	private String eUtilsBaseURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
	private String database;
	private String eSearchTerm = null;
	private int eSearchRetMax;
	private int eFetchBatchSize;
	private List<String> specificSequenceIDs;
	private SequenceFormat sequenceFormat;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element sequenceSourcerElem) {
		database = PluginUtils.configureStringProperty(sequenceSourcerElem, "database", "nuccore");
		sourceName = Optional.ofNullable(PluginUtils.
				configureStringProperty(sequenceSourcerElem, "sourceName", false)).orElse("ncbi-"+database);
		eSearchTerm = PluginUtils.configureStringProperty(sequenceSourcerElem, "eSearchTerm", false);
		if(eSearchTerm == null) {
			specificSequenceIDs = PluginUtils.configureStrings(sequenceSourcerElem, "specificSequenceIDs/sequenceID/text()", true);
		}
		eSearchRetMax = PluginUtils.configureIntProperty(sequenceSourcerElem, "eSearchRetMax", 4000);
		eFetchBatchSize = PluginUtils.configureIntProperty(sequenceSourcerElem, "eFetchBatchSize", 200);
		sequenceFormat = PluginUtils.configureEnumProperty(SequenceFormat.class, sequenceSourcerElem, "sequenceFormat", true);
		addProvidedCmdClass(ImportCommand.class);
		addProvidedCmdClass(ShowImporterCommand.class);
		addProvidedCmdClass(ConfigureImporterCommand.class);
	}

	List<String> getSequenceIDs() {
		if(specificSequenceIDs != null) {
			return specificSequenceIDs;
		}
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eSearchHttpRequest = createESearchRequest();
			Document eSearchResponseDoc = runHttpRequestGetDocument("eSearch", eSearchHttpRequest, httpClient);
			checkForESearchErrors(eSearchResponseDoc);
			return GlueXmlUtils.getXPathStrings(eSearchResponseDoc, "/eSearchResult/IdList/Id/text()");
		} catch (IOException e) {
			throw new NcbiImporterException(e, NcbiImporterException.Code.IO_ERROR, "eSearch", e.getLocalizedMessage());
		}
	}

	private CloseableHttpClient createHttpClient() {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		return httpClient;
	}

	String getSourceUniqueID() {
		return "ncbiImporter:"+database+":"+sequenceFormat.name();
	}

	List<RetrievedSequence> retrieveSequences(List<String> sequenceIDs) {
		List<RetrievedSequence> retrievedSequences = new ArrayList<RetrievedSequence>();
		int batchStart = 0;
		int batchEnd;
		do {
			batchEnd = Math.min(batchStart+eFetchBatchSize, sequenceIDs.size());
			retrievedSequences.addAll(fetchBatch(sequenceIDs.subList(batchStart, batchEnd)));
			batchStart = batchEnd;
		} while(batchEnd < sequenceIDs.size());
		return retrievedSequences;
	}

	private List<RetrievedSequence> fetchBatch(List<String> sequenceIDs) {
		Object eFetchResponseObject;
		try(CloseableHttpClient httpClient = createHttpClient()) {
			HttpUriRequest eFetchRequest = createEFetchRequest(sequenceIDs);
			switch(sequenceFormat) {
			case GENBANK_XML:
				eFetchResponseObject = runHttpRequestGetDocument("eFetch", eFetchRequest, httpClient);
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
		
		int i = 0;
		for(String sequenceID: sequenceIDs) {
			if(i >= individualGBFiles.size()) {
				throw new NcbiImporterException(NcbiImporterException.Code.INSUFFICIENT_SEQUENCES_RETURNED);
			}
			RetrievedSequence retrievedSequence = new RetrievedSequence();
			retrievedSequence.format = sequenceFormat;
			retrievedSequence.sequenceID = sequenceID;
			Object individualFile = individualGBFiles.get(i);
			if(individualFile instanceof Document) {
				retrievedSequence.data = GlueXmlUtils.prettyPrint((Document) individualFile);
			}
			retrievedSequences.add(retrievedSequence);
			i++;
		}
		return retrievedSequences;
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

	private HttpUriRequest createEFetchRequest(List<String> idsToFetch)  {
		String commaSeparatedIDs = String.join(",", idsToFetch.toArray(new String[]{}));


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

	private CreateResult doImport(CommandContext cmdContext) {
		List<String> sequenceIDs = getSequenceIDs();
		List<RetrievedSequence> sequences = retrieveSequences(sequenceIDs);
		ensureSourceExists(cmdContext, sourceName);
		int sequencesCreated = 0;
		for(RetrievedSequence sequence: sequences) {
			String sequenceID = sequence.sequenceID;
			SequenceFormat format = sequence.format;
			byte[] sequenceData = sequence.data;
			createSequence(cmdContext, sourceName, sequenceID, format, sequenceData);
			sequencesCreated++;
		}
		return new CreateResult(Sequence.class, sequencesCreated);
	}
	
	
	class RetrievedSequence {
		String sequenceID;
		SequenceFormat format;
		byte[] data;
	}
	
	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={""},
			metaTags={CmdMeta.updatesDatabase},
			description="Import sequence data from NCBI into the project") 
	public static class ImportCommand extends ModuleProvidedCommand<CreateResult, NcbiImporter> implements ProvidedProjectModeCommand {

		@Override
		protected CreateResult execute(CommandContext cmdContext, NcbiImporter importerPlugin) {
			return importerPlugin.doImport(cmdContext);
		}
		
	}

	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this importer") 
	public static class ShowImporterCommand extends ShowConfigCommand<NcbiImporter> {}

	@SimpleConfigureCommandClass(
			propertyNames={"sourceName", "database", "eSearchTerm", 
					"sequenceFormat", "eSearchRetMax", "eFetchBatchSize"}
	)
	public static class ConfigureImporterCommand extends SimpleConfigureCommand<NcbiImporter> {}


}
