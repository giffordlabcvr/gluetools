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
package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass(
		commandWords={"generate", "variation", "uncommon-aas"}, 
		description = "Generate variations for detecting uncommon amino acids", 
		docoptUsages={"<alignmentName> -r <acRefName> -f <featureName> [-c] (-w <whereClause> | -a)"},
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
			"-f <featureName>, --featureName <featureName>  Protein-coding feature",
			"-c, --recursive                                Include descendent members", 
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members"},
		metaTags = {CmdMeta.updatesDatabase}, 
		furtherHelp = ""
)
public class GenerateVariationUncommonAasCommand extends AbstractAnalyseAasCommand<CreateResult> {
	
	@Override
	protected CreateResult execute(CommandContext cmdContext, CommonAaAnalyser commonAaAnalyser) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(getAlignmentName()));
		alignment.getAncConstrainingRef(cmdContext, getAcRefName());

		List<CommonAminoAcids> commonAas = 
				commonAaAnalyser.commonAas(cmdContext, getAlignmentName(), getAcRefName(), getFeatureName(), getWhereClause(), getRecursive());
		
		List<Map<String,String>> variationPkMaps = commonAaAnalyser.generateVariationUncommonAas(cmdContext, commonAas);
		
		return new CreateResult(Variation.class, variationPkMaps.size());
	}

	@CompleterClass 
	public static class Completer extends AnalyseAasCompleter {
	}
	
}


