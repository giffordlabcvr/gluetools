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
package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.VariableInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a FASTA file", 
		docoptUsages = { "-i <fileName> -r <targetRefName> -f <featureName>" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <targetRefName>, --targetRefName <targetRefName>  Target reference sequence",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "translates a specified feature within the query sequence to amino acids based on the alignment result. "+
				"The <featureName> arguments specifies a feature location on the target reference sequence. "+
				"The translated amino acids will be limited to the specified feature location. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceAminoAcidCommand extends ModulePluginCommand<FastaSequenceAminoAcidResult, FastaSequenceReporter> 
	implements ProvidedProjectModeCommand{

	public static final String FILE_NAME = "fileName";
	public static final String TARGET_REF_NAME = "targetRefName";
	public static final String FEATURE_NAME = "featureName";

	private String fileName;
	private String targetRefName;
	private String featureName;
	
	protected String getFeatureName() {
		return featureName;
	}

	protected String getTargetRefName() {
		return targetRefName;
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.targetRefName = PluginUtils.configureStringProperty(configElem, TARGET_REF_NAME, false);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}
	

	@Override
	protected FastaSequenceAminoAcidResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = FastaSequenceReporter.getFastaEntry(consoleCmdContext, fileName);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		AlignerResult alignerResult = fastaSequenceReporter
				.alignToTargetReference(consoleCmdContext, getTargetRefName(), fastaID, fastaNTSeq);

		// extract segments from aligner result
		List<QueryAlignedSegment> queryToRelatedRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		
		ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(getTargetRefName()));

		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = TranslationUtils.
				translateQaSegments(consoleCmdContext, relatedRef, getFeatureName(), queryToRelatedRefSegs, fastaNTSeq.getSequenceAsString());
		
		return new FastaSequenceAminoAcidResult(labeledQueryAminoAcids);
		
	}

	@CompleterClass
	public static class Completer extends FastaSequenceReporterCommand.Completer {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerDataObjectNameLookup("targetRefName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("featureName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String targetRefName = (String) bindings.get("targetRefName");
					if(targetRefName != null) {
						ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName), true);
						if(targetRef != null) {
							return targetRef.getFeatureLocations().stream()
									.map(fLoc -> new CompletionSuggestion(fLoc.getFeature().getName(), true))
									.collect(Collectors.toList());
						}
					}
					return null;
				}
			});

		}
		
	}
	
}
