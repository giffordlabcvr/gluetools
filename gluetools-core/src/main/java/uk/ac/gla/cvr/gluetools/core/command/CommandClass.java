package uk.ac.gla.cvr.gluetools.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO some command usages should only be available from the console, not the web.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandClass {

	String description();
	String[] docoptUsages() default {};
	String[] docoptOptions() default {};
	String furtherHelp() default "";
	
}
