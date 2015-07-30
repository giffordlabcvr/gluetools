package uk.ac.gla.cvr.gluetools.core.command.root;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandModeClass {
	Class<? extends CommandFactory> commandFactoryClass();
}
