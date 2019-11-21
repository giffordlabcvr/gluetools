package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

public abstract class CategoryResultResolver implements Plugin {

	public abstract int compare(RecognitionCategoryResult recCatResult1,
					List<BlastHsp> hsps1, int totalAlignLen1, RecognitionCategoryResult recCatResult2,
					List<BlastHsp> hsps2, int totalAlignLen2);

	public static List<RecognitionCategoryResult> resolveCategoryResults(List<CategoryResultResolver> categoryResolvers,
			Map<RecognitionCategoryResult, List<BlastHsp>> categoryResultToValidHsps,
			Map<RecognitionCategoryResult, Integer> categoryResultToMaxTotalAlignLength) {
		List<RecognitionCategoryResult> finalCatResults;
		if(categoryResolvers.isEmpty() || categoryResultToValidHsps.isEmpty()) {
			finalCatResults = new ArrayList<RecognitionCategoryResult>(categoryResultToValidHsps.keySet());
		} else {
			Map<RecognitionCategoryResult, String> discarded = new LinkedHashMap<RecognitionCategoryResult, String>();
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
						Integer totalAlignLen1 = categoryResultToMaxTotalAlignLength.getOrDefault(catResult1, 0);
						RecognitionCategoryResult catResult2 = o2.getKey();
						List<BlastHsp> hsps2 = o2.getValue();
						Integer totalAlignLen2 = categoryResultToMaxTotalAlignLength.getOrDefault(catResult2, 0);
						int comp = categoryResolver.compare(catResult1, hsps1, totalAlignLen1, catResult2, hsps2, totalAlignLen2);
						if(comp < 0) {
							retained.remove(catResult1);
							discarded.put(catResult1, categoryResolver.getClass().getSimpleName());
							break;
						} else if(comp > 0) {
							retained.remove(catResult2);
							discarded.put(catResult2, categoryResolver.getClass().getSimpleName());
							break;
						}
					}
				}
			}
			finalCatResults = new ArrayList<RecognitionCategoryResult>(retained);
			discarded.forEach((recCatResult, crClass) ->{
				GlueLogger.getGlueLogger().log(Level.FINEST, "Category "+recCatResult.getCategoryId()+
						" ("+recCatResult.getDirection().name().toLowerCase()+")"+
						": discarded by category resolver of type "+crClass);
			});
		}
		return finalCatResults;
	}

	
}
