package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "var-almt-note"}, 
	docoptUsages={"<alignmentName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a variation-alignment note") 
public class VariationDeleteAlmtNoteCommand extends VariationModeCommand<DeleteResult> {

	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		DeleteResult result = GlueDataObject.delete(cmdContext, VarAlmtNote.class, VarAlmtNote.pkMap(
				alignmentName, getRefSeqName(), getFeatureName(), getVariationName()), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("alignmentName", new QualifiedDataObjectNameInstantiator(
					VarAlmtNote.class, VarAlmtNote.ALIGNMENT_NAME_PATH) {
						@SuppressWarnings("rawtypes")
						@Override
						protected void qualifyResults(CommandMode cmdMode,
								Map<String, Object> bindings,
								Map<String, Object> qualifierValues) {
							VariationMode varMode = (VariationMode) cmdMode;
							qualifierValues.put(VarAlmtNote.REF_SEQ_NAME_PATH, varMode.getRefSeqName());
							qualifierValues.put(VarAlmtNote.FEATURE_NAME_PATH, varMode.getFeatureName());
							qualifierValues.put(VarAlmtNote.VARIATION_NAME_PATH, varMode.getVariationName());
						}
				
			});
		}
	}

}
