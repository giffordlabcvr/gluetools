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
package uk.ac.gla.cvr.gluetools.core.blastRotator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.CategoryResultResolver;
import uk.ac.gla.cvr.gluetools.core.blastRecogniser.CategoryResultResolverFactory;
import uk.ac.gla.cvr.gluetools.core.blastRecogniser.RecognitionCategoryResult;
import uk.ac.gla.cvr.gluetools.core.blastRecogniser.RecognitionCategoryResult.Direction;
import uk.ac.gla.cvr.gluetools.core.blastRotator.BlastSequenceRotatorException.Code;
import uk.ac.gla.cvr.gluetools.core.blastRotator.RotationResultRow.Status;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.MultiReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="blastSequenceRotator",
		description="Identifies circular sequences requiring rotation using nucleotide BLAST against a set of ReferenceSequences")
public class BlastSequenceRotator extends ModulePlugin<BlastSequenceRotator> {

	private static final String REFERENCE_SEQUENCE = "referenceSequence";
	private static final String BLAST_RUNNER = "blastRunner";
	private static final String MINIMUM_BIT_SCORE = "minimumBitScore";
	private static final String MINIMUM_SEGMENT_LENGTH = "minimumSegmentLength";
	private static final String SINGLE_SEGMENT_ROTATION_MAX_LENGTH = "singleSegmentRotationMaxLength";
	private static final String APPLY_SINGLE_SEGMENT_ROTATION = "applySingleSegmentRotation";
	
