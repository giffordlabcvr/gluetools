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
package uk.ac.gla.cvr.gluetools.core.document.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentException.Code;

public class PojoDocumentUtils {

	public static CommandDocument pojoToCommandDocument(Object pojo) {
		CommandDocument commandDocument = new CommandDocument(propertyNameForClass(pojo.getClass()));
		setPojoProperties(commandDocument, pojo);
		return commandDocument;
	}
	
	public static <P> P commandObjectToPojo(CommandObject commandObject, Class<P> pojoClass) {
		P pojo = null;
		try {
			pojo = pojoClass.newInstance();
		} catch(Exception e) {
			throw new PojoDocumentException(e, Code.POJO_CREATION_FAILED, pojoClass.getCanonicalName(), e.getMessage());
		}
		Field[] fields = pojoClass.getFields();
		for(Field field: fields) {
			PojoDocumentField pojoDocumentFieldAnno = field.getAnnotation(PojoDocumentField.class);
			if(pojoDocumentFieldAnno != null) {
				checkModifiers(field, pojo.getClass());
				String fieldName = pojoDocumentFieldAnno.fieldName();
				if(fieldName.length() == 0) {
					fieldName = field.getName();
				}
				Class<?> fieldType = field.getType();
				CommandFieldValue cmdFieldValue = commandObject.getFieldValue(fieldName);
				Object pojoFieldValue;
				if(cmdFieldValue instanceof SimpleCommandValue) {
					pojoFieldValue = ((SimpleCommandValue) cmdFieldValue).getValue();
					Class<? extends Object> valueClass = pojoFieldValue.getClass();
					if(valueClass.equals(Integer.class) && fieldType.equals(Double.class)) {
						// this can happen because of how JSON / JavaScript represents all numbers as floating points, but renders exact integers as integers.
						pojoFieldValue = ((Integer) pojoFieldValue).doubleValue();
					} else if(!valueClass.equals(fieldType)) {
						throw new PojoDocumentException(Code.DOCUMENT_TO_POJO_FAILED, "Object field of incorrect type "+valueClass.getSimpleName()+", expected "+fieldType);
					}
				} else if(cmdFieldValue == null) {
					pojoFieldValue = null;
				} else {
					pojoFieldValue = commandObjectToPojo((CommandObject) cmdFieldValue, fieldType);
				}
				try {
					field.set(pojo, pojoFieldValue);
				} catch (Exception e) {
					throw new PojoDocumentException(e, Code.DOCUMENT_TO_POJO_FAILED, e.getMessage());
				}
			} else {
				PojoDocumentListField pojoDocumentListFieldAnno = field.getAnnotation(PojoDocumentListField.class);
				if(pojoDocumentListFieldAnno != null) {
					checkModifiers(field, pojo.getClass());
					String fieldName = pojoDocumentListFieldAnno.fieldName();
					if(fieldName.length() == 0) {
						fieldName = field.getName();
					}
					Class<?> fieldType = field.getType();
					if(fieldType.equals(List.class)) {
						CommandArray array = commandObject.getArray(fieldName);
						Class<?> itemClass = pojoDocumentListFieldAnno.itemClass();
						List<?> list = commandArrayToList(array, itemClass);
						try {
							field.set(pojo, list);
						} catch (Exception e) {
							throw new PojoDocumentException(e, Code.DOCUMENT_TO_POJO_FAILED, e.getMessage());
						}
					} else {
						throw new PojoDocumentException(Code.POJO_ANNOTATION_ERROR, "Non List field "+fieldName+" annotated as list");
					}
				}
			}
		}
		return pojo;
	}
	
	private static <D> List<D> commandArrayToList(CommandArray array, Class<D> itemClass) {
		List<D> list = new ArrayList<D>();
		for(int i = 0; i < array.size(); i++) {
			CommandArrayItem item = array.getItem(i);
			if(item instanceof SimpleCommandValue) {
				Object value = ((SimpleCommandValue) item).getValue();
				Class<? extends Object> valueClass = value.getClass();
				if(!valueClass.equals(itemClass)) {
					throw new PojoDocumentException(Code.DOCUMENT_TO_POJO_FAILED, "Array item of incorrect type "+valueClass.getSimpleName()+", expected "+itemClass);
				}
				list.add(itemClass.cast(value));
			} else {
				list.add(commandObjectToPojo( (CommandObject) item, itemClass));
			}
		}
		return list;
	}

