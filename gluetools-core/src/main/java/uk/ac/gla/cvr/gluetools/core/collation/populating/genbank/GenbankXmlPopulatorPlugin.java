package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequenceFormat;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.datafield.BooleanField;
import uk.ac.gla.cvr.gluetools.core.datafield.DataField;
import uk.ac.gla.cvr.gluetools.core.datafield.DateField;
import uk.ac.gla.cvr.gluetools.core.datafield.IntegerField;
import uk.ac.gla.cvr.gluetools.core.datafield.StringField;
import uk.ac.gla.cvr.gluetools.core.datamodel.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

// TODO remove legacy populator stuff.
@PluginClass(elemName="genbankXmlPopulator")
public class GenbankXmlPopulatorPlugin implements ModulePlugin {

	private List<XmlPopulatorRule> rules;
	
	protected List<XmlPopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		XmlPopulatorRuleFactory populatorRuleFactory = PluginFactory.get(GenbankXmlPopulatorRuleFactory.creator);
		String alternateElemsXPath = XmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		rules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
	}

	void populate(CollatedSequence collatedSequence) {
		rules.forEach(rule -> {
			rule.execute(collatedSequence, collatedSequence.asXml());
		});
	}

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
	
	private Project initProject() {
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
		Project project = new Project();
		fields.forEach(f -> {project.addDataField(f);});
		return project;
	}
	
	
	@Override
	public CommandResult runModule(CommandContext cmdContext) {
		Project project = initProject();
		String sourceName = "ncbi-nuccore";
		Document doc = XmlUtils.newDocument();
		String commandElemName = CommandUsage.commandForCmdClass(ListSequencesCommand.class);
		Element listSequencesElem = (Element) doc.appendChild(doc.createElement(commandElemName));
		Element sourceNameElem = (Element) listSequencesElem.appendChild(doc.createElement("sourceName"));
		sourceNameElem.appendChild(doc.createTextNode(sourceName));
		Command command = cmdContext.commandFromElement(listSequencesElem);
		@SuppressWarnings("unchecked")
		ListCommandResult<Sequence> listResult = (ListCommandResult<Sequence>) command.execute(cmdContext);
		
		List<CollatedSequence> collatedSequences = new ArrayList<CollatedSequence>();
		
		List<String> displayFieldNames = Arrays.asList(new String[]{
//				GB_GI_NUMBER,
//				GB_PRIMARY_ACCESSION, 
//				GB_ACCESSION_VERSION,
//				GB_LOCUS,
//				GB_LENGTH,
				GB_GENOTYPE,
				GB_SUBTYPE,
				GB_RECOMBINANT, 
				GB_PATENT_RELATED,
//				GB_ORGANISM,
//				GB_ISOLATE,
//				GB_TAXONOMY,
//				GB_HOST, 
				GB_COUNTRY, 
//				GB_COLLECTION_YEAR,
//				GB_COLLECTION_MONTH,
//				GB_COLLECTION_MONTH_DAY,
//				GB_CREATE_DATE,
//				GB_UPDATE_DATE,
		});

		for(Sequence sequence: listResult.getResults()) {
			Document sequenceDoc = null;
			try {
				sequenceDoc = XmlUtils.documentFromBytes(sequence.getData());
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
			CollatedSequence collatedSequence = new CollatedSequence();
			collatedSequence.setOwningProject(project);
			collatedSequence.setSequenceSourceID(sequence.getSequenceID());
			collatedSequence.setFormat(CollatedSequenceFormat.GENBANK_XML);
			collatedSequence.setSequenceDocument(sequenceDoc);
			populate(collatedSequence);
			collatedSequences.add(collatedSequence);
		}
		StringBuffer buffer = new StringBuffer();
		dumpFieldValues(displayFieldNames, collatedSequences, buffer);

		
		return new ConsoleCommandResult() {
			@Override
			public String getResultAsConsoleText() {
				return buffer.toString();
			}
		};
	}
	
	private void dumpFieldValues(List<String> fieldNames,
			List<CollatedSequence> collatedSequences, StringBuffer buffer) {
		collatedSequences.forEach(sequence -> {
			buffer.append("SequenceID["+sequence.getSequenceSourceID()+"] -- ");
			fieldNames.forEach(fieldName -> {
				sequence.getFieldValue(fieldName).ifPresent(f -> { buffer.append(fieldName+": "+f+", "); });
			});
			buffer.append("\n");
		});
		buffer.append("------------------\nTotal: "+collatedSequences.size()+" sequences\n");
		
	}

}
