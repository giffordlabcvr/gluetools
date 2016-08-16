package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.digs.importer.ImportExtractedFieldRule.GlueFieldRequirement;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;


@CommandClass(
		commandWords={"update", "mapping", "extracted"}, 
		description = "Update mapping between a DIGS \"Extracted\" and a GLUE sequence field", 
		docoptUsages = { "<digsExtractedField> ( -i | ( -w | -r ) <glueSequenceField> )" },
		docoptOptions = {
				"-i, --ignore   Ignore the DIGS Extracted field",
				"-w, --warn     Emit warning if field <glueSequenceField> does not exist",
				"-r, --require  Field <glueSequenceField> must exist, emit error otherwise"}
)
public class UpdateMappingExtractedCommand extends ModuleDocumentCommand<UpdateResult> implements ModuleUpdateDocumentCommand {

	private String digsExtractedField;
	private boolean ignore;
	private boolean warn;
	private boolean require;
	private String glueSequenceField;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		digsExtractedField = PluginUtils.configureStringProperty(configElem, "digsExtractedField", true);
		ignore = PluginUtils.configureBooleanProperty(configElem, "ignore", true);
		warn = PluginUtils.configureBooleanProperty(configElem, "warn", true);
		require = PluginUtils.configureBooleanProperty(configElem, "require", true);
		glueSequenceField = PluginUtils.configureStringProperty(configElem, "glueSequenceField", false);
		if(! ( 
				(ignore && !warn && !require) ||
				(!ignore && warn && !require) ||
				(!ignore && !warn && require) ) ) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Exactly one of --ignore, --warn and --require should be specified.");
		}
		if(warn || require && glueSequenceField == null) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "If --warn or --require is specified, <glueSequenceField> must be specified");
		}
		List<String> extractedFields = Arrays.asList(Extracted.ALL_PROPERTIES);
		if(!extractedFields.contains(digsExtractedField)) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Invalid DIGS Extracted field. Valid fields: "+extractedFields);
		}
	}

	@Override
	protected UpdateResult processDocument(CommandContext cmdContext,
			Module module, Document configDoc) {
		Node existing = GlueXmlUtils.getXPathNode(configDoc, "/"+DigsImporter.ELEM_NAME+"/"+
				ImportExtractedFieldRule.EXTRACTED_FIELD_RULE+
				"["+ImportExtractedFieldRule.EXTRACTED_FIELD+"/text() = '"+digsExtractedField+"']");
		if(existing != null) {
			existing.getParentNode().removeChild(existing);
		}
		Element newElem = GlueXmlUtils.appendElement(configDoc.getDocumentElement(), ImportExtractedFieldRule.EXTRACTED_FIELD_RULE);
		GlueXmlUtils.appendElementWithText(newElem, ImportExtractedFieldRule.EXTRACTED_FIELD, digsExtractedField);
		GlueFieldRequirement glueFieldRequirement = null;
		if(ignore) {
			glueFieldRequirement = GlueFieldRequirement.IGNORE;
		} else if(warn) {
			glueFieldRequirement = GlueFieldRequirement.WARN;
		} else {
			glueFieldRequirement = GlueFieldRequirement.REQUIRE;
		}
		GlueXmlUtils.appendElementWithText(newElem, ImportExtractedFieldRule.GLUE_FIELD_REQUIREMENT, glueFieldRequirement.name());
		if(glueSequenceField != null) {
			GlueXmlUtils.appendElementWithText(newElem, ImportExtractedFieldRule.SEQUENCE_FIELD, glueSequenceField);
		}
		return new UpdateResult(Module.class, 1);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			List<String> extractedFields = Arrays.asList(Extracted.ALL_PROPERTIES);
			registerStringListLookup("digsExtractedField", extractedFields);
			registerVariableInstantiator("glueSequenceField", new CustomFieldNameInstantiator(ConfigurableTable.sequence.name()));
		}
		
	}

	

}
