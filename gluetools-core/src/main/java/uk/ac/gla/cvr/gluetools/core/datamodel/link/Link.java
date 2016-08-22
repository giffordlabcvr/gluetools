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
		}, 
		ONE_TO_MANY {
			@Override
			public Multiplicity inverse() {
				return MANY_TO_ONE;
			}
		}, 
		MANY_TO_ONE {
			@Override
			public Multiplicity inverse() {
				return ONE_TO_MANY;
			}
		};
		
		public abstract Multiplicity inverse();
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

}
