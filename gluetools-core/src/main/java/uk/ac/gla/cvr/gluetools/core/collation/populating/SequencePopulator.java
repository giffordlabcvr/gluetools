package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectSetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectUnsetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext.UpdateType;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequencePopulator<P extends ModulePlugin<P>> extends ModulePlugin<P> {

	


	public static String runPropertyPopulator(PropertyPopulator propertyPopulator, String inputText) {
		String extractAndConvertResult = 
				RegexExtractorFormatter.extractAndConvert(inputText, propertyPopulator.getMainExtractor(), propertyPopulator.getValueConverters());
		if(extractAndConvertResult != null) {
			Pattern nullRegex = propertyPopulator.getNullRegex();
			if(nullRegex == null || !nullRegex.matcher(extractAndConvertResult).matches()) {
				return extractAndConvertResult;
			}
		}
		return null;
	}

	public static PropertyUpdate generatePropertyUpdate(FieldType fieldType, String customTableName, Sequence sequence, PropertyPopulator propertyPopulator, String newValueString) {
		String propertyName = propertyPopulator.getProperty();
		boolean overwriteExistingNonNull = propertyPopulator.overwriteExistingNonNull();
		boolean overwriteWithNewNull = propertyPopulator.overwriteWithNewNull();
		boolean link = false;
		if(customTableName != null) {
			link = true;
		}
		
		Object oldValue = sequence.readProperty(propertyName);
		if(!overwriteExistingNonNull) {
			if(oldValue != null) {
				return new PropertyUpdate(false, propertyName, newValueString, link);
			}
		}
		if(!overwriteWithNewNull && newValueString == null) {
			return new PropertyUpdate(false, propertyName, newValueString, link);
		}
		String oldValueString = null;
		if(oldValue != null) {
			if(link) {
				oldValueString = ((CustomTableObject) oldValue).getId();
			} else {
				oldValueString = fieldType.getFieldTranslator().objectValueToString(oldValue);
			}
		}
		if(equals(oldValueString, newValueString)) {
			return new PropertyUpdate(false, propertyName, newValueString, link);
		} else {
			return new PropertyUpdate(true, propertyName, newValueString, link);
		}
	}

	private static boolean equals(String string1, String string2) {
		if(string1 == null && string2 == null) {
			return true;
		}
		if(string1 != null && string2 == null) {
			return false;
		}
		if(string2 != null && string1 == null) {
			return false;
		}
		return(string1.equals(string2));
	}
	
	
	public static PropertyUpdate runSetFieldCommand(CommandContext cmdContext,
			PropertyPopulator propertyPopulator, String fieldValue, boolean noCommit) {
		String propertyName = propertyPopulator.getProperty();
		boolean overwriteExistingNonNull = propertyPopulator.overwriteExistingNonNull();
		boolean overwriteWithNewNull = propertyPopulator.overwriteWithNewNull();
		
		if(!overwriteExistingNonNull) {
			SequenceMode sequenceMode = SequenceModeCommand.getSequenceMode(cmdContext);
			Project project = sequenceMode.getProject();
			project.checkCustomFieldNames(ConfigurableTable.sequence.name(), Collections.singletonList(propertyName));
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, 
					Sequence.pkMap(sequenceMode.getSourceName(), sequenceMode.getSequenceID()));
			Object oldValue = sequence.readProperty(propertyName);
			if(oldValue != null) {
				return new PropertyUpdate(false, propertyName, fieldValue, false);
			}
		}
		if(!overwriteWithNewNull && fieldValue == null) {
			return new PropertyUpdate(false, propertyName, fieldValue, false);
		}
		UpdateResult updateResult;
		if(fieldValue == null) {
			updateResult = cmdContext.cmdBuilder(ConfigurableObjectUnsetFieldCommand.class)
					.set(ConfigurableObjectUnsetFieldCommand.FIELD_NAME, propertyName)
					.set(ConfigurableObjectUnsetFieldCommand.NO_COMMIT, noCommit)
					.execute();
		} else {
			updateResult = cmdContext.cmdBuilder(ConfigurableObjectSetFieldCommand.class)
					.set(ConfigurableObjectSetFieldCommand.FIELD_NAME, propertyName)
					.set(ConfigurableObjectSetFieldCommand.FIELD_VALUE, fieldValue)
					.set(ConfigurableObjectSetFieldCommand.NO_COMMIT, noCommit)
					.execute();
		}
		if(updateResult.getNumber() == 1) {
			return new PropertyUpdate(true, propertyName, fieldValue, false);
		}
		return new PropertyUpdate(false, propertyName, fieldValue, false);
	}
	
	public static class PropertyUpdate {
		private boolean updated;
		private String value;
		private String property;
		private boolean link;

		public PropertyUpdate(boolean updated, String property, String value, boolean link) {
			super();
			this.updated = updated;
			this.property = property;
			this.value = value;
			this.link = link;
		}

		public boolean updated() {
			return updated;
		}

		public String getValue() {
			return value;
		}

		public String getProperty() {
			return property;
		}

		public boolean isLink() {
			return link;
		}
		
		
		
	}

	protected void applyUpdateToDB(
			CommandContext cmdContext, Map<String, FieldType> fieldTypes, 
			Map<String, String> links, Sequence seq,
			PropertyUpdate update) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		
		if(update.isLink()) {
			String linkName = update.getProperty();
			LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project,
					ConfigurableTable.sequence.name(), linkName);
			String customTableName = links.get(linkName);
			String targetId = update.getValue();
			String targetPath = null;
			UpdateType updateType = UpdateType.UNSET;
			if(targetId != null) {
				targetPath = "custom-table-row/"+customTableName+"/"+targetId;
				updateType = UpdateType.SET;
			}
			PropertyCommandDelegate.executeLinkTargetUpdate(cmdContext, project, seq, true, 
					targetPath, linkUpdateContext, updateType);
		} else {
			String valueString = update.getValue();
			String property = update.getProperty();
			if(valueString == null) {
				PropertyCommandDelegate.executeUnsetField(cmdContext, project, ConfigurableTable.sequence.name(), seq, property, true);
			} else {
				Object fieldValue = fieldTypes.get(property).getFieldTranslator().valueFromString(valueString);
				PropertyCommandDelegate.executeSetField(cmdContext, project, ConfigurableTable.sequence.name(), seq, property, fieldValue, true);
			}
		}
	}

	protected Map<String, FieldType> getFieldTypes(CommandContext cmdContext, List<String> fieldNames) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		if(fieldNames == null) {
			fieldNames = project.getModifiableFieldNames(ConfigurableTable.sequence.name());
		}
		Map<String, FieldType> fieldTypes = new LinkedHashMap<String, FieldType>();
		for(String fieldName: fieldNames) {
			fieldTypes.put(fieldName, 
				project.getModifiableFieldType(ConfigurableTable.sequence.name(), fieldName));
		}
		return fieldTypes;
	}

	
	protected Map<String, String> getLinks(CommandContext cmdContext, List<String> propertyNames) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		Set<String> propertyNamesSet = null;
		if(propertyNames != null) {
			propertyNamesSet = new LinkedHashSet<String>(propertyNames);
		}
		Map<String, String> availableLinks = new LinkedHashMap<String, String>();
		for(Link link: project.getLinksForWhichSource(ConfigurableTable.sequence.name())) {
			String destTableName = link.getDestTableName();
			Multiplicity multiplicity = Multiplicity.valueOf(link.getMultiplicity());
			if(project.getCustomTable(destTableName) != null) {
				if(multiplicity.isToOne()) {
					String linkName = link.getSrcLinkName();
					if(propertyNamesSet == null || propertyNamesSet.contains(linkName)) {
						availableLinks.put(linkName, destTableName);
					}
				}
			}
		}
		for(Link link: project.getLinksForWhichDestination(ConfigurableTable.sequence.name())) {
			String srcTableName = link.getSrcTableName();
			Multiplicity multiplicity = Multiplicity.valueOf(link.getMultiplicity());
			if(project.getCustomTable(srcTableName) != null) {
				if(multiplicity.inverse().isToOne()) {
					String linkName = link.getDestLinkName();
					if(propertyNamesSet == null || propertyNamesSet.contains(linkName)) {
						availableLinks.put(linkName, srcTableName);
					}
				}
			}
		}
		return availableLinks;
	}

	
}
