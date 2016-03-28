package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import gnu.trove.map.TIntObjectMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership.VcatMembership;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;

@GlueDataClass(
		defaultListedProperties = { Variation.REF_SEQ_NAME_PATH, Variation.FEATURE_NAME_PATH, _Variation.NAME_PROPERTY, _Variation.DESCRIPTION_PROPERTY },
		listableBuiltInProperties = { _Variation.NAME_PROPERTY, Variation.TRANSLATION_TYPE_PROPERTY, Variation.FEATURE_NAME_PATH, Variation.REF_SEQ_NAME_PATH, 
				Variation.REGEX_PROPERTY, _Variation.DESCRIPTION_PROPERTY, _Variation.REF_START_PROPERTY, _Variation.REF_END_PROPERTY },
		modifiableBuiltInProperties = { _Variation.DESCRIPTION_PROPERTY })		
public class Variation extends _Variation implements IReferenceSegment {

	private static Pattern SIMPLE_NT_PATTERN = Pattern.compile("[NACGT]+");
	private static Pattern SIMPLE_AA_PATTERN = Pattern.compile("[ACDEFGHIKLMNOPQRSTUVWYX*]+");
	
	private Pattern regexPattern;
	private Boolean isSimpleMatch = null;

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
		String regex = getRegex();
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
	

	
	
	public Pattern getRegexPattern() {
		if(regexPattern == null) {
			regexPattern = buildRegexPattern();
		}
		return regexPattern;
	}
	
	private Pattern buildRegexPattern() {
		return Pattern.compile(getRegex());
	}	
	
	@Override
	public void writePropertyDirectly(String propName, Object val) {
		super.writePropertyDirectly(propName, val);
		if(propName.equals(REGEX_PROPERTY)) {
			regexPattern = null;
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
		if(getRegex() == null) {
			throw new VariationException(Code.VARIATION_REGEX_UNDEFINED, 
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

	}	

	public VariationDocument getVariationDocument(CommandContext cmdContext) {
		return new VariationDocument(getName(), 
				getRefStart(), getRefEnd(), 
				getRegexPattern(), getDescription(), getTranslationFormat(), 
				new LinkedHashSet<String>(getVariationCategoryNames(cmdContext)), false);
	}

	public List<String> getVariationCategoryNames(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(VcatMembership.VARIATION_NAME_PATH, getName());
		exp = exp.andExp(ExpressionFactory.matchExp(VcatMembership.VARIATION_FEATURE_NAME_PATH, getFeatureLoc().getFeature().getName()));
		exp = exp.andExp(ExpressionFactory.matchExp(VcatMembership.VARIATION_REFSEQ_NAME_PATH, getFeatureLoc().getReferenceSequence().getName()));
		SelectQuery query = new SelectQuery(VcatMembership.class, exp);
		query.setCacheGroups(VcatMembership.CACHE_GROUP);
		query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		
		List<VcatMembership> vcatMemberships = GlueDataObject.query(cmdContext, VcatMembership.class, query);
		
		List<String> names = vcatMemberships.stream().
			map(vcm -> vcm.getCategory().getName()).collect(Collectors.toList());
		return names;
	}

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf, GlueConfigContext glueConfigContext) {
		String regex = getRegex();
		if(regex != null) {
			indent(glueConfigBuf, indent).append("set pattern \""+regex+"\"").append("\n");
		}
		Integer refStart = getRefStart();
		if(refStart != null) {
			if(getTranslationFormat() == TranslationFormat.AMINO_ACID) {
				TIntObjectMap<LabeledCodon> refNtToLabeledCodon = getFeatureLoc().getRefNtToLabeledCodon(glueConfigContext.getCommandContext());
				indent(glueConfigBuf, indent).append("set location -c "+
						refNtToLabeledCodon.get(getRefStart()).getCodonLabel()+" "+
						refNtToLabeledCodon.get(getRefEnd()-2).getCodonLabel()).append("\n");
				
			} else {			
				indent(glueConfigBuf, indent).append("set location -n "+getRefStart()+" "+getRefEnd()).append("\n");
			}
		}
		for(VcatMembership vcm : getVcatMemberships()) {
			indent(glueConfigBuf, indent).append("add category "+vcm.getCategory().getName()).append("\n");
		}
	}

	public VariationScanResult scanProteinTranslation(CharSequence proteinTranslation) {
		TranslationFormat translationFormat = getTranslationFormat();
		boolean result;
		if(translationFormat == TranslationFormat.AMINO_ACID) {
			if(isSimpleMatch()) {
				result = charSequencesEqual(getRegex(), proteinTranslation);
			} else {
				Pattern regexPattern = getRegexPattern();
				result = regexPattern.matcher(proteinTranslation).find();
			}
		} else {
			return null;
		}
		return new VariationScanResult(this, result, !result);
	}

	private boolean charSequencesEqual(CharSequence seq1, CharSequence seq2) {
		if(seq1.length() != seq2.length()) {
			return false;
		}
		for(int i = 0; i < seq1.length(); i++) {
			if(seq1.charAt(i) != seq2.charAt(i)) {
				return false;
			}
		}
		return true;
	}
	
	
	public VariationScanResult scanNucleotides(CharSequence nucleotides) {
		TranslationFormat translationFormat = getTranslationFormat();
		boolean result;
		if(translationFormat == TranslationFormat.NUCLEOTIDE) {
			if(isSimpleMatch()) {
				result = charSequencesEqual(getRegex(), nucleotides);
			} else {
				Pattern regexPattern = getRegexPattern();
				result = regexPattern.matcher(nucleotides).find();
			}
		} else {
			return null;
		}
		return new VariationScanResult(this, result, !result);
	}
	
	public Variation clone() {
		throw new RuntimeException("Variation.clone() not supported");
	}
	
}
