package uk.ac.gla.cvr.gluetools.core.datamodel;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public class GlueConfigContext {
	private boolean variations;
	private boolean variationCategories;
	private CommandContext cmdContext;

	public GlueConfigContext(CommandContext cmdContext) {
		super();
		this.cmdContext = cmdContext;
	}
	public boolean includeVariations() {
		return variations;
	}
	public void setIncludeVariations(boolean variations) {
		this.variations = variations;
	}
	public boolean includeVariationCategories() {
		return variationCategories;
	}
	public void setIncludeVariationCategories(boolean variationCategories) {
		this.variationCategories = variationCategories;
	}
	public CommandContext getCommandContext() {
		return cmdContext;
	}
	
}
