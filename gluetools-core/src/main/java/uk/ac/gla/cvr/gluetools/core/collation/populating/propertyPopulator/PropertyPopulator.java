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
package uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator;

import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulatorException.Code;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator.PropertyUpdate;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext.UpdateType;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link.Multiplicity;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;


public interface PropertyPopulator {

	public default String getProperty() {
		return null;
	}
	
	public default boolean overwriteExistingNonNull() {
		return true;
	}

	public default boolean overwriteWithNewNull() {
		return false;
	}
	
	
	public static PropertyPathInfo analysePropertyPath(Project project, String startTable, String propertyPath) {
		String[] pathBits = propertyPath.split("\\.");
		String currentTable = startTable;
		StringBuffer modifiedObjectPathBuf = new StringBuffer();
		for(int i = 0; i < pathBits.length - 1; i++) {
			if(i > 0) {
				modifiedObjectPathBuf.append(".");
			}
			modifiedObjectPathBuf.append(pathBits[i]);
			Link link = project.getLink(currentTable, pathBits[i]);
			if(link == null) {
				throw new PropertyPopulatorException(Code.INVALID_PATH, startTable, propertyPath,  
						"There is no relational link named '"+pathBits[i]+"' from table "+currentTable);
			}
			Multiplicity multiplicity = Multiplicity.valueOf(link.getMultiplicity());
			if(currentTable.equals(link.getDestTableName()) && pathBits[i].equals(link.getDestLinkName())) {
				currentTable = link.getSrcTableName();
				if(!multiplicity.inverse().isToOne()) {
					throw new PropertyPopulatorException(Code.INVALID_PATH, startTable, propertyPath,  
							"Relational link '"+pathBits[i]+"' from table "+currentTable+" has a to-many multiplicity");
				}
			} else {
				currentTable = link.getDestTableName();
				if(!multiplicity.isToOne()) {
					throw new PropertyPopulatorException(Code.INVALID_PATH, startTable, propertyPath,  
							"Relational link '"+pathBits[i]+"' from table "+currentTable+" has a to-many multiplicity");
				}
			}
		}
		String finalPathBit = pathBits[pathBits.length - 1];
		
		PropertyPathInfo propertyPathInfo = new PropertyPathInfo();
		propertyPathInfo.setPropertyPath(propertyPath);
		propertyPathInfo.setProperty(finalPathBit);
		if(pathBits.length > 1) {
			propertyPathInfo.setModifiedObjectPath(modifiedObjectPathBuf.toString());
		}
		propertyPathInfo.setModifiedObjectTable(currentTable);
		
		if(project.getModifiableFieldNames(currentTable).contains(finalPathBit)) {
			propertyPathInfo.setFieldType(project.getModifiableFieldType(currentTable, finalPathBit));
		} else {
			Link link = project.getLink(currentTable, finalPathBit);
			if(link != null) {
				propertyPathInfo.setLink(true);
				if(currentTable.equals(link.getDestTableName()) && finalPathBit.equals(link.getDestLinkName())) {
					propertyPathInfo.setLinkTargetTable(link.getSrcTableName());
				} else {
					propertyPathInfo.setLinkTargetTable(link.getDestTableName());
				}
			} else {
				throw new PropertyPopulatorException(Code.INVALID_PATH, startTable, propertyPath,  
						"No link or field is named '"+finalPathBit+"' in table "+currentTable);
			}
		}
		return propertyPathInfo;
	}
	
	public static class PropertyPathInfo {
		// full propertyPath
		private String propertyPath;
		// propertyPath minus the final bit (null if the property path only has one element).
		private String modifiedObjectPath;
		// final bit of the property path
		private String property;
		// table to which the final bit applies
		private String modifiedObjectTable;
		// if update is to a field, the type of that field
		private FieldType fieldType;
		// if update is to a link
		private boolean isLink;
		// if update is to a link, the target table for that link
		private String linkTargetTable;

		private PropertyPathInfo() {
		}

		private void setPropertyPath(String propertyPath) {
			this.propertyPath = propertyPath;
		}

		private void setProperty(String property) {
			this.property = property;
		}

		private void setModifiedObjectTable(String modifiedObjectTable) {
			this.modifiedObjectTable = modifiedObjectTable;
		}

		private void setFieldType(FieldType fieldType) {
			this.fieldType = fieldType;
		}

		private void setLink(boolean isLink) {
			this.isLink = isLink;
		}

