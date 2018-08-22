/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.variationFrequencies;

import java.util.ArrayList;
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
import uk.ac.gla.cvr.gluetools.core.variationFrequencies.VariationFrequenciesGenerator.AlignmentVariationReport;

@CommandClass(
		commandWords={"generate", "frequencies"}, 
		description = "Generate variation frequencies as variation-alignment notes", 
		docoptUsages = { "<almtName> [-q] [-c] [-w <whereClause>] -r <relRefName> -f <featureName> [-d] [-v <vWhereClause>] [-p]" },
		docoptOptions = { 
		"-q, --alignmentRecursive                          Include descendent alignments",
		"-c, --recursive                                   Include descendent members",
		"-w <whereClause>, --whereClause <whereClause>     Qualify members",
		"-v <vWhereClause>, --vWhereClause <vWhereClause>  Qualify variations",
		"-r <relRefName>, --relRefName <relRefName>        Related reference sequence",
		"-f <featureName>, --featureName <featureName>     Feature containing variations",
		"-d, --descendentFeatures                          Include descendent features",
		"-p, --previewOnly                                 Preview only",
			
		},
		furtherHelp = 
		"The <relRefName> argument names a reference sequence constraining an ancestor alignment of this alignment (if constrained), "+
		"or simply a reference which is a member of this alignment (if unconstrained). "+
		"If --alignmentRecursive is used, variation-alignment notes will be generated for both this and descendent alignments. "+
		"If --multiReference is used, the set of possible variations includes those defined on any reference located on the "+
		"path between the named alignment's reference and the ancestor-constraining reference, in the alignment tree. "+
		"The <featureName> arguments names a feature which has a location defined on this ancestor-constraining reference. "+
		"If --descendentFeatures is used, variations will also be scanned on the descendent features of the named feature. "+
		"Variation-alignment notes will be created / updated for each variation whose frequency meets the statistical requirements "+
		"configured in this module. If --previewOnly is used, no notes will be created / updated, only a preview is returned. ",
		metaTags = {}	
)
public class GenerateVariationFrequenciesCommand extends ModulePluginCommand<GenerateVariationFrequenciesResult, VariationFrequenciesGenerator> {

	public static String ALIGNMENT_NAME = "almtName";
	public static String ALIGNMENT_RECURSIVE = "alignmentRecursive";
	public static String PREVIEW_ONLY = "previewOnly";
	
	public static final String RECURSIVE = AlignmentVariationFrequencyCmdDelegate.RECURSIVE;
	public static final String WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.WHERE_CLAUSE;
	public static final String VARIATION_WHERE_CLAUSE = AlignmentVariationFrequencyCmdDelegate.VARIATION_WHERE_CLAUSE;
	public static final String REL_REF_NAME = AlignmentVariationFrequencyCmdDelegate.REL_REF_NAME;
	public static final String FEATURE_NAME = AlignmentVariationFrequencyCmdDelegate.FEATURE_NAME;
	public static final String DESCENDENT_FEATURES = AlignmentVariationFrequencyCmdDelegate.DESCENDENT_FEATURES;
	
	private String almtName; 
	private boolean alignmentRecursive; 
	private boolean previewOnly; 
	
	private AlignmentVariationFrequencyCmdDelegate delegate = new AlignmentVariationFrequencyCmdDelegate();
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.alignmentRecursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALIGNMENT_RECURSIVE, false)).orElse(false);
		this.previewOnly = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, PREVIEW_ONLY, false)).orElse(false);
		this.delegate.configure(pluginConfigContext, configElem);
	}

	@Override
	protected GenerateVariationFrequenciesResult execute(CommandContext cmdContext,
			VariationFrequenciesGenerator variationFrequenciesGenerator) {
		Map<String, List<VariationScanMemberCount>> almtNameToScanCountList = delegate.execute(almtName, alignmentRecursive, cmdContext);
		List<AlignmentVariationReport> almtVarReports = new ArrayList<AlignmentVariationReport>();
		almtNameToScanCountList.forEach((almtName, scanCountList) -> {
			Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
			almtVarReports.addAll(variationFrequenciesGenerator.previewAlmtVarNotes(cmdContext, alignment, scanCountList));
		});
		variationFrequenciesGenerator.log("Generated "+almtVarReports.size()+" alignment-variation frequencies");
		if(!previewOnly) {
			variationFrequenciesGenerator.generateVarAlmtNotes(cmdContext, almtVarReports);
		}
		return new GenerateVariationFrequenciesResult(almtVarReports);
	}

	public static class GenerateVariationFrequenciesResult extends BaseTableResult<VariationFrequenciesGenerator.AlignmentVariationReport> {
		public GenerateVariationFrequenciesResult(List<VariationFrequenciesGenerator.AlignmentVariationReport> almtVarReports) {
			super("generateVariationFrequenciesResult", almtVarReports, 
					column("alignmentName", avr -> avr.alignment.getName()),
					column("referenceName", avr -> avr.variation.getFeatureLoc().getReferenceSequence().getName()),
					column("featureName", avr -> avr.variation.getFeatureLoc().getFeature().getName()),
					column("variationName", avr -> avr.variation.getName()),
					column("frequency", avr -> avr.frequency),
					column("totalPresent", avr -> avr.totalPresent),
					column("totalAbsent", avr -> avr.totalAbsent)
					);
		}
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("relRefName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String almtName = (String) bindings.get("almtName");
					Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName), false);
					return alignment.getRelatedRefs().stream()
							.map(relRef -> new CompletionSuggestion(relRef.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String referenceName = (String) bindings.get("relRefName");
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
