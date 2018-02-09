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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

public abstract class FeatureLocBaseAminoAcidCommand<R extends CommandResult> extends FeatureLocModeCommand<R> {

	public static List<LabeledQueryAminoAcid> featureLocAminoAcids(CommandContext cmdContext, FeatureLocation featureLoc) {
		
		
		String refNts = featureLoc.getReferenceSequence().getSequence().getSequenceObject().getNucleotides(cmdContext);
		int length = refNts.length();
		
		return TranslationUtils.translateQaSegments(cmdContext,
				featureLoc.getReferenceSequence(), featureLoc.getFeature().getName(), 
					Arrays.asList(new QueryAlignedSegment(1, length, 1, length)), refNts);
		
	}

}
