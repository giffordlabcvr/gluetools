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
package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AbstractPlacementCommand<R extends CommandResult> extends AbstractQueryResultCommand<R> {

	public final static String PLACEMENT_INDEX = "placementIndex";

	private Integer placementIndex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.placementIndex = PluginUtils.configureIntProperty(configElem, PLACEMENT_INDEX, true);
	}

	protected final R executeOnQueryResult(CommandContext cmdContext, 
			MaxLikelihoodPlacer maxLikelihoodPlacer, 
			MaxLikelihoodPlacerResult placerResult, 
			MaxLikelihoodSingleQueryResult queryResult) {
		return executeOnPlacementResult(cmdContext, maxLikelihoodPlacer, placerResult, queryResult, getPlacement(queryResult, placementIndex));
	}

	protected static MaxLikelihoodSinglePlacement getPlacement(
			MaxLikelihoodSingleQueryResult queryResult, Integer placementIndex) {
		Optional<MaxLikelihoodSinglePlacement> firstResult = queryResult.singlePlacement
				.stream()
				.filter(res -> res.placementIndex.equals(placementIndex)).findFirst();
		if(!firstResult.isPresent()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "File does not contain placement with index "+placementIndex+
					" for query '"+queryResult.queryName+"'");
		}
		return firstResult.get();
	}

	protected abstract R executeOnPlacementResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement);

	protected static class AbstractPlacementCommandCompleter extends AbstractQueryResultCommandCompleter {
		public AbstractPlacementCommandCompleter() {
			super();
			registerVariableInstantiator("placementIndex", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String inputFile = (String) bindings.get("inputFile");
					String queryName = (String) bindings.get("queryName");
					try {
						MaxLikelihoodPlacerResult placerResult = loadPlacerResult(cmdContext, inputFile);
						MaxLikelihoodSingleQueryResult queryResult = getQueryResult(placerResult, queryName);
						return queryResult.singlePlacement.stream()
								.map(placement -> new CompletionSuggestion(Integer.toString(placement.placementIndex), true))
								.collect(Collectors.toList());
					} catch(Exception e) {
						return null;
					}
				}
			});
		}
	}

}
