package uk.ac.gla.cvr.gluetools.core.command.result;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultException.Code;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public final class PojoCommandResult<D> extends CommandResult {

	public PojoCommandResult(D pojo) {
		super(propertyNameForClass(pojo.getClass()));
		setPojoProperties(getDocumentBuilder(), pojo);
	}
	
	private void setPojoProperties(ObjectBuilder objectBuilder, Object pojo) {
		Field[] fields = pojo.getClass().getFields();
		for(Field field: fields) {
			PojoResultField pojoResultFieldAnno = field.getAnnotation(PojoResultField.class);
			if(pojoResultFieldAnno != null) {
				int mod = field.getModifiers();
				if(Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
					String resultName = pojoResultFieldAnno.resultName();
					if(resultName.length() == 0) {
						resultName = field.getName();
					}
					Class<?> fieldType = field.getType();
					Object readResult;
					try {
						readResult = field.get(pojo);
					} catch (Exception e) {
						throw new PojoResultException(e, Code.POJO_PROPERTY_READ_ERROR, e.getLocalizedMessage());
					}
					if(readResult == null) {
						objectBuilder.setNull(resultName);
					} else if(fieldType.equals(Integer.class)) {
						objectBuilder.setInt(resultName, (Integer) readResult);
					} else if(fieldType.equals(String.class)) {
						objectBuilder.setString(resultName, (String) readResult);
					} else if(fieldType.equals(Double.class)) {
						objectBuilder.setDouble(resultName, (Double) readResult);
					} if(fieldType.equals(Float.class)) {
						objectBuilder.setDouble(resultName, ((Float) readResult).doubleValue());
					} else if(fieldType.equals(Boolean.class)) {
						objectBuilder.setBoolean(resultName, (Boolean) readResult);
					} else if(fieldType.equals(List.class)) {
						List<?> theList = (List<?>) readResult;
						if(!theList.isEmpty()) {
							ArrayBuilder arrayBuilder = objectBuilder.setArray(resultName);
							for(Object elem: theList) {
								addToArray(arrayBuilder, elem);
							}
						}
					} else if(fieldType.isAnnotationPresent(PojoResultClass.class)) {
						ObjectBuilder fieldObjBuilder = objectBuilder.setObject(resultName);
						setPojoProperties(fieldObjBuilder, readResult);
					}
				} else {
					throw new PojoResultException(Code.POJO_FIELD_INCORRECT_MODIFIERS, field.getName(), pojo.getClass().getName());
				}
			}
		}
	}

	private void addToArray(ArrayBuilder arrayBuilder, Object elem) {
		if(elem == null) {
			arrayBuilder.addNull();
			return;
		}
		Class<?> elemType = elem.getClass();
		if(elemType.equals(Integer.class)) {
			arrayBuilder.addInt((Integer) elem);
		} else if(elemType.equals(String.class)) {
			arrayBuilder.addString((String) elem);
		} else if(elemType.equals(Double.class)) {
			arrayBuilder.addDouble((Double) elem);
		} if(elemType.equals(Float.class)) {
			arrayBuilder.addDouble(((Float) elem).doubleValue());
		} else if(elemType.equals(Boolean.class)) {
			arrayBuilder.addBoolean((Boolean) elem);
		} else if(elemType.isAnnotationPresent(PojoResultClass.class)) {
			ObjectBuilder elemObjBuilder = arrayBuilder.addObject();
			setPojoProperties(elemObjBuilder, elem);
		}
	}

	private static String propertyNameForClass(Class<?> theClass) {
		PojoResultClass pojoResultClassAnno = theClass.getAnnotation(PojoResultClass.class);
		if(pojoResultClassAnno == null) {
			throw new PojoResultException(PojoResultException.Code.CLASS_NOT_ANNOTATED, theClass.getName());
		}
		String resultName = pojoResultClassAnno.resultName();
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
