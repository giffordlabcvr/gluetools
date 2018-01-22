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

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter.FastaSequenceReporter.TranslatedQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate amino acids in a FASTA file", 
		docoptUsages = { "-i <fileName> -r <acRefName> -f <featureName> [-t <targetRefName>] [-a <tipAlmtName>]" },
		docoptOptions = { 
				"-i <fileName>, --fileName <fileName>                 FASTA input file",
				"-r <acRefName>, --acRefName <acRefName>              Ancestor-constraining ref",
				"-f <featureName>, --featureName <featureName>        Feature to translate",
				"-t <targetRefName>, --targetRefName <targetRefName>  Target reference",
				"-a <tipAlmtName>, --tipAlmtName <tipAlmtName>        Tip alignment",
		},
		furtherHelp = 
		        "This command aligns a FASTA query sequence to a 'target' reference sequence, and "+
		        "translates a section of the query sequence to amino acids based on the target reference sequence's "+
				"place in the alignment tree. "+
				"If <targetRefName> is not supplied, it may be inferred from the FASTA sequence ID, if the module is appropriately configured. "+
				"The target reference sequence must be a member of a constrained "+
		        "'tip alignment'. The tip alignment may be specified by <tipAlmtName>. If unspecified, it will be "+
		        "inferred from the target reference if possible. "+
		        "The <acRefName> argument specifies an 'ancestor-constraining' reference sequence. "+
				"This must be the constraining reference of an ancestor alignment of the tip alignment. "+
				"The <featureName> arguments specifies a feature location on the ancestor-constraining reference. "+
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
		
		if(targetRefName == null) {
			targetRefName = fastaSequenceReporter.targetRefNameFromFastaId(consoleCmdContext, fastaID);
		}
		
		AlignerResult alignerResult = fastaSequenceReporter
				.alignToTargetReference(consoleCmdContext, targetRefName, fastaID, fastaNTSeq);
		
		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember tipAlmtMember = targetRef.getTipAlignmentMembership(getTipAlmtName());
		Alignment tipAlmt = tipAlmtMember.getAlignment();

		ReferenceSequence ancConstrainingRef = tipAlmt.getAncConstrainingRef(cmdContext, getAcRefName());
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getAcRefName(), getFeatureName()), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();
		
		// extract segments from aligner result
		List<QueryAlignedSegment> queryToTargetRefSegs = alignerResult.getQueryIdToAlignedSegments().get(fastaID);

		// translate segments to tip alignment reference
		List<QueryAlignedSegment> queryToTipAlmtRefSegs = tipAlmt.translateToRef(cmdContext, 
				tipAlmtMember.getSequence().getSource().getName(), tipAlmtMember.getSequence().getSequenceID(), 
				queryToTargetRefSegs);
		
		// translate segments to ancestor constraining reference
		List<QueryAlignedSegment> queryToAncConstrRefSegsFull = tipAlmt.translateToAncConstrainingRef(cmdContext, queryToTipAlmtRefSegs, ancConstrainingRef);

		String fastaNTs = fastaNTSeq.getSequenceAsString();

		
		// trim down to the feature area.
		List<ReferenceSegment> featureLocRefSegs = featureLoc.segmentsAsReferenceSegments();
		
		List<QueryAlignedSegment> queryToAncConstrRefSegsFeatureArea = 
					ReferenceSegment.intersection(queryToAncConstrRefSegsFull, featureLocRefSegs, ReferenceSegment.cloneLeftSegMerger());
		
		List<TranslatedQueryAlignedSegment> translatedQaSegs = fastaSequenceReporter.translateNucleotides(
				cmdContext, featureLoc, queryToAncConstrRefSegsFeatureArea, fastaNTs);

		TIntObjectMap<LabeledCodon> ancRefNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = new ArrayList<LabeledQueryAminoAcid>();

		for(TranslatedQueryAlignedSegment translatedQaSeg: translatedQaSegs) {
			int refNt = translatedQaSeg.getQueryAlignedSegment().getRefStart();
			int queryNt = translatedQaSeg.getQueryAlignedSegment().getQueryStart();
			for(int i = 0; i < translatedQaSeg.getTranslation().length(); i++) {
				String segAA = translatedQaSeg.getTranslation().substring(i, i+1);
				labeledQueryAminoAcids.add(new LabeledQueryAminoAcid(new LabeledAminoAcid(ancRefNtToLabeledCodon.get(refNt), segAA), queryNt));
				refNt = refNt+3;
				queryNt = queryNt+3;
			}
		}

		
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
