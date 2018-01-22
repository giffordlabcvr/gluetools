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
package uk.ac.gla.cvr.gluetools.core.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class GlueResourceMap {

	private static Multiton instances = new Multiton();
	
	private static Multiton.Creator<GlueResourceMap> creator = new
			Multiton.SuppliedCreator<>(GlueResourceMap.class, GlueResourceMap::new);
	
	public static GlueResourceMap getInstance() {
		return instances.get(creator);
	}
	
	private Map<String, byte[]> nameToBytes = new LinkedHashMap<String, byte[]>();
	
	public void put(String name, byte[] bytes) {
		nameToBytes.put(name, bytes);
	}
	
	public byte[] get(String name) {
		return nameToBytes.get(name);
	}
	
	private GlueResourceMap() {

	}

}
