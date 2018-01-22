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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","source"}, 
	docoptUsages={"[-a] <sourceName>"},
	docoptOptions={"-a, --allowExisting  Continue without error if the source already exists."},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new sequence source", 
	furtherHelp="A sequence source is a grouping of sequences where each sequence has a unique ID within the source.") 
public class CreateSourceCommand extends ProjectModeCommand<CreateResult> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String ALLOW_EXISTING = "allowExisting";
	
	private String sourceName;
	private boolean allowExisting;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, true);
		allowExisting = Optional.ofNullable(PluginUtils.
				configureBooleanProperty(configElem, ALLOW_EXISTING, false)).orElse(false);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		createSource(cmdContext, sourceName, allowExisting);
		cmdContext.commit();
		return new CreateResult(Source.class, 1);
	}

	public static Source createSource(CommandContext cmdContext, String sourceName, boolean allowExisting) {
		return GlueDataObject.create(cmdContext, Source.class, Source.pkMap(sourceName), allowExisting);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}
}
