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
package uk.ac.gla.cvr.gluetools.core.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class CommandObject extends CommandValue implements CommandArrayItem, CommandFieldValue {

	private Map<String, CommandFieldValue> fields = new LinkedHashMap<String, CommandFieldValue>();
	
	public CommandObject() {
		super();
	}

	public void accept(String objectFieldName, CommandDocumentVisitor visitor) {
		visitor.preVisitCommandObject(objectFieldName, this);
		fields.forEach((fieldName, commandFieldValue) -> {
			visitor.preVisitCommandFieldValue(fieldName, commandFieldValue);
			if(commandFieldValue instanceof CommandObject) {
				((CommandObject) commandFieldValue).accept(fieldName, visitor);
			} else if(commandFieldValue instanceof CommandArray) {
				((CommandArray) commandFieldValue).accept(fieldName, visitor);
			}
			visitor.postVisitCommandFieldValue(fieldName, commandFieldValue);
		});
		visitor.postVisitCommandObject(objectFieldName, this);
	}
	
	public CommandObject setInt(String name, int value) {
		return setSimpleProperty(name, new Integer(value), GlueTypeUtils.GlueType.Integer);
	}

	public CommandObject setBoolean(String name, boolean value) {
		return setSimpleProperty(name, new Boolean(value), GlueTypeUtils.GlueType.Boolean);
	}

	public CommandObject setDate(String name, Date value) {
		return setSimpleProperty(name, value, GlueTypeUtils.GlueType.Date);
	}

	public CommandObject setDouble(String name, double value) {
		return setSimpleProperty(name, new Double(value), GlueTypeUtils.GlueType.Double);
	}

	public CommandObject setNull(String name) {
		return setSimpleProperty(name, null, GlueTypeUtils.GlueType.Null);
	}

	public CommandObject setString(String name, String value) {
		return setSimpleProperty(name, value, GlueTypeUtils.GlueType.String);
	}

	
	public CommandObject set(String name, Object value) {
		if(value == null) {
			return setNull(name);
		} else if(value instanceof Double) {
			return setDouble(name, ((Double) value).doubleValue());
		} else if(value instanceof Integer) {
			return setInt(name, ((Integer) value).intValue());
		} else if(value instanceof Boolean) {
			return setBoolean(name, ((Boolean) value).booleanValue());
		} else if(value instanceof String) {
			return setString(name, ((String) value));
		} else if(value instanceof Date) {
			return setDate(name, ((Date) value));
		} else {
			throw new RuntimeException("Object of type: "+value.getClass().getCanonicalName()+" cannot be put in a GLUE document");
		}
	}

	
	public CommandObject setObject(String name) {
		CommandObject cmdObject = new CommandObject();
		fields.put(name, cmdObject);
		return cmdObject;
	}

	public CommandObject setObject(String name, CommandObject cmdObject) {
		fields.put(name, cmdObject);
		return cmdObject;
	}

	public CommandArray setArray(String name) {
		CommandArray cmdArray = new CommandArray();
		fields.put(name, cmdArray);
		return cmdArray;
	}

	
	private CommandObject setSimpleProperty(String name, Object value, GlueTypeUtils.GlueType type) {
		fields.put(name, new SimpleCommandValue(type, value));
		return this;
	}

	@Override
	public GlueType getGlueType() {
		return GlueType.Object;
	}

	public CommandFieldValue getFieldValue(String fieldName) {
		return fields.get(fieldName);
	}

	public CommandArray getArray(String fieldName) {
		return (CommandArray) getFieldValue(fieldName);
	}

	public CommandObject getObject(String fieldName) {
		return (CommandObject) getFieldValue(fieldName);
	}

	public Object getSimpleValue(String fieldName) {
		SimpleCommandValue simpleCommandValue = (SimpleCommandValue) getFieldValue(fieldName);
		if(simpleCommandValue != null) {
			return simpleCommandValue.getValue();
		}
		return null;
	}
	
	// this allows command objects to be correctly handled with dot-notation in freemarker.
	// see docs for BeanModel.TemplateModel get(String key)
	public final Object get(String key) {
		CommandFieldValue fieldValue = getFieldValue(key);
		if(fieldValue == null) {
			return fieldValue;
		} else if(fieldValue instanceof CommandArray) {
			List<CommandArrayItem> items = ((CommandArray) fieldValue).getItems();
			List<Object> renderedItems = new ArrayList<Object>(items.size());
			for(CommandArrayItem item: items) {
				if(item instanceof SimpleCommandValue) {
					renderedItems.add(((SimpleCommandValue) item).getValue());
				} else {
					renderedItems.add(item);
				}
			}
			return renderedItems;
		} else if(fieldValue instanceof CommandObject) {
			return ((CommandObject) fieldValue);
		} else if(fieldValue instanceof SimpleCommandValue) {
			return ((SimpleCommandValue) fieldValue).getValue();
		} else {
			throw new RuntimeException("Unknown subtype "+fieldValue.getClass().getCanonicalName()+" of CommandFieldValue");
		}
	}

	public String getString(String fieldName) {
		return (String) getSimpleValue(fieldName);
	}

	public Double getDouble(String fieldName) {
		return (Double) getSimpleValue(fieldName);
	}

	public Date getDate(String fieldName) {
		return (Date) getSimpleValue(fieldName);
	}

	public Integer getInteger(String fieldName) {
		return (Integer) getSimpleValue(fieldName);
	}

	public Boolean getBoolean(String fieldName) {
		return (Boolean) getSimpleValue(fieldName);
	}

	public Set<String> getFieldNames() {
		return fields.keySet();
	}
}
