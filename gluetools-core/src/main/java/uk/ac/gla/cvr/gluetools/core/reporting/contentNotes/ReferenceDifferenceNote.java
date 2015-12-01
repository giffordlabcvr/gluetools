package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.positionVariation.PositionVariation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationDocument;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;

/**
 * A reference difference note points out that the sequence content differs from a reference at certain locations.
 * For efficiency, these differences are encoded in a mask char sequence. In the mask, "-" represents no difference and 
 * any other character represents a difference (the value in the sequence content).
 */

public class ReferenceDifferenceNote extends SequenceContentNote {

	
	private CharSequence mask;
	private List<VariationDocument> foundVariationDocuments = new ArrayList<VariationDocument>();
	
	public ReferenceDifferenceNote(int refStart, int refEnd, CharSequence refChars, CharSequence queryChars, 
			boolean includeDifferenceSummaryNotes, 
			CommandContext cmdContext, String referenceName, String featureName, TranslationFormat translationFormat, 
			Set<String> variationRestrictions, Set<String> vcatRestrictions) {
		super(refStart, refEnd);
		init(refStart, refChars, queryChars, includeDifferenceSummaryNotes, cmdContext, referenceName, featureName, translationFormat, 
				variationRestrictions, vcatRestrictions);
	}

	@Override
	public void toDocument(ObjectBuilder sequenceDifferenceObj) {
		super.toDocument(sequenceDifferenceObj);
		sequenceDifferenceObj.set("mask", mask);
		ArrayBuilder foundVariationsArray = sequenceDifferenceObj.setArray("foundVariation");
		for(VariationDocument variationDocument: foundVariationDocuments) {
			variationDocument.toDocument(foundVariationsArray.addObject());
		}

	}

	
	private void init(int refStart, CharSequence refChars, CharSequence queryChars, boolean includeVariationDocuments, 
			CommandContext cmdContext, String referenceName, String featureName, TranslationFormat translationFormat,
			Set<String> variationRestrictions, Set<String> vcatRestrictions) {
		int refPos = refStart;
		char[] diffChars = new char[refChars.length()];
		LinkedHashSet<Integer> differencePositions = new LinkedHashSet<Integer>();
		for(int i = 0; i < refChars.length(); i++) {
			char refChar = refChars.charAt(i);
			char queryChar = queryChars.charAt(i);
			if(refChar == queryChar) {
				diffChars[i] = '-';
			} else {
				diffChars[i] = queryChar;
				differencePositions.add(refPos);
			}
			refPos++;
		}
		this.mask = new String(diffChars);
		if(includeVariationDocuments) {
			Set<String> variationNames = new LinkedHashSet<String>();
			List<Variation> variationsToScan = new ArrayList<Variation>();

			for(Integer diffPosition : differencePositions) {
				Expression exp = ExpressionFactory
					.matchExp(PositionVariation.FEATURE_NAME_PATH, featureName)
					.andExp(ExpressionFactory
							.matchExp(PositionVariation.REF_SEQ_NAME_PATH, referenceName))
					.andExp(ExpressionFactory
							.matchExp(PositionVariation.TRANSLATION_TYPE_PROPERTY, translationFormat.name()))
					.andExp(ExpressionFactory
							.matchExp(PositionVariation.POSITION_PROPERTY, diffPosition));
				if(variationRestrictions != null) {
					Expression variationExp = ExpressionFactory.expFalse();
					for(String variationName: variationRestrictions) {
						variationExp = variationExp.orExp(ExpressionFactory.matchExp(PositionVariation.VARIATION_NAME_PATH, variationName));
					}
					exp = exp.andExp(variationExp);
				}
				SelectQuery query = new SelectQuery(PositionVariation.class, exp);
				query.setCacheGroups(PositionVariation.CACHE_GROUP);
				query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
				
				query.addOrdering(new Ordering(PositionVariation.VARIATION_NAME_PATH, SortOrder.ASCENDING));
				List<PositionVariation> positionVariations = GlueDataObject.query(cmdContext, PositionVariation.class, query);
				List<Variation> variationsAtPosition = positionVariations.stream().map(PositionVariation::getVariation).collect(Collectors.toList());
				
				if(vcatRestrictions != null) {
					variationsAtPosition = variationsAtPosition.stream()
							.filter(v -> v.getVariationCategoryNames(cmdContext).stream().anyMatch(vc -> vcatRestrictions.contains(vc)))
							.collect(Collectors.toList());
				}
				
				
				variationsAtPosition = variationsAtPosition.stream().filter(v -> !variationNames.contains(v.getName())).collect(Collectors.toList());
				variationNames.addAll(variationsAtPosition.stream().map(v -> v.getName()).collect(Collectors.toList()));
				variationsToScan.addAll(variationsAtPosition);
			}
			for(Variation variation: variationsToScan) {
				if(refStart <= variation.getRefStart() && refStart+queryChars.length()-1 >= variation.getRefEnd()) {
					CharSequence input = queryChars.subSequence(variation.getRefStart()-refStart, variation.getRefEnd()-refStart+1);
					Matcher matcher = variation.getRegexPattern().matcher(input);
					boolean findResult = matcher.find();
					if(findResult) {
						foundVariationDocuments.add(variation.getVariationDocument(cmdContext));
						for(int p = variation.getRefStart(); p <= variation.getRefEnd(); p++) {
							differencePositions.remove(p);
						}
					}
				}
			}
			// add "unknown" variation documents for any differences which did not match a variation.
			for(Integer unknownPos: differencePositions) {
				char refChar = refChars.charAt(unknownPos-refStart);
				char queryChar = queryChars.charAt(unknownPos-refStart);
				
				if(queryChar == 'X') {
					continue;
				}
				
				String name = new String(new char[]{refChar})+Integer.toString(unknownPos)+new String(new char[]{queryChar});
				
				foundVariationDocuments.add(new VariationDocument(name, unknownPos, unknownPos, null, "Unknown variant", translationFormat, new LinkedHashSet<String>(), true));
			}
			Collections.sort(foundVariationDocuments, new Comparator<VariationDocument>(){
				@Override
				public int compare(VariationDocument o1, VariationDocument o2) {
					return Integer.compare(o1.getRefStart(), o2.getRefStart());
				}
			});
		}

		
	}


	public CharSequence getMask() {
		return mask;
	}

	public void setMask(CharSequence maskString) {
		this.mask = maskString;
	}

	@Override
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setMask(getMask().subSequence(length, getMask().length()));
	}

	@Override
	public void truncateRight(int length) {
		super.truncateRight(length);
		setMask(getMask().subSequence(0, getMask().length() - length));
	}

	public List<VariationDocument> getFoundVariationDocuments() {
		return foundVariationDocuments;
	}
	
	
	
	
}
