package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.console.Lexer;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.HasDisplayName;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldTranslator;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote.VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
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
				Variation.PATTERN_PROPERTY, _Variation.DESCRIPTION_PROPERTY, _Variation.REF_START_PROPERTY, _Variation.REF_END_PROPERTY },
		modifiableBuiltInProperties = { _Variation.DESCRIPTION_PROPERTY, _Variation.DISPLAY_NAME_PROPERTY, _Variation.PATTERN_PROPERTY })		
public class Variation extends _Variation implements IReferenceSegment, HasDisplayName {

	private static Pattern SIMPLE_NT_PATTERN = Pattern.compile("[NACGT]+");
	private static Pattern SIMPLE_AA_PATTERN = Pattern.compile("[ACDEFGHIKLMNOPQRSTUVWYX*]+");
	
	// map for the scanner module to put computed values
	private Map<String, Object> scannerDataCache = new LinkedHashMap<String, Object>();
	
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
		String regex = getPattern();
		int ntLength = (getRefEnd()-getRefStart())+1;
		if(getTranslationFormat() == TranslationFormat.NUCLEOTIDE) {
			if(ntLength == regex.length() && SIMPLE_NT_PATTERN.matcher(regex).find()) {
				return true;
			}
			return false;
		} else {
			if(ntLength/3 == regex.length() && SIMPLE_AA_PATTERN.matcher(regex).find()) {
				return true;
			}
			return false;
		}
	}	
	
	
	@Override
	public void writePropertyDirectly(String propName, Object val) {
		super.writePropertyDirectly(propName, val);
		if(propName.equals(PATTERN_PROPERTY)) {
			this.scannerDataCache.clear();
		} else if(propName.equals(SCANNER_MODULE_NAME_PROPERTY)) {
			this.scanner = null;
			this.scannerDataCache.clear();
		}
		
	}


	public void validate(CommandContext cmdContext) {
		FeatureLocation featureLoc = getFeatureLoc();
		Feature feature = featureLoc.getFeature();
		ReferenceSequence refSeq = featureLoc.getReferenceSequence();
		Integer refStart = getRefStart();
		Integer refEnd = getRefEnd();
		if(refStart == null || refEnd == null) {
			throw new VariationException(Code.VARIATION_LOCATION_UNDEFINED, 
					refSeq.getName(), feature.getName(), getName());
		}
		if(getPattern() == null) {
			throw new VariationException(Code.VARIATION_PATTERN_UNDEFINED, 
					refSeq.getName(), feature.getName(), getName());
		}
		List<FeatureSegment> featureLocSegments = featureLoc.getSegments();
		TranslationFormat translationFormat = getTranslationFormat();
		if(translationFormat == TranslationFormat.NUCLEOTIDE) {
			if(!ReferenceSegment.covers(featureLocSegments, 
					Collections.singletonList(new ReferenceSegment(refStart, refEnd)))) {
				throw new VariationException(Code.VARIATION_LOCATION_OUT_OF_RANGE, 
						refSeq.getName(), feature.getName(), getName(), 
						Integer.toString(refStart), Integer.toString(refEnd));
			}
		} else if(translationFormat == TranslationFormat.AMINO_ACID) {
			if(!featureLoc.getFeature().codesAminoAcids()) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_MUST_BE_DEFINED_ON_CODING_FEATURE, 
						refSeq.getName(), feature.getName(), getName());
			}
			FeatureLocation codonNumberingAncestorLocation = featureLoc.getCodonNumberingAncestorLocation(cmdContext);
			if(codonNumberingAncestorLocation == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR, 
						refSeq.getName(), feature.getName(), getName(), translationFormat.name());
			}
			Integer codon1Start = featureLoc.getCodon1Start(cmdContext);
			if(! ( 
					TranslationUtils.isAtEndOfCodon(codon1Start, refEnd) && 
					TranslationUtils.isAtStartOfCodon(codon1Start, refStart))) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_NOT_CODON_ALIGNED, 
						refSeq.getName(), feature.getName(), getName(), Integer.toString(refStart), Integer.toString(refEnd));
			}
		}
		getScanner(cmdContext).validateVariation(this);;
	}	

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		String noCommit = glueConfigContext.getNoCommit() ? "--noCommit " : "";
		String regex = getPattern();
		if(regex != null) {
			indent(glueConfigBuf, indent).append("set pattern "+noCommit+"\""+regex+"\"").append("\n");
		}
		Integer refStart = getRefStart();
		if(refStart != null) {
			if(getTranslationFormat() == TranslationFormat.AMINO_ACID) {
				TIntObjectMap<LabeledCodon> refNtToLabeledCodon = getFeatureLoc().getRefNtToLabeledCodon(glueConfigContext.getCommandContext());
				indent(glueConfigBuf, indent).append("set location "+noCommit+"-c "+
						refNtToLabeledCodon.get(getRefStart()).getCodonLabel()+" "+
						refNtToLabeledCodon.get(getRefEnd()-2).getCodonLabel()).append("\n");
				
			} else {			
				indent(glueConfigBuf, indent).append("set location "+noCommit+"-n "+getRefStart()+" "+getRefEnd()).append("\n");
			}
		}
		List<String> modifiableFieldNames = glueConfigContext.getProject().getModifiableFieldNames(ConfigurableTable.variation.name());
		for(String fieldName: modifiableFieldNames) {
			Object value = readProperty(fieldName);
			if(value != null) {
				FieldType fieldType = glueConfigContext.getProject().getModifiableFieldType(ConfigurableTable.variation.name(), fieldName);
				FieldTranslator<?> fieldTranslator = fieldType.getFieldTranslator();
				String valueAsString = fieldTranslator.objectValueToString(value);
				indent(glueConfigBuf, indent).append("set field "+noCommit+fieldName+" "+Lexer.quotifyIfNecessary(valueAsString)).append("\n");
			}
		}
		
	}

	public VariationScanResult scanNucleotideVariation(
			CommandContext cmdContext, NtQueryAlignedSegment ntQaSeg) {
		BaseVariationScanner<?, ?> scanner = this.getScanner(cmdContext);
		BaseNucleotideVariationScanner<?, ?> nucleotideScanner;
		try {
			nucleotideScanner = (BaseNucleotideVariationScanner<?, ?>) scanner;
		} catch(ClassCastException cce) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					this.getFeatureLoc().getReferenceSequence().getName(), this.getFeatureLoc().getFeature().getName(), this.getName(), 
					BaseNucleotideVariationScanner.class.getSimpleName());
		}
		Integer refStart = this.getRefStart();
		Integer refEnd = this.getRefEnd();
		if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
			return null;
		}
		ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
		List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
				ReferenceSegment.cloneLeftSegMerger());
		if(intersection.isEmpty()) {
			return null;
		}
		NtQueryAlignedSegment intersectionSeg = intersection.get(0);
		CharSequence nucleotides = intersectionSeg.getNucleotides();
		return nucleotideScanner.scanNucleotides(this, nucleotides, intersectionSeg.getQueryStart());
	}


	public VariationScanResult scanAminoAcids(
			CommandContext cmdContext, NtQueryAlignedSegment ntQaSegCdnAligned,
			String fullAminoAcidTranslation) {
		BaseAminoAcidVariationScanner<?, ?> aminoAcidScanner;
		try {
			aminoAcidScanner = (BaseAminoAcidVariationScanner<?, ?>) this.scanner;
		} catch(ClassCastException cce) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					this.getFeatureLoc().getReferenceSequence().getName(), this.getFeatureLoc().getFeature().getName(), this.getName(), 
					BaseAminoAcidVariationScanner.class.getSimpleName());
		}
		Integer refStart = this.getRefStart();
		Integer refEnd = this.getRefEnd();
		int varLengthNt = refEnd - refStart + 1;
		Integer aaTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
		Integer aaTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
		if(!( refStart >= aaTranslationRefNtStart && refEnd <= aaTranslationRefNtEnd )) {
			return null;
		}
		int segToVariationStartOffset = refStart - aaTranslationRefNtStart;
		int startAA = segToVariationStartOffset / 3;
		int endAA = startAA + ( (varLengthNt / 3) - 1);
		CharSequence aminoAcidsForVariation = fullAminoAcidTranslation.subSequence(startAA, endAA+1);
		int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;
		return aminoAcidScanner.scanAminoAcids(this, aminoAcidsForVariation, scanQueryNtStart);
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
	
	
	public Variation clone() {
		throw new RuntimeException("Variation.clone() not supported");
	}
	
	public void setScannerData(String key, Object data) {
		scannerDataCache.put(key, data);
	}

	public Object getScannerData(String key) {
		return scannerDataCache.get(key);
	}

}
