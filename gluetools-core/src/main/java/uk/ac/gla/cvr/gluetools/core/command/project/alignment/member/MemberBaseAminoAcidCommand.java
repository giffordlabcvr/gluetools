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
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public abstract class MemberBaseAminoAcidCommand<R extends CommandResult> extends MemberModeCommand<R> {


	public static final String REL_REF_NAME = "relRefName";
	public static final String FEATURE_NAME = "featureName";

	private String relRefName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, REL_REF_NAME, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	public static List<LabeledQueryAminoAcid> memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, ReferenceSequence relatedRef, FeatureLocation featureLoc) {
		Alignment alignment = almtMember.getAlignment();

		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToRelatedRefSegs = alignment.translateToRelatedRef(cmdContext, memberToAlmtSegs, relatedRef);

		Sequence memberSequence = almtMember.getSequence();
		NucleotideContentProvider memberSeqObj = memberSequence.getSequenceObject();

		Translator translator = new CommandContextTranslator(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = 
				featureLoc.translateQueryNucleotides(cmdContext, translator, memberToRelatedRefSegs, memberSeqObj);
		return labeledQueryAminoAcids;
	}

	public static List<LabeledQueryAminoAcid> memberAminoAcids(CommandContext cmdContext,
			AlignmentMember almtMember, IAminoAcidAlignmentColumnsSelector aaColumnsSelector) {
		aaColumnsSelector.checkAminoAcidSelector(cmdContext);
		Alignment alignment = almtMember.getAlignment();

		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();
		String relatedRefName = aaColumnsSelector.getRelatedRefName();
		ReferenceSequence relatedRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(relatedRefName));
		List<QueryAlignedSegment> memberToRelatedRefSegs = alignment.translateToRelatedRef(cmdContext, memberToAlmtSegs, relatedRef);

		Sequence memberSequence = almtMember.getSequence();
		NucleotideContentProvider memberSeqObj = memberSequence.getSequenceObject();

		Translator translator = new CommandContextTranslator(cmdContext);
		List<LabeledQueryAminoAcid> labeledQueryAminoAcids = 
				aaColumnsSelector.translateQueryNucleotides(cmdContext, translator, memberToRelatedRefSegs, memberSeqObj);
		return labeledQueryAminoAcids;
	}

	
	protected List<LabeledQueryAminoAcid> getMemberAminoAcids(
			CommandContext cmdContext) {
		AlignmentMember almtMember = lookupMember(cmdContext);
		Alignment alignment = almtMember.getAlignment();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, relRefName);
		FeatureLocation featureLoc = 
				GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName), false);
		Feature feature = featureLoc.getFeature();
		feature.checkCodesAminoAcids();
		List<LabeledQueryAminoAcid> memberAminoAcids = memberAminoAcids(cmdContext, almtMember, relatedRef, featureLoc);
		return memberAminoAcids;
	}

	protected String getRelRefName() {
		return relRefName;
	}

	protected String getFeatureName() {
		return featureName;
	}




	
}
