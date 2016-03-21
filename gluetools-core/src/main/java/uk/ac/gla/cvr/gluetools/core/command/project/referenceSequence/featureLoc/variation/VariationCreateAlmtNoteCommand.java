package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","var-almt-note"}, 
	docoptUsages={"<alignmentName>"},
	description="Create a new varation-alignment note", 
	metaTags={CmdMeta.updatesDatabase}
	) 
public class VariationCreateAlmtNoteCommand extends VariationModeCommand<CreateResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	
	private String alignmentName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Variation variation = lookupVariation(cmdContext);
		createAlmtNote(cmdContext, variation, alignment);
		cmdContext.commit();
		return new CreateResult(VarAlmtNote.class, 1);
	}

	public static VarAlmtNote createAlmtNote(CommandContext cmdContext, Variation variation, Alignment alignment) {
		VarAlmtNote varAlmtNote = GlueDataObject.create(cmdContext, VarAlmtNote.class, 
				VarAlmtNote.pkMap(alignment.getName(), variation.getFeatureLoc().getReferenceSequence().getName(), 
						variation.getFeatureLoc().getFeature().getName(), variation.getName()), false);
		varAlmtNote.setAlignment(alignment);
		varAlmtNote.setVariation(variation);
		return varAlmtNote;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
		}
	}
}
