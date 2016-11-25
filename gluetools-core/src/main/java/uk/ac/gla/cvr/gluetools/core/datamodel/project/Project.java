package uk.ac.gla.cvr.gluetools.core.datamodel.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueConfigContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.Keyword;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ModePathElement;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.PkPath;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@GlueDataClass(defaultListedProperties = {_Project.NAME_PROPERTY, _Project.DESCRIPTION_PROPERTY})
public class Project extends _Project {

	private Map<String, List<PkField>> tableNameToPkFields = new LinkedHashMap<String, List<PkField>>();
	private Map<String, String> classNameToTableName = new LinkedHashMap<String, String>();
	
	public static Map<String, String> pkMap(String name) {
		return Collections.singletonMap(NAME_PROPERTY, name);
	}
	
	@Override
	public void setPKValues(Map<String, String> idMap) {
		setName(idMap.get(NAME_PROPERTY));
	}

	public void setTablePkFields(String tableName, List<PkField> pkFields) {
		tableNameToPkFields.put(tableName, pkFields);
	}

	public List<PkField> getTablePkFields(String tableName) {
		return tableNameToPkFields.get(tableName);
	}

	public void setClassTableName(Class<? extends GlueDataObject> theClass, String tableName) {
		classNameToTableName.put(theClass.getCanonicalName(), tableName);
	}

	public CustomTable getCustomTable(String tableName) {
		return getCustomTables().stream()
				.filter(t -> t.getName().equals(tableName))
				.findFirst().orElse(null);
	}
	
