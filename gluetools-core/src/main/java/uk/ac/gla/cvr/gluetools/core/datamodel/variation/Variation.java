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
package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasDisplayName;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseNucleotideVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ExactMatchAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ExactMatchNucleotideVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.RegexAminoAcidVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.RegexNucleotideVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

@GlueDataClass(
		defaultListedProperties = { Variation.REF_SEQ_NAME_PATH, Variation.FEATURE_NAME_PATH, _Variation.NAME_PROPERTY, _Variation.DESCRIPTION_PROPERTY },
		listableBuiltInProperties = { _Variation.NAME_PROPERTY, _Variation.DISPLAY_NAME_PROPERTY, Variation.TRANSLATION_TYPE_PROPERTY, Variation.FEATURE_NAME_PATH, Variation.REF_SEQ_NAME_PATH, 
				 _Variation.DESCRIPTION_PROPERTY, _Variation.SCANNER_MODULE_NAME_PROPERTY },
		modifiableBuiltInProperties = { _Variation.DESCRIPTION_PROPERTY, _Variation.DISPLAY_NAME_PROPERTY, _Variation.SCANNER_MODULE_NAME_PROPERTY })		
public class Variation extends _Variation implements HasDisplayName {

	private Boolean isSimpleMatch = null;
	private BaseVariationScanner<?,?> scanner = null;

	public static final String FEATURE_NAME_PATH = _Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;

	public static Map<String, String> pkMap(String referenceName, String featureName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}
	
	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getFeatureLoc().getReferenceSequence().getName(), getFeatureLoc().getFeature().getName(), getName());
	}
	
	public TranslationFormat getTranslationFormat() {
		return TranslationUtils.translationFormatFromString(getTranslationType());
	}	

	public Boolean isSimpleMatch() {
		if(isSimpleMatch == null) {
			isSimpleMatch = buildIsSimpleMatch();
		}
		return isSimpleMatch;
	}
	
	private Boolean buildIsSimpleMatch() {
		for(PatternLocation patternLoc : getPatternLocs()) {
			if(!patternLoc.isSimpleMatch()) {
				return false;
			}
		}
		return true;
	}	
	
	
	@Override
	public void writeProperty(String propName, Object val) {
		super.writeProperty(propName, val);
		if(propName.equals(SCANNER_MODULE_NAME_PROPERTY)) {
			this.scanner = null;
			getPatternLocs().forEach(p -> p.clearScannerData());
		}
		
	}


	public void validate(CommandContext cmdContext) {
		
		FeatureLocation featureLoc = getFeatureLoc();
		Feature feature = featureLoc.getFeature();
		ReferenceSequence refSeq = featureLoc.getReferenceSequence();
		
		TranslationFormat translationFormat = getTranslationFormat();
		if(translationFormat == TranslationFormat.AMINO_ACID) {
			if(!featureLoc.getFeature().codesAminoAcids()) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_MUST_BE_DEFINED_ON_CODING_FEATURE, 
						refSeq.getName(), feature.getName(), getName());
			}
			FeatureLocation codonNumberingAncestorLocation = featureLoc.getCodonNumberingAncestorLocation(cmdContext);
			if(codonNumberingAncestorLocation == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR, 
						refSeq.getName(), feature.getName(), getName(), translationFormat.name());
			}
		}
		getPatternLocs().forEach(loc -> loc.validate(cmdContext));
		getScanner(cmdContext).validateVariation(this);;
	}	


	public VariationScanResult scanNucleotideVariation(CommandContext cmdContext, NtQueryAlignedSegment ntQaSeg) {
		BaseVariationScanner<?, ?> scanner = this.getScanner(cmdContext);
		BaseNucleotideVariationScanner<?, ?> nucleotideScanner;
		try {
			nucleotideScanner = (BaseNucleotideVariationScanner<?, ?>) scanner;
		} catch(ClassCastException cce) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					this.getFeatureLoc().getReferenceSequence().getName(), this.getFeatureLoc().getFeature().getName(), this.getName(), 
					BaseNucleotideVariationScanner.class.getSimpleName());
		}
		return nucleotideScanner.scanNucleotides(this, ntQaSeg);
	}


	public VariationScanResult scanAminoAcids(CommandContext cmdContext, 
			NtQueryAlignedSegment ntQaSegCdnAligned, String fullAminoAcidTranslation) {
		BaseVariationScanner<?, ?> scanner = this.getScanner(cmdContext);
		BaseAminoAcidVariationScanner<?, ?> aminoAcidScanner;
		try {
			aminoAcidScanner = (BaseAminoAcidVariationScanner<?, ?>) scanner;
		} catch(ClassCastException cce) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					this.getFeatureLoc().getReferenceSequence().getName(), this.getFeatureLoc().getFeature().getName(), this.getName(), 
					BaseAminoAcidVariationScanner.class.getSimpleName());
		}
		return aminoAcidScanner.scanAminoAcids(cmdContext, this, ntQaSegCdnAligned, fullAminoAcidTranslation);
	}

	
	private BaseVariationScanner<?, ?> getScanner(CommandContext cmdContext) {
		if(this.scanner == null) {
			this.scanner = buildScanner(cmdContext);
		}
		return this.scanner;
	}

	private BaseVariationScanner<?, ?> buildScanner(CommandContext cmdContext) {
		String scannerModuleName = getScannerModuleName();
		if(scannerModuleName != null) {
			return Module.resolveModulePlugin(cmdContext, BaseVariationScanner.class, scannerModuleName);
		}
		TranslationFormat translationFormat = getTranslationFormat();
		switch(translationFormat) {
		case NUCLEOTIDE:
			if(isSimpleMatch()) {
				return ExactMatchNucleotideVariationScanner.getDefaultInstance();
			} else {
				return RegexNucleotideVariationScanner.getDefaultInstance();
			}
		case AMINO_ACID:
			if(isSimpleMatch()) {
				return ExactMatchAminoAcidVariationScanner.getDefaultInstance();
			} else {
				return RegexAminoAcidVariationScanner.getDefaultInstance();
			}
		default:
			throw new RuntimeException("Unknown translation type");
		}
	}
	
	/**
	 * @return List of variation-alignment notes associated with this variation, 
	 * ordered by the depth of the alignment in question in the alignment tree.
	 */
	public List<VarAlmtNote> getVarAlmtNotesOrderedByDepth() {
		List<VarAlmtNote> varAlmtNotes = new ArrayList<VarAlmtNote>(super.getVarAlmtNotes());
		Collections.sort(varAlmtNotes, new Comparator<VarAlmtNote>() {
			@Override
			public int compare(VarAlmtNote van1, VarAlmtNote van2) {
				Alignment almt1 = van1.getAlignment();
				Alignment almt2 = van2.getAlignment();
				int comp = Integer.compare(almt1.getDepth(), almt2.getDepth());
				if(comp != 0) {
					return comp;
				}
				return almt1.getName().compareTo(almt2.getName());
			}
		});
		
		return varAlmtNotes;
	}
	
	public Integer minLocStart() {
		Integer minLocStart = null;
		for(PatternLocation loc: getPatternLocs()) {
			Integer locStart = loc.getRefStart();
			if(minLocStart == null || locStart < minLocStart) {
				minLocStart = locStart;
			}
		}
		return minLocStart;
	}

	public Integer maxLocEnd() {
		Integer maxLocEnd = null;
		for(PatternLocation loc: getPatternLocs()) {
			Integer locEnd = loc.getRefEnd();
			if(maxLocEnd == null || locEnd > maxLocEnd) {
				maxLocEnd = locEnd;
			}
		}
		return maxLocEnd;
	}
	
}
