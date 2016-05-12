package uk.ac.gla.cvr.gluetools.core.variationFrequencies;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentVariationFrequencyCmdDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;
import uk.ac.gla.cvr.gluetools.core.variationFrequencies.GenerateVariationFrequenciesCommand.GenerateVariationFrequenciesResult;

@CommandClass(
		commandWords={"generate", "frequencies"}, 
		description = "Generate variation frequencies as variation-alignment notes", 
		docoptUsages = { "<almtName> [-q] [-c] [-w <whereClause>] -r <acRefName> [-m] -f <featureName> [-d] [-v <vWhereClause>]" },
		docoptOptions = { 
		"-q, --alignmentRecursive                          Include descendent alignments",
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-v <vWhereClause>, --vWhereClause <vWhereClause>  Qualify variations",
		"-r <acRefName>, --acRefName <acRefName>           Ancestor-constraining ref",
		"-m, --multiReference                              Scan across references",
		"-f <featureName>, --featureName <featureName>     Feature containing variations",
		"-d, --descendentFeatures                          Include descendent features",
		},
		furtherHelp = 
		"The <acRefName> argument names a reference sequence constraining an ancestor alignment of the named alignment. "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between the named alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. ",
		metaTags = {}	
)
public class GenerateVariationFrequenciesCommand extends ModulePluginCommand<GenerateVariationFrequenciesResult, VariationFrequenciesGenerator> {

	public static String ALIGNMENT_NAME = "almtName";
	public static String ALIGNMENT_RECURSIVE = "alignmentRecursive";
	
	public static final String RECURSIVE = AlignmentVariationFrequencyCmdDelegate.RECURSIVE;
	public static final String WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.WHERE_CLAUSE;
	public static final String VARIATION_WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.VARIATION_WHERE_CLAUSE;
	public static final String AC_REF_NAME = AlignmentVariationFrequencyCmdDelegate.AC_REF_NAME;
	public static final String MULTI_REFERENCE = AlignmentVariationFrequencyCmdDelegate.MULTI_REFERENCE;
	public static final String FEATURE_NAME = AlignmentVariationFrequencyCmdDelegate.FEATURE_NAME;
	public static final String DESCENDENT_FEATURES = AlignmentVariationFrequencyCmdDelegate.DESCENDENT_FEATURES;
	
	private String almtName; 
	private boolean alignmentRecursive; 
	
	private AlignmentVariationFrequencyCmdDelegate delegate = new AlignmentVariationFrequencyCmdDelegate();
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.alignmentRecursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALIGNMENT_RECURSIVE, false)).orElse(false);
		this.delegate.configure(pluginConfigContext, configElem);
	}

	@Override
	protected GenerateVariationFrequenciesResult execute(CommandContext cmdContext,
			VariationFrequenciesGenerator variationFrequenciesGenerator) {
		List<Alignment> alignments = new LinkedList<Alignment>();
		Alignment namedAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
		alignments.add(namedAlignment);
		if(alignmentRecursive) {
			alignments.addAll(namedAlignment.getDescendents());
		}
		List<VariationFrequenciesGenerator.AlignmentVariationReport> almtVarReports = new ArrayList<VariationFrequenciesGenerator.AlignmentVariationReport>();
		for(Alignment alignment: alignments) {
			List<VariationScanMemberCount> scanCounts = delegate.execute(alignment, cmdContext);
			variationFrequenciesGenerator.generateAlmtVarNotes(cmdContext, alignment, scanCounts, almtVarReports);
		}
		variationFrequenciesGenerator.log("Generated "+almtVarReports.size()+" alignment-variation frequencies");

		return new GenerateVariationFrequenciesResult(almtVarReports);
	}

	public static class GenerateVariationFrequenciesResult extends BaseTableResult<VariationFrequenciesGenerator.AlignmentVariationReport> {
		public GenerateVariationFrequenciesResult(List<VariationFrequenciesGenerator.AlignmentVariationReport> almtVarReports) {
			super("generateVariationFrequenciesResult", almtVarReports, 
					column("alignmentName", avr -> avr.alignment.getName()),
					column("referenceName", avr -> avr.variation.getFeatureLoc().getReferenceSequence().getName()),
					column("featureName", avr -> avr.variation.getFeatureLoc().getFeature().getName()),
					column("variationName", avr -> avr.variation.getName()),
					column("frequency", avr -> avr.frequency)
					);
		}
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("acRefName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String almtName = (String) bindings.get("almtName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					return alignment.getAncConstrainingRefs().stream()
							.map(ancCR -> new CompletionSuggestion(ancCR.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("acRefName");
					ReferenceSequence referenceSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(referenceName), true);
					if(referenceSequence != null) {
						return referenceSequence.getFeatureLocations().stream()
								.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
								.collect(Collectors.toList());
					}
					return null;
				}
			});
		}


	}
	
}
