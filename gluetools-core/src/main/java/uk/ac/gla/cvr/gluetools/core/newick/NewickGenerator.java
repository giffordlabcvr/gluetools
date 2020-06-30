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
package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface NewickGenerator extends Plugin {

	/* floating point number associated with branches */
	public default String generateBranchLength(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("length");
	}

	/* main string associated with leaf nodes */
	public default String generateLeafName(PhyloLeaf phyloLeaf) {
		return (String) phyloLeaf.ensureUserData().get("name");
	}

	/* main string associated with internal nodes. Appears after ")" */
	public default String generateInternalName(PhyloInternal phyloInternal) {
		return (String) phyloInternal.ensureUserData().get("name");
	}

	/* comment string associated with internal nodes. Appears after internal name, in [] */
	public default String generateInternalComment(PhyloInternal phyloInternal) {
		return (String) phyloInternal.ensureUserData().get("comment");
	}

	/* optional string within [] associated with branches */
	public default String generateBranchComment(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("comment");
	}

	/* optional string within {}, associated with branches */
	public default String generateBranchLabel(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("label");
	}

	public static String escapeNewickString(String input) {
		// https://en.wikipedia.org/wiki/Newick_format#The_grammar_rules
		// An unquoted string may not contain blanks, parentheses, square brackets, single_quotes, colons, semicolons, or commas. Underscore characters in unquoted strings are converted to blanks.[2]
		// A string may also be quoted by enclosing it in single quotes. Single quotes in the original string are represented as two consecutive single quote characters.[2]
		// Whitespace may appear anywhere except within an unquoted string or a Length
		// Newlines may appear anywhere except within a string or a Length.
		String escaped = input;
		if(input.matches("^.*[\\s()\\[\\]':;,].*$")) {
			escaped = "'"+input.replaceAll("\\s", " ").replaceAll("'", "''")+"'";
		};
		return escaped;
	}
	
}
