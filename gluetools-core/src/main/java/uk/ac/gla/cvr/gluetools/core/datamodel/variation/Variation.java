package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

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
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationUtils;

@GlueDataClass(defaultListColumns = {_Variation.NAME_PROPERTY, Variation.TRANSCRIPTION_TYPE_PROPERTY, Variation.REGEX_PROPERTY, _Variation.DESCRIPTION_PROPERTY})
public class Variation extends _Variation {

	private Pattern regexPattern;

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
		return TranslationUtils.transcriptionFormatFromString(getTranscriptionType());
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
		TranslationFormat transcriptionFormat = getTranslationFormat();
		if(transcriptionFormat == TranslationFormat.NUCLEOTIDE) {
			if(!ReferenceSegment.covers(featureLocSegments, 
					Collections.singletonList(new ReferenceSegment(refStart, refEnd)))) {
				throw new VariationException(Code.VARIATION_LOCATION_OUT_OF_RANGE, 
						refSeq.getName(), feature.getName(), getName(), 
						Integer.toString(refStart), Integer.toString(refEnd));
			}
		} else if(transcriptionFormat == TranslationFormat.AMINO_ACID) {
			Feature orfAncestor = feature.getOrfAncestor();
			if(orfAncestor == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_MUST_BE_DEFINED_IN_ORF, 
						refSeq.getName(), feature.getName(), getName(), transcriptionFormat.name());
			}
			FeatureLocation codonNumberingAncestorLocation = featureLoc.getCodonNumberingAncestorLocation(cmdContext);
			if(codonNumberingAncestorLocation == null) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR, 
						refSeq.getName(), feature.getName(), getName(), transcriptionFormat.name());
			}
			Integer maxCodonNumber = codonNumberingAncestorLocation.getOwnCodonNumberingMax(cmdContext);
			if(refStart < 1 || refEnd > maxCodonNumber) {
				throw new VariationException(Code.AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE, 
						refSeq.getName(), feature.getName(), getName(), 
						codonNumberingAncestorLocation.getFeature().getName(),
						Integer.toString(refStart), Integer.toString(refEnd), maxCodonNumber);
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
			indent(glueConfigBuf, indent).append("set location "+getRefStart()+" "+getRefEnd()).append("\n");
		}
		for(VcatMembership vcm : getVcatMemberships()) {
			indent(glueConfigBuf, indent).append("add category "+vcm.getCategory().getName()).append("\n");
		}
	}
	
}
