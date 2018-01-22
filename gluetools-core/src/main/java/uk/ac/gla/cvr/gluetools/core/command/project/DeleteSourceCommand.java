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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "source"}, 
	docoptUsages={"[-b <batchSize>] <sourceName>"},
	docoptOptions={"-b <batchSize>, --batchSize <batchSize>  Sequence deletion batch size"},
	metaTags={CmdMeta.updatesDatabase},		
	description="Delete a sequence source and all its sequences",
	furtherHelp="Sequences are deleted in batches before the source is deleted."+
	" Default batch size is 250.") 
public class DeleteSourceCommand extends ProjectModeCommand<DeleteResult> {

	private String sourceName;
	private int batchSize;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Source source = GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName), true);
		if(source == null) {
			return new DeleteResult(Source.class, 0);
		}
		GlueLogger.getGlueLogger().fine("Finding sequences in source "+sourceName);
		List<Map<String, String>> pkMaps = 
				source
				.getSequences()
				.stream().map(seq -> seq.pkMap())
				.collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" sequences.");
		int totalDeleted = 0;
		for(Map<String, String> pkMap: pkMaps) {
			DeleteResult delResult = GlueDataObject.delete(cmdContext, Sequence.class, pkMap, true);
			totalDeleted += delResult.getNumber();
			if(totalDeleted % batchSize == 0) {
				cmdContext.commit();
				GlueLogger.getGlueLogger().finest("Deleted "+totalDeleted+" sequences from source "+sourceName);
			} 
		}
		GlueLogger.getGlueLogger().finest("Deleted "+totalDeleted+" sequences from source "+sourceName);
		DeleteResult result = GlueDataObject.delete(cmdContext, Source.class, Source.pkMap(sourceName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
		}
	}


}
