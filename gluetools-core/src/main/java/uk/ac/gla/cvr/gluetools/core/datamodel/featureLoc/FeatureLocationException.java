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
package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class FeatureLocationException extends GlueException {

	public enum Code implements GlueErrorCode {
		NEXT_ANCESTOR_FEATURE_LOCATION_UNDEFINED("refSeqName", "featureName", "nextAncestorFeatureName"),
		FEATURE_LOCATION_NOT_CONTAINED_WITHIN_NEXT_ANCESTOR("refSeqName", "featureName", "nextAncestorFeatureName"), 
		FEATURE_LOCATION_HAS_NO_SEGMENTS("refSeqName", "featureName"), 
		FEATURE_LOCATION_INVALID_CODON_LABEL("refSeqName", "featureName", "invalidCodonLabel"), 
		FEATURE_OR_ANCESTOR_MUST_HAVE_OWN_CODON_NUMBERING("featureName"),
		FEATURE_LOCATION_MUST_HAVE_SEGMENTS_TO_ESTABLISH_READING_FRAME("referenceName", "featureName"),
		CIRCULAR_GENOME_ERROR("referenceName", "featureName", "errorTxt"),
		SPLICE_INDEX_ERROR("referenceName", "featureName", "errorTxt"),
		CODING_FEATURE_LOCATION_HAS_UNCODED_REGIONS("referenceName", "featureName", "uncodedRegions"),
		CODING_FEATURE_LOCATION_DOES_NOT_COVER_CODON("referenceName", "featureName", "uncoveredCodon"),
		CODING_FEATURE_LOCATION_ERROR("referenceName", "featureName", "errorTxt");
		
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public FeatureLocationException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FeatureLocationException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
