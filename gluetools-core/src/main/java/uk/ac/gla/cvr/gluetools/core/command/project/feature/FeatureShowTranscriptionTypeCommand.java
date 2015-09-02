package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;

@CommandClass( 
		commandWords={"show", "transcription-type"},
		docoptUsages={""},
		description="Show the transcription type of this feature"
	) 
public class FeatureShowTranscriptionTypeCommand extends FeatureModeCommand<FeatureShowTranscriptionTypeCommand.FeatureShowTranscriptionTypeResult> {

	@Override
	public FeatureShowTranscriptionTypeResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		return new FeatureShowTranscriptionTypeResult(feature.getTranscriptionFormat().name());
	}
	
	public class FeatureShowTranscriptionTypeResult extends MapResult {

		public FeatureShowTranscriptionTypeResult(String transcriptionType) {
			super("featureShowTranscriptionTypeResult", mapBuilder().put(Feature.TRANSCRIPTION_TYPE_PROPERTY, transcriptionType));
		}


	}


}
