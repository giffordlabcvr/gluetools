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
package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

public abstract class MemberBaseAminoAcidCommand<R extends CommandResult> extends MemberModeCommand<R> {


	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.referenceName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	public static List<LabeledQueryAminoAcid> memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence relatedRef, String featureName) {
		Sequence memberSequence = almtMember.getSequence();
		AbstractSequenceObject memberSeqObj = memberSequence.getSequenceObject();
		String nucleotides = memberSeqObj.getNucleotides(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		
		List<QueryAlignedSegment> memberToAlmtRefSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToRelatedRefSegsFull = alignment.translateToRelatedRef(cmdContext, memberToAlmtRefSegs, relatedRef);

		return TranslationUtils.translateQaSegments(cmdContext, relatedRef, featureName, memberToRelatedRefSegsFull, nucleotides);
	}

	protected List<LabeledQueryAminoAcid> getMemberAminoAcids(
			CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, referenceName);
		List<LabeledQueryAminoAcid> memberAminoAcids = memberAminoAcids(cmdContext, almtMember, relatedRef, featureName);
		return memberAminoAcids;
	}




	
}
