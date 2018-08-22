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
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a FASTA file", 
		docoptUsages = { "-i <fileName> -r <relRefName> -f <featureName> -t <targetRefName> -a <linkingAlmtName>" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                       FASTA input file",
				"-r <relRefName>, --relRefName <relRefName>                 Related reference",
				"-f <featureName>, --featureName <featureName>              Feature to translate",
				"-t <targetRefName>, --targetRefName <targetRefName>        Target reference",
				"-a <linkingAlmtName>, --linkingAlmtName <linkingAlmtName>  Linking alignment",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "translates a section of the query sequence to amino acids based on an alignment between the target reference "+
		        "and the related reference, where the coding feature is defined. "+
				"The target reference sequence must be a member of the specified linking alignment."+
		        "The <relRefName> argument specifies the related reference sequence, on which the feature is defined. "+
				"If the linking alignment is constrained, the related reference must constrain an ancestor alignment "+
		        "of the linking alignment. Otherwise, it may be any reference sequence which shares membership of the "+
				"linking alignment with the target reference. "+
				"The <featureName> arguments specifies a feature location on the related reference. "+
				"The translated amino acids will be limited to the specified feature location. ",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FastaSequenceAminoAcidCommand extends FastaSequenceReporterCommand<FastaSequenceAminoAcidResult> 
	implements ProvidedProjectModeCommand{

	
	public static final String FILE_NAME = "fileName";

	private String fileName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}
	

	@Override
	protected FastaSequenceAminoAcidResult execute(CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		
		Entry<String, DNASequence> fastaEntry = FastaSequenceReporter.getFastaEntry(consoleCmdContext, fileName);
		String fastaID = fastaEntry.getKey();
		DNASequence fastaNTSeq = fastaEntry.getValue();

		String targetRefName = getTargetRefName();
		
		AlignerResult alignerResult = fastaSequenceReporter
				.alignToTargetReference(consoleCmdContext, targetRefName, fastaID, fastaNTSeq);
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());
		Alignment linkingAlmt = linkingAlmtMember.getAlignment();

		ReferenceSequence relatedRef = linkingAlmt.getRelatedRef(cmdContext, getRelRefName());
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRelRefName(), getFeatureName()), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();
		
		// extract segments from aligner result
		List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		// translate segments to linking alignment coordinate space
		List<QueryAlignedSegment> queryToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
				linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);
		
		// translate segments to related reference
		List<QueryAlignedSegment> queryToRelatedRef = linkingAlmt.translateToRelatedRef(cmdContext, queryToLinkingAlmtSegs, relatedRef);

		String fastaNTs = fastaNTSeq.getSequenceAsString();

		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = 
				FeatureLocation.translateNtContent(consoleCmdContext, featureLoc, queryToRelatedRef, new SimpleNucleotideContentProvider(fastaNTs));
		return new FastaSequenceAminoAcidResult(labeledQueryAminoAcids);
		
	}

	@CompleterClass
	public static class Completer extends FastaSequenceReporterCommand.Completer {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
