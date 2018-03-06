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
package uk.ac.gla.cvr.gluetools.core.clusterPickerRunner;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ClusterPickerException extends GlueException {

	public enum Code implements GlueErrorCode {

		CLUSTER_PICKER_CONFIG_EXCEPTION("errorTxt"), 
		CLUSTER_PICKER_DATA_EXCEPTION("errorTxt"), 
		CLUSTER_PICKER_FILE_EXCEPTION("errorTxt"),
		CLUSTER_PICKER_PROCESS_EXCEPTION("errorTxt"),
		TREE_LEAF_NOT_IN_ALIGNMENT("treeLeafName"),
		ALIGNMENT_MEMBER_NOT_IN_TREE("alignmentMember");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public ClusterPickerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ClusterPickerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
