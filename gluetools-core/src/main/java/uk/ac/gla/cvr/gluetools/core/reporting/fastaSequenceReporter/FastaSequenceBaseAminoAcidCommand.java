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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public abstract class FastaSequenceBaseAminoAcidCommand extends FastaSequenceReporterCommand<FastaSequenceAminoAcidResult> 
	implements ProvidedProjectModeCommand{

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected FastaSequenceAminoAcidResult executeAux(
			CommandContext cmdContext,
			FastaSequenceReporter fastaSequenceReporter, String fastaID,
			DNASequence fastaNTSeq, String targetRefName, List<QueryAlignedSegment> establishedQueryToTargetRefSegs) {

		ReferenceSequence targetRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(targetRefName));

		AlignmentMember linkingAlmtMember = targetRef.getLinkingAlignmentMembership(getLinkingAlmtName());
		Alignment linkingAlmt = linkingAlmtMember.getAlignment();

		// extract segments from aligner result

		IAminoAcidAlignmentColumnsSelector aaColumnsSelector = getAminoAcidAlignmentColumnsSelector(cmdContext);
		aaColumnsSelector.checkAminoAcidSelector(cmdContext);
		
		// translate segments to linking alignment coordinate space
		List<QueryAlignedSegment> queryToLinkingAlmtSegs = linkingAlmt.translateToAlmt(cmdContext, 
				linkingAlmtMember.getSequence().getSource().getName(), linkingAlmtMember.getSequence().getSequenceID(), 
				establishedQueryToTargetRefSegs);
		
		ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(aaColumnsSelector.getRelatedRefName()));

		// translate segments to related reference
		List<QueryAlignedSegment> queryToRelatedRef = linkingAlmt.translateToRelatedRef(cmdContext, queryToLinkingAlmtSegs, relatedRef);

		String fastaNTs = fastaNTSeq.getSequenceAsString();

		Translator translator = new CommandContextTranslator(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = 
				aaColumnsSelector.translateQueryNucleotides(cmdContext, translator, queryToRelatedRef, new SimpleNucleotideContentProvider(fastaNTs));
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