	private BlastRunner blastRunner = new BlastRunner();
	private List<String> refSeqNames;
	private Optional<Double> minimumBitScore;
	private Integer minimumSegmentLength;
	private Integer singleSegmentRotationMaxLength;
	private Boolean applySingleSegmentRotation;
	private List<CategoryResultResolver> categoryResolvers;

	
	public BlastSequenceRotator() {
		super();
		registerModulePluginCmdClass(RotateSequenceCommand.class);
		registerModulePluginCmdClass(RotateFileCommand.class);
		registerModulePluginCmdClass(RotateFastaDocumentCommand.class);
		addSimplePropertyName(MINIMUM_BIT_SCORE);
		addSimplePropertyName(MINIMUM_SEGMENT_LENGTH);
		addSimplePropertyName(SINGLE_SEGMENT_ROTATION_MAX_LENGTH);
		addSimplePropertyName(APPLY_SINGLE_SEGMENT_ROTATION);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, BLAST_RUNNER);
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
		this.refSeqNames = PluginUtils.configureStringsProperty(configElem, REFERENCE_SEQUENCE, 1, null);
		this.minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_BIT_SCORE, false));
		this.minimumSegmentLength = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_SEGMENT_LENGTH, false)).orElse(10);
		this.applySingleSegmentRotation = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, APPLY_SINGLE_SEGMENT_ROTATION, false)).orElse(false);
		this.singleSegmentRotationMaxLength = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, SINGLE_SEGMENT_ROTATION_MAX_LENGTH, false)).orElse(100);
		
		CategoryResultResolverFactory categoryResolverFactory = PluginFactory.get(CategoryResultResolverFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(categoryResolverFactory.getElementNames());
		List<Element> categoryResolverElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		categoryResolvers = categoryResolverFactory.createFromElements(pluginConfigContext, categoryResolverElems);


	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		BlastDbManager.getInstance().removeMultiRefBlastDB(cmdContext, multiDbName());
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		for(String refSeqName: refSeqNames) {
			ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
			if(refSequence == null) {
				throw new BlastSequenceRotatorException(Code.NO_SUCH_REFERENCE_SEQUENCE, refSeqName);
			}
		}
	}

	public Map<String, RotationResultRow> rotate(CommandContext cmdContext, Map<String, DNASequence> queries) {
		Map<String, RotationResultRow> queryIdToRotationResult = new LinkedHashMap<String, RotationResultRow>();
		Set<String> refNamesSet = new LinkedHashSet<String>();
		refNamesSet.addAll(refSeqNames);
		
		MultiReferenceBlastDB multiReferenceDB = BlastDbManager.getInstance().ensureMultiReferenceDB(cmdContext, multiDbName(), refNamesSet);
		GlueLogger.getGlueLogger().finest("Executing BLAST");

		List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, BlastRunner.BlastType.BLASTN, multiReferenceDB, 
				FastaUtils.mapToFasta(queries, LineFeedStyle.forOS()));

		Map<String, BlastResult> queryIdToBlastResult = new LinkedHashMap<String, BlastResult>();
		for(BlastResult blastResult: blastResults) {
			queryIdToBlastResult.put(blastResult.getQueryFastaId(), blastResult);
		}
		
		BlastHspFilter hspFilter = initHspFilter();
		
		queries.keySet().forEach(queryId -> {
			Integer sequenceLength = queries.get(queryId).getSequenceAsString().length();
			RotationResultRow rotationResultRow;
			BlastResult blastResult = queryIdToBlastResult.get(queryId);
			if(blastResult == null || blastResult.getHits().isEmpty()) {
				rotationResultRow = new RotationResultRow(queryId, sequenceLength, Status.NO_BLAST_HITS, null);
			} else {
				Map<RecognitionCategoryResult, List<BlastHsp>> categoryResultToValidHsps = new LinkedHashMap<RecognitionCategoryResult, List<BlastHsp>>();
				Map<RecognitionCategoryResult, Integer> categoryResultToMaxTotalAlignLength = new LinkedHashMap<RecognitionCategoryResult, Integer>();
				for(String refSeqName: refSeqNames) {
					List<BlastHsp> refHsps = BlastUtils.blastResultToHsps(refSeqName, hspFilter, blastResult);
					if(refHsps.size() > 0) {
						RecognitionCategoryResult recCatResult = new RecognitionCategoryResult(refSeqName, Direction.FORWARD);
						categoryResultToValidHsps.put(recCatResult, refHsps);
						int totalAlignLen = 0;
						for(BlastHsp hsp: refHsps) {
							totalAlignLen += hsp.getAlignLen();
						}
						categoryResultToMaxTotalAlignLength.put(recCatResult, totalAlignLen);
					}
				}
				GlueLogger.getGlueLogger().finest("For query ID "+queryId+" BLAST rotator is resolving category results.");
				List<RecognitionCategoryResult> finalCatResults = CategoryResultResolver.resolveCategoryResults(this.categoryResolvers, categoryResultToValidHsps,
						categoryResultToMaxTotalAlignLength);
				if(finalCatResults.size() == 0) {
					rotationResultRow = new RotationResultRow(queryId, sequenceLength, Status.NO_BLAST_HITS, null);
				} else {
					RecognitionCategoryResult selectedCatResult = finalCatResults.get(0);
					String selectedRefName = selectedCatResult.getCategoryId();
					GlueLogger.getGlueLogger().finest("For query ID "+queryId+" BLAST rotator selected reference "+selectedRefName);
					List<BlastHsp> selectedHsps = categoryResultToValidHsps.get(selectedCatResult);
					// generate segments from each HSP, and put all these together in a List.
					List<BlastSegmentList> perHspAlignedSegments = 
							selectedHsps.stream()
							.map(hsp -> {
								BlastUtils.checkBlastHsp(hsp);
								return hsp.computeBlastAlignedSegments(1, Function.identity());
							})
							.collect(Collectors.toList());
			
					// merge/rationalise the segments;
					List<QueryAlignedSegment> qaSegs = new ArrayList<QueryAlignedSegment>(BlastUtils.mergeSegments(perHspAlignedSegments, false));
					GlueLogger.getGlueLogger().finest("For query ID "+queryId+" the segments are: "+qaSegs);
					if(qaSegs.isEmpty()) {
						rotationResultRow = new RotationResultRow(queryId, sequenceLength, Status.NO_ACCEPTABLE_HSPS, null);
					} else {
						// translate qaSegs into rotation result
						rotationResultRow = qaSegsToRotationResult(cmdContext, queryId, queries.get(queryId).getSequenceAsString(), selectedRefName, qaSegs);
					}
				}
			}
			queryIdToRotationResult.put(queryId, rotationResultRow);
		});
		return queryIdToRotationResult;
	}

	private RotationResultRow qaSegsToRotationResult(CommandContext cmdContext, String queryId, String sequenceNts, String selectedRefName, List<QueryAlignedSegment> qaSegs) {
		ReferenceSequence refSeq = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(selectedRefName));
		int sequenceLength = sequenceNts.length();
		int refLength = refSeq.getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		qaSegs.sort(new Comparator<QueryAlignedSegment>(){
			@Override
			public int compare(QueryAlignedSegment o1, QueryAlignedSegment o2) {
				int comp;
				comp = Integer.compare(o1.getQueryStart(), o2.getQueryStart());
				if(comp != 0) {return comp;}
				comp = Integer.compare(o1.getQueryEnd(), o2.getQueryEnd());
				return 0;
			}
		});

		RotationResultRow.Status status = Status.NO_ROTATION_NECESSARY;
		Integer rotationNts = null;
		
		QueryAlignedSegment lastQaSeg = null;
		
		// when a single segment is the result of the blast, try rotating the input sequence by small amounts to see if an improved
		// totalAlign length results.
		if(qaSegs.size() == 1 && this.applySingleSegmentRotation) {
			QueryAlignedSegment qaSeg = qaSegs.get(0);
			int lengthDiff = refLength - qaSeg.getCurrentLength();
			if(lengthDiff > 0 && lengthDiff <= this.singleSegmentRotationMaxLength && sequenceLength > this.singleSegmentRotationMaxLength) {
				List<Integer> rotationValues = new ArrayList<Integer>();
				int i = 1;
				while(i < this.singleSegmentRotationMaxLength) {
					int diff = i;
					if(qaSeg.getRefStart()-diff >= 1) {
						rotationValues.add(diff);
						i++;
					}
					if(qaSeg.getRefEnd()+diff <= refLength) {
						rotationValues.add(sequenceLength-diff);
						i++;
					}
					if(i == diff) {
						break;
					}
				}
				if(rotationValues.size() > 0) {
					Map<String, DNASequence> rotatedQueries = new LinkedHashMap<String, DNASequence>();
					Set<String> refNamesSet = new LinkedHashSet<String>();
					refNamesSet.add(selectedRefName);
					MultiReferenceBlastDB referenceDb = BlastDbManager.getInstance().ensureMultiReferenceDB(cmdContext, multiDbName(), refNamesSet);
					for(Integer rotationValue: rotationValues) {
						String rotatedFastaID = queryId+"_"+rotationValue.toString();
						String rotatedNts = sequenceNts.substring(sequenceLength-rotationValue) + sequenceNts.substring(0, sequenceLength-rotationValue);
						rotatedQueries.put(rotatedFastaID, new DNASequence(rotatedNts));
					}
					GlueLogger.getGlueLogger().finest("Executing BLAST for single segment rotation");
					List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, BlastRunner.BlastType.BLASTN, referenceDb, 
							FastaUtils.mapToFasta(rotatedQueries, LineFeedStyle.forOS()));
					Map<String, BlastResult> queryIdToBlastResult = new LinkedHashMap<String, BlastResult>();
					for(BlastResult blastResult: blastResults) {
						queryIdToBlastResult.put(blastResult.getQueryFastaId(), blastResult);
					}
					int bestMatchLength = qaSeg.getCurrentLength();
					BlastHspFilter hspFilter = initHspFilter();
					for(Integer rotationValue: rotationValues) {
						String rotatedFastaID = queryId+"_"+rotationValue.toString();
						BlastResult blastResult = queryIdToBlastResult.get(rotatedFastaID);
						if(blastResult != null && !blastResult.getHits().isEmpty()) {
							List<BlastHsp> refHsps = BlastUtils.blastResultToHsps(selectedRefName, hspFilter, blastResult);
							if(refHsps.size() > 0) {
								int totalAlignLen = 0;
								for(BlastHsp hsp: refHsps) {
									totalAlignLen += hsp.getAlignLen();
								}
								if(totalAlignLen > bestMatchLength) {
									bestMatchLength = totalAlignLen;
									status = Status.ROTATION_NECESSARY;
									rotationNts = rotationValue;
								}
							}
						}
					}
				}
			}
		} else {
			for(QueryAlignedSegment qaSeg: qaSegs) {
				GlueLogger.log(Level.FINEST, "Rotation processing qaSeg: "+qaSeg.toString());
				if(qaSeg.getCurrentLength() < minimumSegmentLength) {
					GlueLogger.log(Level.FINEST, "Rotation ignored short qaSeg: "+qaSeg.toString());
					continue;
				}
				if(status == Status.NO_ROTATION_NECESSARY) {
					if(lastQaSeg != null) {
						if(qaSeg.getRefStart() < lastQaSeg.getRefStart()) {
							status = Status.ROTATION_NECESSARY;
							GlueLogger.log(Level.FINEST, "Rotation identified cut point at qaSeg: "+qaSeg.toString());
							rotationNts = sequenceLength-(qaSeg.getQueryStart() - 1);
						}
					}
				} 
				lastQaSeg = qaSeg;
			}
		}
		return new RotationResultRow(queryId, sequenceLength, status, rotationNts);
	}

	private BlastHspFilter initHspFilter() {
		return new BlastHspFilter() {
			// hsp must be forward direction and meet any minimum bit score
			@Override
			public boolean allowBlastHsp(BlastHsp blastHsp) {
				boolean hspAccepted = blastHsp.getQueryTo() >= blastHsp.getQueryFrom() && // allow only forward HSPs
						blastHsp.getHitTo() >= blastHsp.getHitFrom() &&
						minimumBitScore.map(minBitScore -> (minBitScore <= blastHsp.getBitScore())).orElse(true);
				if(hspAccepted) {
					GlueLogger.log(Level.FINEST, "Rotator accepted HSP "+blastHsp);
				} else {
					GlueLogger.log(Level.FINEST, "Rotator rejected HSP "+blastHsp);
				}
				return hspAccepted;			}
		};
	}

	private String multiDbName() {
		return "blastSequenceRotator_"+getModuleName();
	}
	@SuppressWarnings("unused")
	private String singleDbName(String refName) {
		return "blastSequenceRotator_"+getModuleName()+"_"+refName;
	}
	
		
	
}
