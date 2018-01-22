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

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class CommonAasResult extends BaseTableResult<CommonAminoAcids> {

	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";
	public static final String CODON_LABEL = "codon";
	public static final String COMMON_AAS = "commonAAs";
	
	public CommonAasResult(List<CommonAminoAcids> rowData) {
		super("commonAas", rowData,
				column(REFERENCE_NAME, aap -> aap.getRefName()),
				column(FEATURE_NAME, aap -> aap.getFeatureName()),
				column(CODON_LABEL, aap -> aap.getCodonLabel()),
				column(COMMON_AAS, aap -> String.join("/", aap.getCommonAas())));
	}

}