	public Field getCustomField(String tableName, String fieldName) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(tableName))
				.filter(f -> f.getName().equals(fieldName))
				.findFirst().orElse(null);
	}

	public List<String> getCustomFieldNames(String tableName) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(tableName))
				.map(Field::getName)
				.collect(Collectors.toList());
	}

	public List<Field> getCustomFields(String tableName) {
		return getFields().stream()
				.filter(f -> f.getTable().equals(tableName))
				.collect(Collectors.toList());
	}

	public List<Link> getLinksForWhichSource(String tableName) {
		return getLinks().stream()
				.filter(l -> l.getSrcTableName().equals(tableName))
				.collect(Collectors.toList());
	}

	public List<Link> getLinksForWhichDestination(String tableName) {
		return getLinks().stream()
				.filter(l -> l.getDestTableName().equals(tableName))
				.collect(Collectors.toList());
	}

	public void checkTableName(String tableName) {
		CustomTable customTable = getCustomTable(tableName);
		if(customTable != null) {
			return;
		}
		try {
			Enum.valueOf(ConfigurableTable.class, tableName).getDataObjectClass();
		} catch(IllegalArgumentException iae) {
			throw new ProjectModeCommandException(Code.NO_SUCH_TABLE, tableName);
		}
	}

	public void checkCustomTableName(String tableName) {
		CustomTable customTable = getCustomTable(tableName);
		if(customTable == null) {
			throw new ProjectModeCommandException(Code.NO_SUCH_CUSTOM_TABLE, tableName);
		}
	}

	
	public List<String> getTableNames() {
		List<String> tableNames = new ArrayList<String>();
		tableNames.addAll(Arrays.asList(ConfigurableTable.values()).stream().map(v -> v.name()).collect(Collectors.toList()));
		tableNames.addAll(getCustomTables().stream().map(t -> t.getName()).collect(Collectors.toList()));
		return tableNames;
	}
	
	public Class<? extends GlueDataObject> getDataObjectClass(String tableName) {
		CustomTable customTable = getCustomTable(tableName);
		if(customTable != null) {
			return customTable.getRowClass();
		}
		try {
			return Enum.valueOf(ConfigurableTable.class, tableName).getDataObjectClass();
		} catch(IllegalArgumentException iae) {
			throw new ProjectModeCommandException(Code.NO_SUCH_TABLE, tableName);
		}
	}

	public GlueDataClass getDataClassAnnotation(String tableName) {
		Class<? extends GlueDataObject> dataObjectClass = getDataObjectClass(tableName);
		return getDataClassAnnotation(dataObjectClass);
	}

	public static GlueDataClass getDataClassAnnotation(Class<? extends GlueDataObject> dataObjectClass) {
		GlueDataClass dataClassAnnotation = dataObjectClass.getAnnotation(GlueDataClass.class);
		if(dataClassAnnotation == null) {
			// Hack: see CustomTableObject for details.
			Class<?> superclass = dataObjectClass.getSuperclass();
			return superclass.getAnnotation(GlueDataClass.class);
		}
		return dataClassAnnotation;
	}

	
	public List<String> getListableProperties(String tableName) {
		List<String> listableProperties; 
		if(tableName.equals(ConfigurableTable.alignment_member.name())) {
			listableProperties = getListableMemberFields();
		} else {
			GlueDataClass dataClassAnnotation = getDataClassAnnotation(tableName);
			listableProperties = new ArrayList<String>(Arrays.asList(dataClassAnnotation.listableBuiltInProperties()));
			listableProperties.addAll(getCustomFieldNames(tableName));
		}
		List<Link> toOneLinksForWhichSource = 
				getLinksForWhichSource(tableName).stream().filter(l -> l.isToOne()).collect(Collectors.toList());
		listableProperties
			.addAll(toOneLinksForWhichSource.stream().map(l -> l.getSrcLinkName()).collect(Collectors.toList()));

		List<Link> fromOneLinksForWhichDest = 
				getLinksForWhichDestination(tableName).stream().filter(l -> l.isFromOne()).collect(Collectors.toList());
		listableProperties
			.addAll(fromOneLinksForWhichDest.stream().map(l -> l.getDestLinkName()).collect(Collectors.toList()));

		
		return listableProperties;
	}

	public List<String> getModifiableFieldNames(String tableName) {
		GlueDataClass dataClassAnnotation = getDataClassAnnotation(tableName);
		List<String> modifiableFields = new ArrayList<String>(Arrays.asList(dataClassAnnotation.modifiableBuiltInProperties()));
		modifiableFields.addAll(getCustomFieldNames(tableName));
		return modifiableFields;
	}
	
	public FieldType getModifiableFieldType(String tableName, String fieldName) {
		Field customField = getCustomField(tableName, fieldName);
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

	public void checkListableProperties(String tableName, List<String> propertyNames) {
		List<String> listableProperties = getListableProperties(tableName);
		Set<String> validPropertyNames = new LinkedHashSet<String>(listableProperties);
		if(propertyNames != null) {
			propertyNames.forEach(f-> {
				if(!validPropertyNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, listableProperties, tableName);
				}
			});
		}
	}

	public void checkCustomFieldNames(String tableName, List<String> fieldNames) {
		List<String> validFieldNamesList = getCustomFieldNames(tableName);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, validFieldNamesList, tableName);
				}
			});
		}
	}

	public void checkModifiableFieldNames(String tableName, List<String> fieldNames) {
		List<String> validFieldNamesList = getModifiableFieldNames(tableName);
		Set<String> validFieldNames = new LinkedHashSet<String>(validFieldNamesList);
		if(fieldNames != null) {
			fieldNames.forEach(f-> {
				if(!validFieldNames.contains(f)) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INVALID_PROPERTY, f, validFieldNamesList, tableName);
				}
			});
		}
	}

	
	private List<String> getListableMemberFields() {
		GlueDataClass dataClassAnnotation = getDataClassAnnotation(ConfigurableTable.alignment_member.name());
		List<String> listableProperties = new ArrayList<String>(Arrays.asList(dataClassAnnotation.listableBuiltInProperties()));
		listableProperties.addAll(getCustomFieldNames(ConfigurableTable.alignment_member.name()));
		listableProperties.addAll(getListableProperties(ConfigurableTable.sequence.name()).stream().
			map(s -> _AlignmentMember.SEQUENCE_PROPERTY+"."+s).collect(Collectors.toList()));
		return new ArrayList<String>(listableProperties);
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
	public void checkProperty(String tableName, String propertyName, FieldType requiredFieldType, boolean requireModifiable) {
		if(requireModifiable) {
			List<String> validList = getModifiableFieldNames(tableName);
			if(!validList.contains(propertyName)) {
				throw new ProjectModeCommandException(ProjectModeCommandException.Code.NO_SUCH_MODIFIABLE_PROPERTY, 
						tableName, propertyName);
			}
			if(requiredFieldType != null) {
				FieldType fieldType = getModifiableFieldType(tableName, propertyName);
				if(fieldType != requiredFieldType) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INCORRECT_FIELD_TYPE, 
							tableName, propertyName, requiredFieldType.name(), fieldType.name());
				}
			}
		} else {
			List<String> validList = getListableProperties(tableName);
			if(!validList.contains(propertyName)) {
				throw new ProjectModeCommandException(ProjectModeCommandException.Code.NO_SUCH_PROPERTY, 
						tableName, propertyName);
			}
			if(requiredFieldType != null) {
				FieldType fieldType = getModifiableFieldType(tableName, propertyName);
				if(fieldType != requiredFieldType) {
					throw new ProjectModeCommandException(ProjectModeCommandException.Code.INCORRECT_FIELD_TYPE, 
							tableName, propertyName, requiredFieldType.name(), fieldType.name());
				}
			}
		}
	}

	public ModePathElement[] getModePathForTable(String tableName) {
		CustomTable customTable = getCustomTable(tableName);
		if(customTable != null) {
			return new ModePathElement[]{new Keyword("custom-table-row"), new Keyword(tableName), new PkPath(CustomTableObject.ID_PROPERTY)};
		} else {
			return ConfigurableTable.valueOf(tableName).getModePath();
		}
	}
	
	public String getTableNameForDataObjectClass(Class<? extends GlueDataObject> theClass) {
		return classNameToTableName.get(theClass.getCanonicalName());
	}
	
	public Link getSrcTableLink(String srcTableName, String srcLinkName) {
		return getLinksForWhichSource(srcTableName).stream()
				.filter(l -> l.getSrcLinkName().equals(srcLinkName))
				.findFirst().orElse(null);
	}

	public Link getDestTableLink(String destTableName, String destLinkName) {
		return getLinksForWhichDestination(destTableName).stream()
				.filter(l -> l.getDestLinkName().equals(destLinkName))
				.findFirst().orElse(null);
	}

	public String pkMapToTargetPath(String tableName, Map<String, String> pkMap) {
		ModePathElement[] modePath = getModePathForTable(tableName);
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < modePath.length; i++) {
			if(i > 0) {
				buf.append("/");
			}
			if(modePath[i] instanceof Keyword) {
				Keyword keyword = (Keyword) modePath[i];
				buf.append(keyword.getKeyword());
			} else if(modePath[i] instanceof PkPath) {
				PkPath pkPath = (PkPath) modePath[i];
				buf.append(pkMap.get(pkPath.getPkPath()));
			} else {
				throw new RuntimeException("Unknown type of ModePathElement "+modePath[i]);
			}
		}
		return buf.toString();
	}
	
	public Map<String, String> targetPathToPkMap(String tableName, String targetPath) {
		ModePathElement[] modePath = getModePathForTable(tableName);
		return targetPathToPkMap(tableName, modePath, targetPath);
	}

	public static Map<String, String> targetPathToPkMap(ConfigurableTable configurableTable, String targetPath) {
		return targetPathToPkMap(configurableTable.name(), configurableTable.getModePath(), targetPath);
	}
	
	
	public static boolean validTargetPath(ModePathElement[] modePath, String targetPath) {
		String[] bits = targetPath.split("/");
		if(bits.length == modePath.length) {
			for(int i = 0; i < modePath.length; i++) {
				if(modePath[i] instanceof Keyword) {
					Keyword keyword = (Keyword) modePath[i];
					if(!bits[i].equals(keyword.getKeyword())) {
						return false;
					}
				} else if(modePath[i] instanceof PkPath) {
					continue;
				} else {
					throw new RuntimeException("Unknown type of ModePathElement "+modePath[i]);
				}
			}
			return true;
		}
		return false;
	}
	
	private static Map<String, String> targetPathToPkMap(String tableName,
			ModePathElement[] modePath, String targetPath) {
		String[] bits = targetPath.split("/");
		if(bits.length == modePath.length) {
			Map<String, String> pkMap = new LinkedHashMap<String, String>();
			for(int i = 0; i < modePath.length; i++) {
				if(modePath[i] instanceof Keyword) {
					Keyword keyword = (Keyword) modePath[i];
					if(!bits[i].equals(keyword.getKeyword())) {
						throw new ProjectModeCommandException(Code.INVALID_TARGET_PATH, tableName, targetPath, correctForm(modePath));
					}
				} else if(modePath[i] instanceof PkPath) {
					pkMap.put(((PkPath) modePath[i]).getPkPath(), bits[i]);
				} else {
					throw new RuntimeException("Unknown type of ModePathElement "+modePath[i]);
				}
			}
			return pkMap;
		}
		throw new ProjectModeCommandException(Code.INVALID_TARGET_PATH, tableName, targetPath, correctForm(modePath));
	}

	
	private static String correctForm(ModePathElement[] modePath) {
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < modePath.length; i++) {
			if(i > 0) {
				buf.append("/");
			}
			ModePathElement modePathElement = modePath[i];
			buf.append(modePathElement.correctForm());
		}
		return buf.toString();
	}

	public Link getLink(String tableName, String linkName) {
		Link link = getLinksForWhichSource(tableName).stream()
				.filter(l -> l.getSrcLinkName().equals(linkName))
				.findFirst().orElse(null);
		if(link == null) {
			link = getLinksForWhichDestination(tableName).stream()
					.filter(l -> l.getDestLinkName().equals(linkName))
					.findFirst().orElse(null);
		}
		return link;
	}
	
}
