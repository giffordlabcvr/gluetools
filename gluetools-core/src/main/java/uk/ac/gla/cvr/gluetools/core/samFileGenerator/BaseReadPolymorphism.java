package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import java.util.Optional;

import htsjdk.samtools.SAMRecord;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseReadPolymorphism implements Plugin {

	private Boolean applyToRead1;
	private Boolean applyToRead2;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.applyToRead1 = 
				Optional.ofNullable(PluginUtils.
						configureBooleanProperty(configElem, "applyToRead1", false)).orElse(true);
		this.applyToRead2 = 
				Optional.ofNullable(PluginUtils.
						configureBooleanProperty(configElem, "applyToRead2", false)).orElse(true);
	}

	
	public abstract void applyPolymorphism(CommandContext cmdContext, SAMRecord samRecord, SamFileGenerator samFileGenerator);


	public Boolean getApplyToRead1() {
		return applyToRead1;
	}


	public Boolean getApplyToRead2() {
		return applyToRead2;
	}
	
}
