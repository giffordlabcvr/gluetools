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
package uk.ac.gla.cvr.gluetools.core.blastRecogniser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.BlastSequenceRecogniserException.Code;
import uk.ac.gla.cvr.gluetools.core.blastRecogniser.RecognitionCategoryResult.Direction;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHit;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.MultiReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="blastSequenceRecogniser",
		description="Classifies sequences based on a nucleotide BLAST against a set of ReferenceSequences")
public class BlastSequenceRecogniser extends ModulePlugin<BlastSequenceRecogniser> {

	



	private static final String REFERENCE_SEQUENCE = "referenceSequence";
	private static final String RECOGNITION_CATEGORY = "recognitionCategory";
	private static final String BLAST_RUNNER = "blastRunner";
	
	private BlastRunner blastRunner = new BlastRunner();
	private List<String> refSeqNames;
	private List<RecognitionCategory> recognitionCategories;
	// multiple categories may be returned for each sequence.
	// alternate categories are resolved using the resolvers in turn, with earlier resolvers
	// taking precedence. If a category resolver prefers one category result over another, the non-preferred result is discarded
	private List<CategoryResultResolver> categoryResolvers;
	
	public BlastSequenceRecogniser() {
		super();
		registerModulePluginCmdClass(RecogniseFileCommand.class);
		registerModulePluginCmdClass(RecogniseSequenceCommand.class);
		registerModulePluginCmdClass(RecogniseFastaDocumentCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, BLAST_RUNNER);
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
		this.recognitionCategories = PluginFactory.createPlugins(pluginConfigContext, RecognitionCategory.class, 
				PluginUtils.findConfigElements(configElem, RECOGNITION_CATEGORY));
		this.refSeqNames = PluginUtils.configureStringsProperty(configElem, REFERENCE_SEQUENCE, 1, null);

		CategoryResultResolverFactory categoryResolverFactory = PluginFactory.get(CategoryResultResolverFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(categoryResolverFactory.getElementNames());
		List<Element> categoryResolverElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		categoryResolvers = categoryResolverFactory.createFromElements(pluginConfigContext, categoryResolverElems);
	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		BlastDbManager.getInstance().removeMultiRefBlastDB(cmdContext, dbName());
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		Set<String> refSeqNamesSet = new LinkedHashSet<String>(refSeqNames);
		for(String refSeqName: refSeqNamesSet) {
			ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName), true);
			if(refSequence == null) {
				throw new BlastSequenceRecogniserException(Code.NO_SUCH_REFERENCE_SEQUENCE, refSeqName);
			}
		}
		Set<String> unusedRefs = new LinkedHashSet<String>(refSeqNamesSet);
		Map<String, String> refToCategory = new LinkedHashMap<String, String>();
		for(RecognitionCategory recognitionCategory: recognitionCategories) {
			String recognitionCategoryId = recognitionCategory.getId();
			List<String> categoryUsedRefs = recognitionCategory.getRefSeqNames();
			for(String categoryUsedRef: categoryUsedRefs) {
				if(!refSeqNamesSet.contains(categoryUsedRef)) {
					throw new BlastSequenceRecogniserException(Code.CATEGORY_USES_UNKNOWN_REFERENCE, recognitionCategoryId, categoryUsedRef);
				}
				String alreadyUsingCategory = refToCategory.get(categoryUsedRef);
				if(alreadyUsingCategory != null) {
					throw new BlastSequenceRecogniserException(Code.CATEGORY_REFERENCES_OVERLAP, recognitionCategoryId, alreadyUsingCategory, categoryUsedRef);
				}
				refToCategory.put(categoryUsedRef, recognitionCategoryId);
				unusedRefs.remove(categoryUsedRef);
			}
		}
		if(!unusedRefs.isEmpty()) {
			throw new BlastSequenceRecogniserException(Code.NO_CATEGORY_FOR_REFERENCE, unusedRefs.iterator().next());
		}
	}

	public Map<String, List<RecognitionCategoryResult>> recognise(CommandContext cmdContext, Map<String, DNASequence> queries) {
		LinkedHashSet<String> refNamesSet = new LinkedHashSet<String>(refSeqNames);
		Map<String, List<RecognitionCategoryResult>> queryIdToCategoryResults = new LinkedHashMap<String, List<RecognitionCategoryResult>>();
		MultiReferenceBlastDB multiReferenceDB = BlastDbManager.getInstance().ensureMultiReferenceDB(cmdContext, dbName(), refNamesSet);
		GlueLogger.getGlueLogger().finest("Executing BLAST");

		List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, BlastRunner.BlastType.BLASTN, multiReferenceDB, 
				FastaUtils.mapToFasta(queries, LineFeedStyle.forOS()));

		Map<String, RecognitionCategory> refToCategory = new LinkedHashMap<String, RecognitionCategory>();
		for(RecognitionCategory recognitionCategory: recognitionCategories) {
			List<String> categoryUsedRefs = recognitionCategory.getRefSeqNames();
			for(String categoryUsedRef: categoryUsedRefs) {
				refToCategory.put(categoryUsedRef, recognitionCategory);
			}
		}

		for(BlastResult blastResult: blastResults) {
			String queryId = blastResult.getQueryFastaId();
			GlueLogger.getGlueLogger().finest("Applying recognition categories for "+queryId);
			Map<RecognitionCategoryResult, List<BlastHsp>> categoryResultToValidHsps = new LinkedHashMap<RecognitionCategoryResult, List<BlastHsp>>();
			List<BlastHit> hits = blastResult.getHits();
			for(BlastHit hit: hits) {
				String referenceName = hit.getReferenceName();
				RecognitionCategory category = refToCategory.get(referenceName);
				for(Direction direction: new Direction[]{Direction.FORWARD, Direction.REVERSE}) {
					RecognitionCategoryResult recCatResult = new RecognitionCategoryResult(category.getId(), direction);
					BlastHspFilter hspFilter = direction == Direction.FORWARD ? category.getForwardHspFilter() : category.getReverseHspFilter();
					// HSPs get filtered out by various thresholds defined on the rec category.
					List<BlastHsp> hsps = hit.getHsps().stream().filter(hspFilter::allowBlastHsp).collect(Collectors.toList());
					if(!hsps.isEmpty()) {
						int totalAlignLen = 0;
						for(BlastHsp hsp: hsps) {
							log(Level.FINEST, "Category "+recCatResult.getCategoryId()+
									" ("+direction.name().toLowerCase()+")"+
									": allowed HSP on query ["+hsp.getQueryFrom()+", "+
									hsp.getQueryTo()+"] with identity: "+
									(hsp.getIdentity()/ (double) hsp.getAlignLen())*100.0+"%, score: "+
									hsp.getScore()+", bit score: "+hsp.getBitScore());
							totalAlignLen += hsp.getAlignLen();
						}
						// rec category may also define a minimum total align length of the hsps which pass the filter.
						if(totalAlignLen >= category.getMinimumTotalAlignLength()) {
							List<BlastHsp> validHsps = categoryResultToValidHsps.get(recCatResult);
							if(validHsps == null) {
								validHsps = new ArrayList<BlastHsp>();
								categoryResultToValidHsps.put(recCatResult, validHsps);
							}
							validHsps.addAll(hsps);
						}
					}
				}
			}
			List<RecognitionCategoryResult> finalCatResults;
			if(this.categoryResolvers.isEmpty() || categoryResultToValidHsps.isEmpty()) {
				finalCatResults = new ArrayList<RecognitionCategoryResult>(categoryResultToValidHsps.keySet());
			} else {
				Set<RecognitionCategoryResult> discarded = new LinkedHashSet<RecognitionCategoryResult>();
				Set<RecognitionCategoryResult> retained = new LinkedHashSet<RecognitionCategoryResult>(categoryResultToValidHsps.keySet());
				List<Entry<RecognitionCategoryResult, List<BlastHsp>>> entryList = 
						new ArrayList<Entry<RecognitionCategoryResult, List<BlastHsp>>>(categoryResultToValidHsps.entrySet());
				for(int i = 0; i < entryList.size()-1; i++) {
					Entry<RecognitionCategoryResult, List<BlastHsp>> o1 = entryList.get(i);
					for(int j = i+1; j < entryList.size(); j++) {
						Entry<RecognitionCategoryResult, List<BlastHsp>> o2 = entryList.get(j);
						for(CategoryResultResolver categoryResolver: categoryResolvers) {
							RecognitionCategoryResult catResult1 = o1.getKey();
							List<BlastHsp> hsps1 = o1.getValue();
							RecognitionCategoryResult catResult2 = o2.getKey();
							List<BlastHsp> hsps2 = o2.getValue();
							int comp = categoryResolver.compare(catResult1, hsps1, catResult2, hsps2);
							if(comp == -1) {
								retained.remove(catResult1);
								discarded.add(catResult1);
								break;
							} else if(comp == 1) {
								retained.remove(catResult2);
								discarded.add(catResult2);
								break;
							}
						}
					}
				}
				finalCatResults = new ArrayList<RecognitionCategoryResult>(retained);
				discarded.forEach(recCatResult ->{
					log(Level.FINEST, "Category "+recCatResult.getCategoryId()+
							" ("+recCatResult.getDirection().name().toLowerCase()+")"+
							": discarded by category resolvers");

				});
			}
			queryIdToCategoryResults.put(queryId, finalCatResults);
		}
		
		return queryIdToCategoryResults;
	}

	
	
	private String dbName() {
		return "blastSequenceRecogniser_"+getModuleName();
	}
	
		
	
}
