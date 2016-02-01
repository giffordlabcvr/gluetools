package uk.ac.gla.cvr.gluetools.core.transcription;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;

public class CommandContextTranslator implements Translator {
	private boolean translateBeyondPossibleStopCodon;
	private boolean translateBeyondDefiniteStopCodon;

	public CommandContextTranslator(CommandContext cmdContext) {
		translateBeyondPossibleStopCodon = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_BEYOND_POSSIBLE_STOP).equals("true");
		translateBeyondDefiniteStopCodon = cmdContext.getProjectSettingValue(ProjectSettingOption.TRANSLATE_BEYOND_DEFINITE_STOP).equals("true");
	}

	public String translate(CharSequence nts) {
		return TranslationUtils.translate(nts, false, false, translateBeyondPossibleStopCodon, translateBeyondDefiniteStopCodon);
	}
	
}