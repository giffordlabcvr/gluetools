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
package uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@GlueDataClass(defaultListedProperties = {_FeatureSegment.REF_START_PROPERTY, _FeatureSegment.REF_END_PROPERTY, _FeatureSegment.PRE_TRANSLATION_MODIFIER_NAME_PROPERTY})
public class FeatureSegment extends _FeatureSegment implements IReferenceSegment {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureSegment.FEATURE_LOCATION_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+
					_ReferenceSequence.NAME_PROPERTY;

	public static final String FEATURE_NAME_PATH = 
			_FeatureSegment.FEATURE_LOCATION_PROPERTY+"."+FeatureLocation.FEATURE_NAME_PATH;


	
	public static Map<String, String> pkMap(String refSeqName, String featureName, 
			int refStart, int refEnd) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, refSeqName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(REF_START_PROPERTY, Integer.toString(refStart));
		idMap.put(REF_END_PROPERTY, Integer.toString(refEnd));
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setRefStart(Integer.parseInt(pkMap.get(REF_START_PROPERTY)));
		setRefEnd(Integer.parseInt(pkMap.get(REF_END_PROPERTY)));
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(
				getFeatureLocation().getReferenceSequence().getName(), 
				getFeatureLocation().getFeature().getName(),
				getRefStart(), 
				getRefEnd());
	}

	public ReferenceSegment asReferenceSegment() {
		return new ReferenceSegment(getRefStart(), getRefEnd());
	}
	
	public FeatureSegment clone() {
		FeatureSegment copy = new FeatureSegment();
		copy.setRefStart(getRefStart());
		copy.setRefEnd(getRefEnd());
		return copy;
	}

	public String toString() {
		return "["+getRefStart()+", "+getRefEnd()+"]";
	}

}
