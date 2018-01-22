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

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

@CommandClass(
		commandWords={"show", "common-aas"}, 
		description = "Show the common amino-acids at different codon locations", 
		docoptUsages={"<alignmentName> -r <acRefName> -f <featureName> [-c] (-w <whereClause> | -a)"},
		docoptOptions={
			"-r <acRefName>, --acRefName <acRefName>        Ancestor-constraining ref",
			"-f <featureName>, --featureName <featureName>  Protein-coding feature",
			"-c, --recursive                                Include descendent members", 
			"-w <whereClause>, --whereClause <whereClause>  Qualify members",
		    "-a, --allMembers                               All members"},
		furtherHelp = ""
)
public class ShowCommonAasCommand extends AbstractAnalyseAasCommand<CommonAasResult> {


	
	@Override
	protected CommonAasResult execute(CommandContext cmdContext, CommonAaAnalyser commonAaAnalyser) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(getAlignmentName()));
		alignment.getAncConstrainingRef(cmdContext, getAcRefName());
		List<CommonAminoAcids> commonAas = 
				commonAaAnalyser.commonAas(cmdContext, getAlignmentName(), getAcRefName(), getFeatureName(), getWhereClause(), getRecursive());
		return new CommonAasResult(commonAas);
	}

	@CompleterClass 
	public static class Completer extends AnalyseAasCompleter {
	}
	
}


