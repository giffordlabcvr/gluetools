package uk.ac.gla.cvr.gluetools.core.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginClass {
	
	public static final String NULL = "NULL";
	
	String elemName();
	boolean deprecated() default false;
	String deprecationWarning() default NULL;
}
