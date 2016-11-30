package uk.ac.gla.cvr.gluetools.core.document;

import java.util.Date;
import java.util.LinkedHashMap;
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
		} else {
			throw new RuntimeException("Object of type: "+value.getClass().getCanonicalName()+" cannot be put in a GLUE document");
		}
	}

	
	public CommandObject setObject(String name) {
		CommandObject cmdObject = new CommandObject();
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
		return ((SimpleCommandValue) getFieldValue(fieldName)).getValue();
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
