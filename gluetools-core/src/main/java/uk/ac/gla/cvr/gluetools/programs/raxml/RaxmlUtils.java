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
package uk.ac.gla.cvr.gluetools.programs.raxml;

public class RaxmlUtils {

	public static String 
		RAXMLHPC_EXECUTABLE_PROPERTY = "gluetools.core.programs.raxml.raxmlhpc.executable"; 

	public static String 
		RAXML_TEMP_DIR_PROPERTY = "gluetools.core.programs.raxml.temp.dir";

	public static String 
		RAXMLHPC_NUMBER_CPUS = "gluetools.core.programs.raxml.raxmlhpc.cpus";

	
	public static boolean validRaxmlName(String string) {
		if(string.length() < 1 || string.length() > 256) {
			return false;
		}
		if(!string.matches("[^ \t\n\r\f:\\(\\)\\[\\]]+")) {
			return false;
		}
		return true;
	}

}
