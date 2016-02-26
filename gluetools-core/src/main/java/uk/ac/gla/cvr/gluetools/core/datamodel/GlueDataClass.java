package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GlueDataClass {
	// built-in properties listed by default by the list command
	String[] defaultListedProperties();
	// built-in properties which may optionally be listed by the list command
	String[] listableBuiltInProperties() default {};
	// built-in properties which may be set / unset by generic set / unset commands, in a similar way to custom fields
	String[] modifiableBuiltInProperties() default {};
}
