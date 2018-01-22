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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;

@CommandClass( 
		commandWords={"list", "pattern-loc-codon"}, 
		docoptUsages={""},
		metaTags={},
		description="List the pattern-locations of the variation, with codon coordinates") 

public class VariationListPatternLocCodonCommand extends VariationModeCommand<BaseTableResult<PatternLocation>> {

	@Override
	public BaseTableResult<PatternLocation> execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		Variation variation = lookupVariation(cmdContext);
		TranslationFormat translationFormat = variation.getTranslationFormat();
		
		if(translationFormat != TranslationFormat.AMINO_ACID) {
			throw new VariationException(Code.VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS, 
					getRefSeqName(), getFeatureName(), getVariationName());
		}

		List<PatternLocation> patternLocs = variation.getPatternLocs();
		List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);
		TIntObjectHashMap<LabeledCodon> ntStartToCodon = new TIntObjectHashMap<LabeledCodon>();
		TIntObjectHashMap<LabeledCodon> ntEndToCodon = new TIntObjectHashMap<LabeledCodon>();
		labeledCodons.forEach(lc -> {
			ntStartToCodon.put(lc.getNtStart(), lc);
			ntEndToCodon.put(lc.getNtStart()+2, lc);
		});
		return new BaseTableResult<PatternLocation>("variationListPatternLocCodonResult", patternLocs, 
				BaseTableResult.column("ntStart", pLoc -> pLoc.getRefStart()), 
				BaseTableResult.column("ntEnd", pLoc -> pLoc.getRefEnd()), 
				BaseTableResult.column("lcStart", pLoc -> ntStartToCodon.get(pLoc.getRefStart()).getCodonLabel()), 
				BaseTableResult.column("lcEnd", pLoc -> ntEndToCodon.get(pLoc.getRefEnd()).getCodonLabel()), 
				BaseTableResult.column("pattern", pLoc -> pLoc.getPattern()));
		
	}
}
