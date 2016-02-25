package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GlueDataClass {
	// built-in fields listed by default by the list command
	String[] defaultListedFields();
	// built-in fields which may optionally be listed by the list command
	String[] listableBuiltInFields() default {};
	// built-in fields which may be set / unset by generic set / unset commands, in a similar way to custom fields
	String[] modifiableBuiltInFields() default {};
}
