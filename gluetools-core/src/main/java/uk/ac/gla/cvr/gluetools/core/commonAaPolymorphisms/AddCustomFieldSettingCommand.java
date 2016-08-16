package uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms;

import java.util.Arrays;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleUpdateDocumentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"add", "custom-field-setting"}, 
		description = "Add a custom field setting for generated variations", 
		docoptUsages={"<fieldName> <fieldValue>"},
		docoptOptions={},
		metaTags = {CmdMeta.updatesDatabase}, 
		furtherHelp = "The setting will be applied to all variations which the module generates"
)
public class AddCustomFieldSettingCommand extends ModuleDocumentCommand<OkResult> implements ModuleUpdateDocumentCommand {

	private String fieldName;
	private String fieldValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		fieldValue = PluginUtils.configureStringProperty(configElem, "fieldValue", true);
	}

	@Override
	protected OkResult processDocument(CommandContext cmdContext, Module module, Document modulePluginDoc) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		insideProjectMode.getProject().checkCustomFieldNames(ConfigurableTable.variation.name(), Arrays.asList(fieldName));
		Element customFieldSettingElem = GlueXmlUtils.appendElement(modulePluginDoc.getDocumentElement(), "customFieldSetting");
		GlueXmlUtils.appendElementWithText(customFieldSettingElem, "fieldName", fieldName);
		GlueXmlUtils.appendElementWithText(customFieldSettingElem, "fieldValue", fieldValue);
		return new OkResult();
	}
	
	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("fieldName", new CustomFieldNameInstantiator(ConfigurableTable.variation.name()));
		}
	}

}