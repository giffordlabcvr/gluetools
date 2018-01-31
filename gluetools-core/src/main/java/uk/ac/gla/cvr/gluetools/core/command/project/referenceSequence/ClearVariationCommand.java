/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"clear", "variation"}, 
	docoptUsages={"[-f <featureName>]"},
	docoptOptions={
			"-f <featureName>, --feature <featureName>  Delete from the named feature-location"
	},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a set of variations") 
public class ClearVariationCommand extends ReferenceSequenceModeCommand<DeleteResult> {

	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "feature", false);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName());
		
		if(featureName != null) {
			// check feature exists
			GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
			exp = exp.andExp(ExpressionFactory.matchExp(Variation.FEATURE_NAME_PATH, featureName));
		}
		List<Variation> variations = GlueDataObject.query(cmdContext, Variation.class, new SelectQuery(Variation.class, exp));
		
		int numDeleted = 0;
		for(Variation variation: variations) {
			DeleteResult result = 
					GlueDataObject.delete(cmdContext, Variation.class, variation.pkMap(), false);
			numDeleted += result.getNumber();
			
		}
		cmdContext.commit();
		return new DeleteResult(Variation.class, numDeleted);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("featureName", new AdvancedCmdCompleter.VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String refSeqName = ((ReferenceSequenceMode) cmdContext.peekCommandMode()).getRefSeqName();
					ReferenceSequence refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(refSeqName));
					return refSequence.getFeatureLocations().stream()
							.map(fl -> new CompletionSuggestion(fl.getFeature().getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
		
	}

}
