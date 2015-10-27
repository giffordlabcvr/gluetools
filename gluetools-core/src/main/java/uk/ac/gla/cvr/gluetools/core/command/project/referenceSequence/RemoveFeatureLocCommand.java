package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"remove", "feature-location"}, 
	docoptUsages={"[-r] <featureName>"},
	docoptOptions={"-r, --recursive  Also remove locations of descendent features"},
	metaTags={CmdMeta.updatesDatabase},
	description="Remove a feature location") 
public class RemoveFeatureLocCommand extends ReferenceSequenceModeCommand<DeleteResult> {

	public static final String FEATURE_NAME = "featureName";
	public static final String RECURSIVE = "recursive";

	private String featureName;
	private Boolean recursive;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(featureName));
		List<String> locsToDelete = new ArrayList<String>();
		locsToDelete.add(featureName);
		if(recursive) {
			List<Feature> descendents = feature.getDescendents();
			locsToDelete.addAll(descendents.stream().map(Feature::getName).collect(Collectors.toList()));
		}
		int deleted = 0;
		for(String locToDelete: locsToDelete) {
			DeleteResult result = GlueDataObject.delete(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(getRefSeqName(), locToDelete), true);
			cmdContext.commit();
			deleted += result.getNumber();
		}
		return new DeleteResult(FeatureLocation.class, deleted);
	}

	@CompleterClass
	public static class Completer extends FeatureLocNameCompleter {}

}
