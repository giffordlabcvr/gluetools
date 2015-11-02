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
		GlueLogger.getGlueLogger().fine("Finding sequences in source "+sourceName);
		List<Map<String, String>> pkMaps = 
				GlueDataObject.lookup(cmdContext, Source.class, Source.pkMap(sourceName))
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