		private void setLinkTargetTable(String linkTargetTable) {
			this.linkTargetTable = linkTargetTable;
		}

		public String getPropertyPath() {
			return propertyPath;
		}

		public String getProperty() {
			return property;
		}

		public String getModifiedObjectTable() {
			return modifiedObjectTable;
		}

		public FieldType getFieldType() {
			return fieldType;
		}

		public boolean isLink() {
			return isLink;
		}

		public String getLinkTargetTable() {
			return linkTargetTable;
		}

		public String getModifiedObjectPath() {
			return modifiedObjectPath;
		}

		private void setModifiedObjectPath(String modifiedObjectPath) {
			this.modifiedObjectPath = modifiedObjectPath;
		}

		
		
	}

	public static PropertyUpdate generatePropertyUpdate(PropertyPathInfo propertyPathInfo, 
			Sequence sequence, PropertyPopulator propertyPopulator, String newValueString) {
		boolean overwriteExistingNonNull = propertyPopulator.overwriteExistingNonNull();
		boolean overwriteWithNewNull = propertyPopulator.overwriteWithNewNull();
		
		String propertyPath = propertyPathInfo.getPropertyPath();
		Object oldValue = sequence.readNestedProperty(propertyPath);
		if(!overwriteExistingNonNull) {
			if(oldValue != null) {
				// existing non null, do not update.
				return new PropertyUpdate(false, propertyPathInfo, newValueString);
			}
		}
		if(!overwriteWithNewNull && newValueString == null) {
			// new value is null, do not update.
			return new PropertyUpdate(false, propertyPathInfo, newValueString);
		}
		String oldValueString = null;
		if(oldValue != null) {
			if(propertyPathInfo.isLink()) {
				oldValueString = ((CustomTableObject) oldValue).getId();
			} else {
				oldValueString = propertyPathInfo.getFieldType().getFieldTranslator().objectValueToString(oldValue);
			}
		}
		if(equals(oldValueString, newValueString)) {
			return new PropertyUpdate(false, propertyPathInfo, newValueString);
		} else {
			return new PropertyUpdate(true, propertyPathInfo, newValueString);
		}
	}

	
	public static void applyUpdateToDB(CommandContext cmdContext, Sequence seq, PropertyUpdate update) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		
		GlueDataObject modifiedObject = seq;
		PropertyPathInfo propertyPathInfo = update.getPropertyPathInfo();
		String modifiedObjectPath = propertyPathInfo.getModifiedObjectPath();
		if(modifiedObjectPath != null) {
			String[] pathBits = modifiedObjectPath.split("\\.");
			StringBuffer pathSoFar = new StringBuffer();
			for(int i = 0; i < pathBits.length; i++) {
				if(i > 0) {
					pathSoFar.append(".");
				}
				modifiedObject = (GlueDataObject) modifiedObject.readProperty(pathBits[i]);
				if(modifiedObject == null) {
					throw new PropertyPopulatorException(Code.NULL_LINK_TARGET, seq.getSource().getName(), seq.getSequenceID(), pathSoFar.toString());
				}
			}
		}
		
		if(propertyPathInfo.isLink) {
			String linkName = propertyPathInfo.getProperty();
			LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project,
					propertyPathInfo.modifiedObjectTable, linkName);
			String linkTargetTable = propertyPathInfo.linkTargetTable;
			String targetId = update.getValue();
			String targetPath = null;
			UpdateType updateType = UpdateType.UNSET;
			if(targetId != null) {
				targetPath = "custom-table-row/"+linkTargetTable+"/"+targetId;
				updateType = UpdateType.SET;
			}
			PropertyCommandDelegate.executeLinkTargetUpdate(cmdContext, project, modifiedObject, true, 
					targetPath, linkUpdateContext, updateType);
		} else {
			String valueString = update.getValue();
			String fieldName = propertyPathInfo.getProperty();
			if(valueString == null) {
				PropertyCommandDelegate.executeUnsetField(cmdContext, project, propertyPathInfo.modifiedObjectTable, modifiedObject, fieldName, true);
			} else {
				Object fieldValue = propertyPathInfo.getFieldType().getFieldTranslator().valueFromString(valueString);
				PropertyCommandDelegate.executeSetField(cmdContext, project, propertyPathInfo.modifiedObjectTable, modifiedObject, fieldName, fieldValue, true);
			}
		}
	}
	
	static boolean equals(String string1, String string2) {
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
	
	

	
	
	
}
