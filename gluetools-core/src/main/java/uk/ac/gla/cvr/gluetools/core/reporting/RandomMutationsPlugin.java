package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Random;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

@PluginClass(elemName="randomMutations")
public class RandomMutationsPlugin extends ModulePlugin<RandomMutationsPlugin> {

	public RandomMutationsPlugin() {
		addProvidedCmdClass(GenerateCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {

	}

	@CommandClass(
			commandWords="generate", description = "Generate some random mutations", docoptUsages = { "" }
	)
	public static class GenerateCommand extends ModuleProvidedCommand<RandomMutationsPlugin> {
		@Override
		protected CommandResult execute(CommandContext cmdContext, RandomMutationsPlugin modulePlugin) {
			Element rootElem = GlueXmlUtils.documentWithElement("mutationSet");
			JsonUtils.setJsonType(rootElem, JsonType.Object, false);
			Random random = new Random();
			int numAAs = 90;
			int minNumIsolates = 4000;
			int maxNumIsolates = 14000;
			double mutationChance = 0.25;
			for(int aaIndex = 0; aaIndex < numAAs; aaIndex++) {
				Element aaLocusElem = GlueXmlUtils.appendElement(rootElem, "aaLocus");
				JsonUtils.setJsonType(aaLocusElem, JsonType.Object, true);

				GlueXmlUtils.appendElementWithText(aaLocusElem, "consensusAA", randomAA(random), JsonType.String);
				GlueXmlUtils.appendElementWithText(aaLocusElem, "numIsolates", Integer.toString( (int) (
						minNumIsolates + Math.floor(random.nextDouble() * (maxNumIsolates - minNumIsolates)))), JsonType.Integer);
				
				double percentage = 49.9;
				while(random.nextDouble() < mutationChance) {
					double mutPercentage = random.nextDouble() * percentage;
					if(mutPercentage > 1.0) {
						Element mutationElem = GlueXmlUtils.appendElement(aaLocusElem, "mutation");
						JsonUtils.setJsonType(mutationElem, JsonType.Object, true);
						GlueXmlUtils.appendElementWithText(mutationElem, "mutationAA", randomAA(random), JsonType.String);
						GlueXmlUtils.appendElementWithText(mutationElem, "isolatesPercent", Double.toString(mutPercentage), JsonType.Double);
						percentage = mutPercentage / 2.0;
					}
				}
			}
			return new CommandResult(rootElem.getOwnerDocument());
		}

		private String randomAA(Random random) {
			String allAAs = "ACDEFGHIKLMNOPQRSTUVWY";
			int index = random.nextInt(allAAs.length());
			return allAAs.substring(index, index+1);
		}
	}
}
