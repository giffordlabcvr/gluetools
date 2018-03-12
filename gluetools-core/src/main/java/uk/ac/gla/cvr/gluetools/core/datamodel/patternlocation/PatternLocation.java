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
package uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@GlueDataClass(
		defaultListedProperties = { _PatternLocation.REF_START_PROPERTY, _PatternLocation.REF_END_PROPERTY, _PatternLocation.PATTERN_PROPERTY },
		listableBuiltInProperties = { _PatternLocation.REF_START_PROPERTY, _PatternLocation.REF_END_PROPERTY, _PatternLocation.PATTERN_PROPERTY },
		modifiableBuiltInProperties = { _PatternLocation.PATTERN_PROPERTY })		
public class PatternLocation extends _PatternLocation implements IReferenceSegment {

	
	public static final String VARIATION_NAME_PATH = _PatternLocation.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;
	public static final String FEATURE_NAME_PATH = _PatternLocation.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _PatternLocation.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;

	// map for the scanner module to put computed values
	private Map<String, Object> scannerDataCache = new LinkedHashMap<String, Object>();
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setRefStart(Integer.parseInt(pkMap.get(REF_START_PROPERTY)));
		setRefEnd(Integer.parseInt(pkMap.get(REF_END_PROPERTY)));
	}
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getVariation().getFeatureLoc().getReferenceSequence().getName(), 
				getVariation().getFeatureLoc().getFeature().getName(), 
				getVariation().getName(), getRefStart(), getRefEnd());
	}

	public static Map<String, String> pkMap(String refSeqName, String featureName, String variationName, Integer refStart, Integer refEnd) {
		Map<String, String> pkMap = new LinkedHashMap<String, String>();
		pkMap.put(REF_SEQ_NAME_PATH, refSeqName);
		pkMap.put(FEATURE_NAME_PATH, featureName);
		pkMap.put(VARIATION_NAME_PATH, variationName);
		pkMap.put(REF_START_PROPERTY, Integer.toString(refStart));
		pkMap.put(REF_END_PROPERTY, Integer.toString(refEnd));
		return pkMap;
	}
	
	
	public PatternLocation clone() {
		throw new RuntimeException("PatternLocation.clone() not supported");
	}
	
	
	public void setScannerData(String key, Object data) {
		scannerDataCache.put(key, data);
	}

	public Object getScannerData(String key) {
		return scannerDataCache.get(key);
	}

	public void clearScannerData() {
		scannerDataCache.clear();
	}

	@Override
	public void writeProperty(String propName, Object val) {
		super.writeProperty(propName, val);
		if(propName.equals(PATTERN_PROPERTY)) {
			clearScannerData();
		} 
	}
	public void validate(CommandContext cmdContext) {
		Variation variation = getVariation();
		FeatureLocation featureLoc = variation.getFeatureLoc();
		Feature feature = featureLoc.getFeature();
		ReferenceSequence refSeq = featureLoc.getReferenceSequence();
		
		
		Integer refStart = getRefStart();
		Integer refEnd = getRefEnd();
		String pattern = getPattern();

		if(refStart == null || refEnd == null) {
			throw new VariationException(Code.VARIATION_LOCATION_UNDEFINED, 
					refSeq.getName(), feature.getName(), variation.getName());
		}
		if(pattern == null) {
			throw new VariationException(Code.VARIATION_PATTERN_UNDEFINED, 
					refSeq.getName(), feature.getName(), variation.getName());
		}
		List<FeatureSegment> featureLocSegments = featureLoc.getSegments();
		TranslationFormat translationFormat = variation.getTranslationFormat();
		if(translationFormat == TranslationFormat.NUCLEOTIDE) {
			if(!ReferenceSegment.covers(featureLocSegments, 
					Collections.singletonList(new ReferenceSegment(refStart, refEnd)))) {
				throw new VariationException(Code.VARIATION_LOCATION_OUT_OF_RANGE, 
						refSeq.getName(), feature.getName(), variation.getName(), 
						Integer.toString(refStart), Integer.toString(refEnd));
			}
		} else if(translationFormat == TranslationFormat.AMINO_ACID) {
			Integer codon1Start = featureLoc.getCodon1Start(cmdContext);
			if(! ( 
					TranslationUtils.isAtEndOfCodon(codon1Start, refEnd) && 
					TranslationUtils.isAtStartOfCodon(codon1Start, refStart))) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_NOT_CODON_ALIGNED, 
						refSeq.getName(), feature.getName(), variation.getName(), Integer.toString(refStart), Integer.toString(refEnd));
			}
		}

		
		
	}
	public ReferenceSegment asReferenceSegment() {
		return new ReferenceSegment(getRefStart(), getRefEnd());
	}

}