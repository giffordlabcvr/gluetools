package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequenceFormat;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile.GenbankFlatFileUtils;
import uk.ac.gla.cvr.gluetools.core.datafield.BooleanField;
import uk.ac.gla.cvr.gluetools.core.datafield.DataField;
import uk.ac.gla.cvr.gluetools.core.datafield.StringField;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulator;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


public class TestXmlPopulator {
	
	@Test 
	public void testXmlPopulator1() throws Exception {
		String genbankFile = "testXmlPopulator1.gb";
		String populatorRulesFile = "testXmlPopulator1.xml";
		List<String> fieldNames = Arrays.asList("GB_GENOTYPE");
		Project project = initProject(fieldNames);
		List<CollatedSequence> collatedSequences = initSequences(project, genbankFile);
		runPopulator(collatedSequences, populatorRulesFile);
		dumpFieldValues(fieldNames, collatedSequences);
	}

	@Test 
	public void testXmlPopulator2() throws Exception {
		String genbankFile = "testXmlPopulator2.gb";
		String populatorRulesFile = "testXmlPopulator2.xml";
		List<String> fieldNames = Arrays.asList("GB_GENOTYPE", "GB_PRIMARY_ACCESSION");
		Project project = initProject(fieldNames);
		List<CollatedSequence> collatedSequences = initSequences(project, genbankFile);
		runPopulator(collatedSequences, populatorRulesFile);
		dumpFieldValues(fieldNames, collatedSequences);
	}

	@Test 
	public void testHcvRuleSet() throws Exception {
		String xmlDirectory = "/Users/joshsinger/hcv_rega/retrieved_xml";
		String populatorRulesFile = "hcvRuleSet.xml";
		String 
			GB_PRIMARY_ACCESSION = "GB_PRIMARY_ACCESSION",
			GB_GENOTYPE = "GB_GENOTYPE",
			GB_SUBTYPE = "GB_SUBTYPE",
			GB_RECOMBINANT = "GB_RECOMBINANT",
			GB_PATENT_RELATED = "GB_PATENT_RELATED";
		
		List<DataField<?>> fields = Arrays.asList(
				new StringField(GB_PRIMARY_ACCESSION),
				new StringField(GB_GENOTYPE), 
				new StringField(GB_SUBTYPE), 
				new BooleanField(GB_RECOMBINANT),
				new BooleanField(GB_PATENT_RELATED)
				
		);
		Project project = initProjectFromFields(fields);
		List<CollatedSequence> collatedSequences = initSequencesXml(project, xmlDirectory);
		runPopulator(collatedSequences, populatorRulesFile);
		//List<String> displayFieldNames = fields.stream().map(s -> s.getName()).collect(Collectors.toList());
		List<String> displayFieldNames = Arrays.asList(
				GB_GENOTYPE,
				GB_SUBTYPE,
				GB_RECOMBINANT
		);
		dumpFieldValues(displayFieldNames, collatedSequences);
	}
	
	private void dumpFieldValues(List<String> fieldNames,
			List<CollatedSequence> collatedSequences) {
		collatedSequences.forEach(sequence -> {
			System.out.print(sequence.getSequenceSourceID()+" -- ");
			fieldNames.forEach(fieldName -> {
				sequence.getDataFieldValue(fieldName).ifPresent(f -> { System.out.print(fieldName+": "+f.getValue()+", "); });
			});
			System.out.println();
		});
	}

	private Project initProject(List<String> fieldNames) {
		Project project = new Project();
		fieldNames.forEach(f -> {project.addDataField(new StringField(f));});
		return project;
	}

	private Project initProjectFromFields(List<DataField<?>> fields) {
		Project project = new Project();
		fields.forEach(f -> {project.addDataField(f);});
		return project;
	}

	
	private void runPopulator(List<CollatedSequence> collatedSequences, String populatorRulesFile)
			throws SAXException, IOException {
		Document document;
		try(InputStream docStream = getClass().getResourceAsStream(populatorRulesFile)) {
			document = XmlUtils.documentFromStream(docStream);
		}
		DataFieldPopulator dataFieldPopulator = PluginFactory.get(DataFieldPopulatorFactory.creator).createFromElement(document.getDocumentElement());
		collatedSequences.forEach(sequence -> {
			dataFieldPopulator.populate(sequence);
		});
	}

	public List<CollatedSequence> initSequences(Project project, String genbankFile)
			throws IOException {
		String fileContent;
		try(InputStream inputStream = getClass().getResourceAsStream(genbankFile)) {	
			fileContent = IOUtils.toString(inputStream);
		}
		List<Object> gbStrings = GenbankFlatFileUtils.divideConcatenatedGBFiles(fileContent);
		List<CollatedSequence> collatedSequences = gbStrings.stream().map(string -> {
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setOwningProject(project);
			collatedSequence.setFormat(CollatedSequenceFormat.GENBANK_FLAT_FILE);
			collatedSequence.setSequenceText((String) string);
			return collatedSequence;
		}).collect(Collectors.toList());
		return collatedSequences;
	}
	
	public List<CollatedSequence> initSequencesXml(Project project, String directoryPath) {
		File directory = new File(directoryPath);
		File[] files = directory.listFiles();
		return Arrays.asList(files).stream().map(file -> {
			Document document;
			try(FileInputStream fileInputStream = new FileInputStream(file)) {
				document = XmlUtils.documentFromStream(fileInputStream);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setOwningProject(project);
			collatedSequence.setSequenceSourceID(file.getName().replace(".xml", ""));
			collatedSequence.setFormat(CollatedSequenceFormat.GENBANK_XML);
			collatedSequence.setSequenceDocument(document);
			return collatedSequence;
		}).collect(Collectors.toList());
	}
	
	@Test
	public void textXPath() throws Exception {
		Document document = XmlUtils.documentFromStream(getClass().getResourceAsStream("testXmlPopulator1.xml"));
		System.out.println(XmlUtils.getXPathStrings(document, "/dataFieldPopulator/*[self::rules|self::foo]/xPathNodes/xPathExpression/text()"));
	}
	
	
}
