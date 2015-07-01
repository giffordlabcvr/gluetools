package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SimpleConfigureCommandClass {

	String[] propertyNames();

	String description() default "Configure a property of this module.";

}
