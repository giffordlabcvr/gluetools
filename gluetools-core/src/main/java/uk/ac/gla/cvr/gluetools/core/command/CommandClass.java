package uk.ac.gla.cvr.gluetools.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandClass {

	String description();
	String[] docoptUsages() default {};
	String[] docoptOptions() default {};
	
}
