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

public abstract class AbstractQueryResultCommand<R extends CommandResult> extends AbstractPlacerResultCommand<R> {

	public final static String QUERY_NAME = "queryName";

	private String queryName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.queryName = PluginUtils.configureStringProperty(configElem, QUERY_NAME, true);
	}

	protected final R executeOnPlacerResult(CommandContext cmdContext, 
			MaxLikelihoodPlacer maxLikelihoodPlacer, MaxLikelihoodPlacerResult placerResult) {
		return executeOnQueryResult(cmdContext, maxLikelihoodPlacer, placerResult, getQueryResult(placerResult, this.queryName));
	}

	protected static MaxLikelihoodSingleQueryResult getQueryResult(
			MaxLikelihoodPlacerResult placerResult, String queryName2) {
		Optional<MaxLikelihoodSingleQueryResult> firstResult = placerResult.singleQueryResult.stream().filter(res -> res.queryName.equals(queryName2)).findFirst();
		if(!firstResult.isPresent()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "File does not contain result for query '"+queryName2+"'");
		}
		MaxLikelihoodSingleQueryResult queryResult = firstResult.get();
		return queryResult;
	}

	protected abstract R executeOnQueryResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult);

	protected static class AbstractQueryResultCommandCompleter extends AbstractPlacerResultCommandCompleter {
		public AbstractQueryResultCommandCompleter() {
			super();
			registerVariableInstantiator("queryName", new VariableInstantiator() {
				@Override
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String inputFile = (String) bindings.get("inputFile");
					try {
						MaxLikelihoodPlacerResult placerResult = loadPlacerResult(cmdContext, inputFile);
						return placerResult.singleQueryResult.stream()
								.map(res -> new CompletionSuggestion(res.queryName, true))
								.collect(Collectors.toList());
					} catch(Exception e) {
						return null;
					}
				}
			});
		}
	}

}
