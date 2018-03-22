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
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidDeletionMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidDeletionScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidInsertionMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidInsertionScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidRegexPolymorphismMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidRegexPolymorphismScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidSimplePolymorphismMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.AminoAcidSimplePolymorphismScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.BaseVariationScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideDeletionMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideDeletionScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideInsertionMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideInsertionScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideRegexPolymorphismMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideRegexPolymorphismScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideSimplePolymorphismMatchResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.NucleotideSimplePolymorphismScanner;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

@GlueDataClass(
		defaultListedProperties = { Variation.REF_SEQ_NAME_PATH, Variation.FEATURE_NAME_PATH, _Variation.NAME_PROPERTY, _Variation.DESCRIPTION_PROPERTY, _Variation.REF_START_PROPERTY, _Variation.REF_END_PROPERTY},
		listableBuiltInProperties = { _Variation.NAME_PROPERTY, _Variation.DISPLAY_NAME_PROPERTY, Variation.TYPE_PROPERTY, Variation.FEATURE_NAME_PATH, Variation.REF_SEQ_NAME_PATH, 
				 _Variation.DESCRIPTION_PROPERTY, _Variation.REF_START_PROPERTY, _Variation.REF_END_PROPERTY },
		modifiableBuiltInProperties = { _Variation.DESCRIPTION_PROPERTY, _Variation.DISPLAY_NAME_PROPERTY })		
public class Variation extends _Variation implements HasDisplayName {

	private BaseVariationScanner<?> scanner = null;
	private VariationType variationType = null;
	
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
	
	public enum VariationType {
		nucleotideSimplePolymorphism(NucleotideSimplePolymorphismScanner.class, NucleotideSimplePolymorphismMatchResult.class),
		nucleotideRegexPolymorphism(NucleotideRegexPolymorphismScanner.class, NucleotideRegexPolymorphismMatchResult.class),
		nucleotideInsertion(NucleotideInsertionScanner.class, NucleotideInsertionMatchResult.class),
		nucleotideDeletion(NucleotideDeletionScanner.class, NucleotideDeletionMatchResult.class),
		aminoAcidSimplePolymorphism(AminoAcidSimplePolymorphismScanner.class, AminoAcidSimplePolymorphismMatchResult.class),
		aminoAcidRegexPolymorphism(AminoAcidRegexPolymorphismScanner.class, AminoAcidRegexPolymorphismMatchResult.class),
		aminoAcidInsertion(AminoAcidInsertionScanner.class, AminoAcidInsertionMatchResult.class),
		aminoAcidDeletion(AminoAcidDeletionScanner.class, AminoAcidDeletionMatchResult.class);
		
		private Class<? extends BaseVariationScanner<?>> scannerClass;
		private Class<? extends VariationScannerMatchResult> matchResultClass;
		
		private VariationType(Class<? extends BaseVariationScanner<?>> scannerClass, 
				Class<? extends VariationScannerMatchResult> matchResultClass) {
			this.scannerClass = scannerClass;
			this.matchResultClass = matchResultClass;
		}
		
		public Class<? extends BaseVariationScanner<?>> getScannerClass() {
			return this.scannerClass;
		}
		
		public Class<? extends VariationScannerMatchResult> getMatchResultClass() {
			return this.matchResultClass;
		}

	}
	
	public VariationType getVariationType() {
		if(this.variationType == null) {
			this.variationType = VariationType.valueOf(getType());
		}
		return variationType;
	}	

	public void validate(CommandContext cmdContext) {
		
		FeatureLocation featureLoc = getFeatureLoc();
		Feature feature = featureLoc.getFeature();
		ReferenceSequence refSeq = featureLoc.getReferenceSequence();
		
		VariationType variationType = getVariationType();
		if(variationType.name().startsWith("aminoAcid")) {
			if(!featureLoc.getFeature().codesAminoAcids()) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_MUST_BE_DEFINED_ON_CODING_FEATURE, 
						refSeq.getName(), feature.getName(), getName());
			}
			FeatureLocation codonNumberingAncestorLocation = featureLoc.getCodonNumberingAncestorLocation(cmdContext);
			if(codonNumberingAncestorLocation == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR, 
						refSeq.getName(), feature.getName(), getName(), variationType);
			}
		}
		Integer refStart = getRefStart();
		Integer refEnd = getRefEnd();
		if(variationType.name().startsWith("nucleotide")) {
			List<FeatureSegment> featureLocSegments = featureLoc.getSegments();
			if(!ReferenceSegment.covers(featureLocSegments, 
					Collections.singletonList(new ReferenceSegment(refStart, refEnd)))) {
				throw new VariationException(Code.VARIATION_LOCATION_OUT_OF_RANGE, 
						refSeq.getName(), feature.getName(), getName(), 
						Integer.toString(refStart), Integer.toString(refEnd));
			}
		} else if(variationType.name().startsWith("aminoAcid")) {
			Integer codon1Start = featureLoc.getCodon1Start(cmdContext);
			if(! ( 
					TranslationUtils.isAtEndOfCodon(codon1Start, refEnd) && 
					TranslationUtils.isAtStartOfCodon(codon1Start, refStart))) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_NOT_CODON_ALIGNED, 
						refSeq.getName(), feature.getName(), getName(), Integer.toString(refStart), Integer.toString(refEnd));
			}
		} else {
			throw new RuntimeException("Unknown variation type");
		}
		getScanner(cmdContext).validate();;
		
	}	


	public BaseVariationScanner<?> getScanner(CommandContext cmdContext) {
		if(this.scanner == null) {
			this.scanner = buildScanner(cmdContext);
		}
		return this.scanner;
	}

	private BaseVariationScanner<?> buildScanner(CommandContext cmdContext) {
		VariationType variationType = getVariationType();
		Class<? extends BaseVariationScanner<?>> scannerClass = variationType.getScannerClass();
		try {
			BaseVariationScanner<?> newScanner = scannerClass.newInstance();
			newScanner.init(cmdContext, this);
			return newScanner;
		} catch(ReflectiveOperationException roe) {
			throw new RuntimeException("Unable to instantiate scanner class for variation type: "+variationType+": "+roe.getLocalizedMessage(), roe);
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
	
	public Map<VariationMetatagType, String> getMetatagsMap() {
		Map<VariationMetatagType, String> metatagsMap = new LinkedHashMap<VariationMetatagType, String>();
		getMetatags().forEach(metatag -> {
			metatagsMap.put(VariationMetatagType.valueOf(metatag.getName()), metatag.getValue());
		});
		return metatagsMap;
	}

	public void clearCachedScanner() {
		this.scanner = null;
	}
	
}
