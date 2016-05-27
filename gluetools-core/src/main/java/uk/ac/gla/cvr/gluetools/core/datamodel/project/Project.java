package uk.ac.gla.cvr.gluetools.core.datamodel.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@GlueDataClass(defaultListedProperties = {_Project.NAME_PROPERTY, _Project.DESCRIPTION_PROPERTY})
public class Project extends _Project {

	public static Map<String, String> pkMap(String name) {
		return Collections.singletonMap(NAME_PROPERTY, name);
	}

	@Override
	public void setPKValues(Map<String, String> idMap) {
		setName(idMap.get(NAME_PROPERTY));
	}
	
	public Field getCustomField(ConfigurableTable cTable, String fieldName) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(cTable.name()))
				.filter(f -> f.getName().equals(fieldName))
				.findFirst().orElse(null);
	}

	public List<String> getCustomFieldNames(ConfigurableTable cTable) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(cTable.name()))
				.map(Field::getName)
				.collect(Collectors.toList());
	}

	public List<Field> getCustomFields(ConfigurableTable cTable) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(cTable.name()))
				.collect(Collectors.toList());
	}

	public List<String> getListableProperties(ConfigurableTable cTable) {
		GlueDataClass dataClassAnnotation = cTable.getDataObjectClass().getAnnotation(GlueDataClass.class);
		List<String> listableProperties = new ArrayList<String>(Arrays.asList(dataClassAnnotation.listableBuiltInProperties()));
		listableProperties.addAll(getCustomFieldNames(cTable));
		return listableProperties;
	}

	public List<String> getModifiableFieldNames(ConfigurableTable cTable) {
		GlueDataClass dataClassAnnotation = cTable.getDataObjectClass().getAnnotation(GlueDataClass.class);
		List<String> modifiableFields = new ArrayList<String>(Arrays.asList(dataClassAnnotation.modifiableBuiltInProperties()));
		modifiableFields.addAll(getCustomFieldNames(cTable));
		return modifiableFields;
	}
	
	public FieldType getModifiableFieldType(ConfigurableTable cTable, String fieldName) {
		Field customField = getCustomField(cTable, fieldName);
		if(customField != null) {
			return customField.getFieldType();
		}
		// assume built in modifiable fields are of string type.
		return FieldType.VARCHAR;
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}

	public void checkListableProperties(ConfigurableTable cTable, List<String> propertyNames) {
		List<String> listableProperties = getListableProperties(cTable);
		Set<String> validPropertyNames = new LinkedHashSet<String>(listableProperties);
		if(propertyNames != null) {
			propertyNames.forEach(f-> {
				if(!validPropertyNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, listableProperties, cTable.name());
				}
			});
		}
	}

	public void checkCustomFieldNames(ConfigurableTable cTable, List<String> fieldNames) {
		List<String> validFieldNamesList = getCustomFieldNames(cTable);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, validFieldNamesList, cTable.name());
				}
			});
		}
	}

	public void checkModifiableFieldNames(ConfigurableTable cTable, List<String> fieldNames) {
		List<String> validFieldNamesList = getModifiableFieldNames(cTable);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, validFieldNamesList, cTable.name());
				}
			});
		}
	}

	
	public void checkListableMemberField(List<String> fieldNames) {
		List<String> validMemberFieldsList = getListableMemberFields();
		Set<String> validMemberFields = new LinkedHashSet<String>(validMemberFieldsList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validMemberFields.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, validMemberFieldsList, "member");
				}
			});
		}

	}

	public List<String> getListableMemberFields() {
		List<String> validMemberFieldsList = getListableProperties(ConfigurableTable.sequence).stream().
			map(s -> _AlignmentMember.SEQUENCE_PROPERTY+"."+s).collect(Collectors.toList());
		return validMemberFieldsList;
	}

	@Override
	public void generateGlueConfig(int indent, StringBuffer glueConfigBuf,
			GlueConfigContext glueConfigContext) {
		if(glueConfigContext.getIncludeVariations()) {
			List<ReferenceSequence> refSequences = GlueDataObject.query(glueConfigContext.getCommandContext(), ReferenceSequence.class, new SelectQuery(ReferenceSequence.class));
			for(ReferenceSequence refSequence: refSequences) {
				StringBuffer refSeqConfigBuf = new StringBuffer();
				refSequence.generateGlueConfig(indent+INDENT, refSeqConfigBuf, glueConfigContext);
				if(refSeqConfigBuf.length() > 0) {
					indent(glueConfigBuf, indent).append("reference "+refSequence.getName()).append("\n");
					glueConfigBuf.append(refSeqConfigBuf.toString());
					indent(glueConfigBuf, indent+INDENT).append("exit").append("\n");
				}
			}
		}
	}

	/**
	 * Check that a property exists, is of the correct type, and (optionally) is modifiable.
	 */
	public void checkProperty(ConfigurableTable cTable, String propertyName, FieldType requiredFieldType, boolean requireModifiable) {
		if(requireModifiable) {
			List<String> validList = getModifiableFieldNames(cTable);
			if(!validList.contains(propertyName)) {
				throw new ProjectModeCommandException(ProjectModeCommandException.Code.NO_SUCH_MODIFIABLE_PROPERTY, 
						cTable.name(), propertyName);
			}
			if(requiredFieldType != null) {
				FieldType fieldType = getModifiableFieldType(cTable, propertyName);
				if(fieldType != requiredFieldType) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INCORRECT_FIELD_TYPE, 
							cTable.name(), propertyName, requiredFieldType.name(), fieldType.name());
				}
			}
		} else {
			List<String> validList = getListableProperties(cTable);
			if(!validList.contains(propertyName)) {
				throw new ProjectModeCommandException(ProjectModeCommandException.Code.NO_SUCH_PROPERTY, 
						cTable.name(), propertyName);
			}
			if(requiredFieldType != null) {
				FieldType fieldType = getModifiableFieldType(cTable, propertyName);
				if(fieldType != requiredFieldType) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INCORRECT_FIELD_TYPE, 
							cTable.name(), propertyName, requiredFieldType.name(), fieldType.name());
				}
			}
		}
	}
}
