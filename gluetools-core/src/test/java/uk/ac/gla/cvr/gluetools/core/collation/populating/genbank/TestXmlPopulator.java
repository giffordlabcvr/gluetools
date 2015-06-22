package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.datafield.BooleanField;
import uk.ac.gla.cvr.gluetools.core.datafield.DataField;
import uk.ac.gla.cvr.gluetools.core.datafield.DateField;
import uk.ac.gla.cvr.gluetools.core.datafield.IntegerField;
import uk.ac.gla.cvr.gluetools.core.datafield.StringField;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import freemarker.template.Configuration;


public class TestXmlPopulator {
	
	
	String 
		GB_GI_NUMBER = "GB_GI_NUMBER",
		GB_PRIMARY_ACCESSION = "GB_PRIMARY_ACCESSION",
		GB_ACCESSION_VERSION = "GB_ACCESSION_VERSION",
		GB_LOCUS = "GB_LOCUS",
		GB_LENGTH = "GB_LENGTH",
		GB_GENOTYPE = "GB_GENOTYPE",
		GB_SUBTYPE = "GB_SUBTYPE",
		GB_RECOMBINANT = "GB_RECOMBINANT",
		GB_PATENT_RELATED = "GB_PATENT_RELATED",
		GB_ORGANISM = "GB_ORGANISM",
		GB_ISOLATE = "GB_ISOLATE",
		GB_TAXONOMY = "GB_TAXONOMY",
		GB_HOST = "GB_HOST", 
		GB_COUNTRY = "GB_COUNTRY",
		GB_COLLECTION_YEAR = "GB_COLLECTION_YEAR", 
		GB_COLLECTION_MONTH = "GB_COLLECTION_MONTH",
		GB_COLLECTION_MONTH_DAY = "GB_COLLECTION_MONTH_DAY",
		GB_CREATE_DATE = "GB_CREATE_DATE",
		GB_UPDATE_DATE = "GB_UPDATE_DATE";
	
	

	//@Test 
	public void testHcvRuleSet() throws Exception {
		String xmlDirectory = "/Users/joshsinger/hcv_rega/retrieved_xml";
		String populatorRulesFile = "hcvRuleSet.xml";
		
		
		List<DataField<?>> fields = Arrays.asList(new DataField<?>[]{
				new StringField(GB_GI_NUMBER),
				new StringField(GB_PRIMARY_ACCESSION),
				new StringField(GB_ACCESSION_VERSION),
				new StringField(GB_LOCUS),
				new IntegerField(GB_LENGTH),
				new StringField(GB_GENOTYPE), 
				new StringField(GB_SUBTYPE),
				new BooleanField(GB_RECOMBINANT),
				new BooleanField(GB_PATENT_RELATED),
				new StringField(GB_ORGANISM),
				new StringField(GB_ISOLATE),
				new StringField(GB_TAXONOMY),
				new StringField(GB_HOST),
				new StringField(GB_COUNTRY),
				new IntegerField(GB_COLLECTION_YEAR),
				new StringField(GB_COLLECTION_MONTH),
				new IntegerField(GB_COLLECTION_MONTH_DAY),
				new DateField(GB_CREATE_DATE),
				new DateField(GB_UPDATE_DATE),
				
		});
		Project project = initProjectFromFields(fields);
		List<CollatedSequence> collatedSequences = initSequencesXml(project, xmlDirectory);
		runPopulator(collatedSequences, populatorRulesFile);
		@SuppressWarnings("unused")
		Predicate<? super CollatedSequence> problematicPredicate = problematicPredicate();
		
		//collatedSequences = collatedSequences.stream().filter(problematicPredicate).collect(Collectors.toList());
		//List<String> displayFieldNames = fields.stream().map(s -> s.getName()).collect(Collectors.toList());
		List<String> displayFieldNames = Arrays.asList(new String[]{
//				GB_GI_NUMBER,
//				GB_PRIMARY_ACCESSION, 
//				GB_ACCESSION_VERSION,
//				GB_LOCUS,
//				GB_LENGTH,
				GB_GENOTYPE,
				GB_SUBTYPE,
//				GB_RECOMBINANT, 
//				GB_PATENT_RELATED,
//				GB_ORGANISM,
//				GB_ISOLATE,
//				GB_TAXONOMY,
//				GB_HOST, 
//				GB_COUNTRY, 
//				GB_COLLECTION_YEAR,
//				GB_COLLECTION_MONTH,
//				GB_COLLECTION_MONTH_DAY,
//				GB_CREATE_DATE,
//				GB_UPDATE_DATE,
		});
		dumpFieldValues(displayFieldNames, collatedSequences);
	}

	// return true if the sequence fields are problematic.
	private Predicate<? super CollatedSequence> problematicPredicate() {
		return seq -> {
			if(!seq.getString(GB_ORGANISM).equals(Optional.of("Hepatitis C virus"))) {
				return false;
			}
			if(seq.getBoolean(GB_PATENT_RELATED).equals(Optional.of(Boolean.TRUE))) {
				if(seq.hasFieldValue(GB_GENOTYPE) || seq.hasFieldValue(GB_SUBTYPE)) {
					return true;
				} else {
					return false;
				}
			}
			if(seq.getBoolean(GB_RECOMBINANT).equals(Optional.of(Boolean.TRUE))) {
				return false;
			}
			if(seq.hasFieldValue(GB_GENOTYPE) && seq.hasFieldValue(GB_SUBTYPE)) {
				return false;
			}			
			return true;

		};
	}
	
	private void dumpFieldValues(List<String> fieldNames,
			List<CollatedSequence> collatedSequences) {
		collatedSequences.forEach(sequence -> {
			System.out.print(sequence.getSequenceSourceID()+" -- ");
			fieldNames.forEach(fieldName -> {
				sequence.getFieldValue(fieldName).ifPresent(f -> { System.out.print(fieldName+": "+f+", "); });
			});
			System.out.println();
		});
		System.out.println("------------------\nTotal: "+collatedSequences.size()+" sequences");
		
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
		PluginConfigContext pluginConfigContext = new PluginConfigContext(new Configuration());
		GenbankXmlPopulatorPlugin dataFieldPopulator = (GenbankXmlPopulatorPlugin) PluginFactory.get(ModulePluginFactory.creator).
				createFromElement(pluginConfigContext, document.getDocumentElement());
		collatedSequences.forEach(sequence -> {
			dataFieldPopulator.populate(sequence);
		});
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
			collatedSequence.setFormat(SequenceFormat.GENBANK_XML);
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
