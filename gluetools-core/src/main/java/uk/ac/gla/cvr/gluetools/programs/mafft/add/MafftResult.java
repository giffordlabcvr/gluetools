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
package uk.ac.gla.cvr.gluetools.programs.mafft.add;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;
public class MafftResult {

	private Map<String, DNASequence> resultAlignment;

	public Map<String, DNASequence> getResultAlignment() {
		return resultAlignment;
	}

	public void setResultAlignment(Map<String, DNASequence> resultAlignment) {
		this.resultAlignment = resultAlignment;
	}
	
	public static MafftResult emptyResult() {
		MafftResult mafftResult = new MafftResult();
		mafftResult.setResultAlignment(new LinkedHashMap<String, DNASequence>());
		return mafftResult;
	}

	public static MafftResult fixedResult(Map<String, DNASequence> resultAlignment) {
		MafftResult mafftResult = new MafftResult();
		mafftResult.setResultAlignment(new LinkedHashMap<String, DNASequence>(resultAlignment));
		return mafftResult;
	}
}