	private static void checkModifiers(Field field, Class<?> pojoClass) {
		int mod = field.getModifiers();
		if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
			return;
		} else {
			throw new PojoDocumentException(Code.POJO_FIELD_INCORRECT_MODIFIERS, field.getName(), pojoClass.getName());
		}
	}
	
	public static void setPojoProperties(CommandObject commandObject, Object pojo) {
		Field[] fields = pojo.getClass().getFields();
		for(Field field: fields) {
			PojoDocumentField pojoDocumentFieldAnno = field.getAnnotation(PojoDocumentField.class);
			if(pojoDocumentFieldAnno != null) {
				checkModifiers(field, pojo.getClass());
				String fieldName = pojoDocumentFieldAnno.fieldName();
				if(fieldName.length() == 0) {
					fieldName = field.getName();
				}
				Class<?> fieldType = field.getType();
				Object readResult;
				try {
					readResult = field.get(pojo);
				} catch (Exception e) {
					throw new PojoDocumentException(e, Code.POJO_PROPERTY_READ_ERROR, e.getLocalizedMessage());
				}
				if(readResult == null) {
					commandObject.setNull(fieldName);
				} else if(fieldType.equals(Integer.class)) {
					commandObject.setInt(fieldName, (Integer) readResult);
				} else if(fieldType.equals(String.class)) {
					commandObject.setString(fieldName, (String) readResult);
				} else if(fieldType.equals(Double.class)) {
					commandObject.setDouble(fieldName, (Double) readResult);
				} else if(fieldType.equals(Float.class)) {
					commandObject.setDouble(fieldName, ((Float) readResult).doubleValue());
				} else if(fieldType.equals(Boolean.class)) {
					commandObject.setBoolean(fieldName, (Boolean) readResult);
				} else if(fieldType.isAnnotationPresent(PojoDocumentClass.class)) {
					CommandObject fieldObjBuilder = commandObject.setObject(fieldName);
					setPojoProperties(fieldObjBuilder, readResult);
				} else {
					throw new PojoDocumentException(Code.POJO_ANNOTATION_ERROR, "Field "+fieldName+" with incorrect type "+fieldType.getSimpleName()+" annotated with "+PojoDocumentField.class.getSimpleName());
				}
			} else {
				PojoDocumentListField pojoDocumentListFieldAnno = field.getAnnotation(PojoDocumentListField.class);
				if(pojoDocumentListFieldAnno != null) {
					checkModifiers(field, pojo.getClass());
					String fieldName = pojoDocumentListFieldAnno.fieldName();
					if(fieldName.length() == 0) {
						fieldName = field.getName();
					}
					Class<?> fieldType = field.getType();
					Object readResult;
					try {
						readResult = field.get(pojo);
					} catch (Exception e) {
						throw new PojoDocumentException(e, Code.POJO_PROPERTY_READ_ERROR, e.getLocalizedMessage());
					}
					if(fieldType.equals(List.class)) {
						List<?> theList = (List<?>) readResult;
						if(theList != null && !theList.isEmpty()) {
							CommandArray commandArray = commandObject.setArray(fieldName);
							for(Object elem: theList) {
								addToArray(commandArray, elem);
							}
						}
					} else {
						throw new PojoDocumentException(Code.POJO_ANNOTATION_ERROR, "Field "+fieldName+" with incorrect type "+fieldType.getSimpleName()+" annotated with "+PojoDocumentListField.class.getSimpleName());
					}
				} 
			}
		}
	}

	private static void addToArray(CommandArray commandArray, Object elem) {
		if(elem == null) {
			commandArray.addNull();
			return;
		}
		Class<?> elemType = elem.getClass();
		if(elemType.equals(Integer.class)) {
			commandArray.addInt((Integer) elem);
		} else if(elemType.equals(String.class)) {
			commandArray.addString((String) elem);
		} else if(elemType.equals(Double.class)) {
			commandArray.addDouble((Double) elem);
		} else if(elemType.equals(Float.class)) {
			commandArray.addDouble(((Float) elem).doubleValue());
		} else if(elemType.equals(Boolean.class)) {
			commandArray.addBoolean((Boolean) elem);
		} else if(elemType.isAnnotationPresent(PojoDocumentClass.class)) {
			CommandObject elemCmdObj = commandArray.addObject();
			setPojoProperties(elemCmdObj, elem);
		}
	}

	public static String propertyNameForClass(Class<?> theClass) {
		PojoDocumentClass pojoDocumentClassAnno = theClass.getAnnotation(PojoDocumentClass.class);
		if(pojoDocumentClassAnno == null) {
			throw new PojoDocumentException(PojoDocumentException.Code.CLASS_NOT_ANNOTATED, theClass.getName());
		}
		String resultName = pojoDocumentClassAnno.elemName();
		if(resultName.length() > 0) {
			return resultName;
		}
		String simpleName = theClass.getSimpleName();
		StringBuffer buf = new StringBuffer();
		buf.append(Character.toLowerCase(simpleName.charAt(0)));
		buf.append(simpleName.subSequence(1, simpleName.length()));
		return buf.toString();
	}

}
