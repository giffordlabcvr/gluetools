package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;




@CommandClass(
		commandWords={"generate", "common", "variations"}, 
		description = "Generate common variations from members of a single constrained alignment", 
		docoptUsages = { "<alignmentName> [-r] [-w <whereClause>] <referenceName> <featureName> [-c <vcatName> ...]" }, 
		docoptOptions = {
				"-r, --recursive                                Include members of descendent alignments",
				"-w <whereClause>, --whereClause <whereClause>  Qualify included members",
				"-c <vcatName>, --categories <vcatName>         Add variation category"},
		furtherHelp = "The alignment named by <alignmentName> must be constrained to a reference. "+
		"The reference sequence named <referenceName> may be the alignment's constraining reference, or "+
		"the constraining reference of any ancestor alignment. The named reference sequence must have a "+
		"location defined for the feature named by <featureName>, this must be a non-informational feature. "+
		"If --recursive is used, the set of alignment members will be expanded to include members of the named "+
		"alignment's child alignments, and those of their child aligments etc. This can therefore be used to generate "+
		"variations from across a whole evolutionary clade. "+
		"If a <whereClause> is supplied, this can qualify further the set of "+
		"included alignment members. Example: \n"+
		"  generate common variations AL_3 -w \"sequence.source.name = 'ncbi-curated'\" MREF NS3\n"+
		"One or more named variation categories may be specified, using the --vcatName option. If so, the generated "+
		"variations are added to the named categories",
		metaTags = {CmdMeta.updatesDatabase}	
)
public class GenerateCommonVariationsCommand extends VariationsCommand<GenerateVariationsResult> {

	private List<String> vcatNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatNames = PluginUtils.configureStringsProperty(configElem, "categories");
	}

	@Override
	protected GenerateVariationsResult execute(CommandContext cmdContext, MutationFrequenciesReporter modulePlugin) {
		PreviewVariationsResult previewVariationsResult = modulePlugin.previewVariations(cmdContext, 
				getAlignmentName(), isRecursive(), getWhereClause(), getReferenceName(), getFeatureName());
		return GenerateVariationCommand.generateVariationsFromPreview(cmdContext, modulePlugin, previewVariationsResult, vcatNames, getReferenceName(), getFeatureName());
	}


	@CompleterClass
	public static class Completer extends AlignmentAnalysisCommandCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("vcatName", new VcatNameCompleter());
		}
	}

}
