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
package uk.ac.gla.cvr.gluetools.core.datamodel.link;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;

@GlueDataClass(defaultListedProperties = {_Link.SRC_TABLE_NAME_PROPERTY, _Link.SRC_LINK_NAME_PROPERTY, _Link.DEST_TABLE_NAME_PROPERTY, _Link.DEST_LINK_NAME_PROPERTY, _Link.MULTIPLICITY_PROPERTY})
public class Link extends _Link {

	public enum Multiplicity {
		ONE_TO_ONE {
			@Override
			public Multiplicity inverse() {
				return ONE_TO_ONE;
			}
			@Override
			public boolean isToOne() {
				return true;
			}
			@Override
			public boolean isToMany() {
				return false;
			}
		}, 
		ONE_TO_MANY {
			@Override
			public Multiplicity inverse() {
				return MANY_TO_ONE;
			}
			@Override
			public boolean isToOne() {
				return false;
			}
			@Override
			public boolean isToMany() {
				return true;
			}
		}, 
		MANY_TO_ONE {
			@Override
			public Multiplicity inverse() {
				return ONE_TO_MANY;
			}
			@Override
			public boolean isToOne() {
				return true;
			}
			@Override
			public boolean isToMany() {
				return false;
			}
		};
		
		public abstract Multiplicity inverse();

		public abstract boolean isToOne();

		public abstract boolean isToMany();
	}
	
	public static Map<String, String> pkMap(String projectName, String table, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PROPERTY+"."+_Project.NAME_PROPERTY, projectName);
		idMap.put(SRC_TABLE_NAME_PROPERTY, table);
		idMap.put(SRC_LINK_NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setSrcLinkName(pkMap.get(SRC_LINK_NAME_PROPERTY));
		setSrcTableName(pkMap.get(SRC_TABLE_NAME_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getProject().getName(), getSrcTableName(), getSrcLinkName());
	}

	public boolean isToOne() {
		Multiplicity multEnum = Multiplicity.valueOf(getMultiplicity());
		return multEnum.isToOne();
	}

	public boolean isToMany() {
		Multiplicity multEnum = Multiplicity.valueOf(getMultiplicity());
		return multEnum.isToMany();
	}

	public boolean isFromOne() {
		Multiplicity multEnum = Multiplicity.valueOf(getMultiplicity());
		return multEnum.inverse().isToOne();
	}

	public boolean isFromMany() {
		Multiplicity multEnum = Multiplicity.valueOf(getMultiplicity());
		return multEnum.inverse().isToMany();
	}

}
