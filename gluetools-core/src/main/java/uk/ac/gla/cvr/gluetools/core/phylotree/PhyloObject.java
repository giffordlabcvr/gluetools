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
package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class PhyloObject<C extends PhyloObject<?>> {

	private Map<String, Object> userData;
	
	public Map<String, Object> getUserData() {
		return userData;
	}

	public void setUserData(Map<String, Object> userData) {
		this.userData = userData;
	}

	public Map<String, Object> ensureUserData() {
		if(this.userData == null) {
			this.userData = new LinkedHashMap<String, Object>();
		}
		return this.userData;
	}

	public abstract C clone();
	
	protected void copyPropertiesTo(C other) {
		Map<String, Object> userData = getUserData();
		if(userData != null) {
			other.setUserData(new LinkedHashMap<String, Object>(userData));
		}
	}
	
	public String toString() {
		return this.getClass().getSimpleName()+ensureUserData().toString();
	}

}
