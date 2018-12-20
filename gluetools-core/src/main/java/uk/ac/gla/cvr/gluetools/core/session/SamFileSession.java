package uk.ac.gla.cvr.gluetools.core.session;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporterPreprocessor.SamReporterPreprocessorSession;
import uk.ac.gla.cvr.gluetools.core.session.SessionException.Code;

public class SamFileSession extends Session {

	public static final String SESSION_TYPE = "samFileSession";

	private SamReporterPreprocessorSession samReporterPreprocessorSession;
	
	@Override
	public void init(CommandContext cmdContext, String[] sessionArgs) {
		if(!(cmdContext instanceof ConsoleCommandContext)) {
			throw new SessionException(Code.SESSION_CREATION_ERROR, "Session of type 'samFileSession' may only be used in a console command context");
		}
		String samReporterName = getSessionArgString(sessionArgs, "samReporterName", 0, true);
		String samFilePath = getSessionArgString(sessionArgs, "samFilePath", 1, true);
		SamReporter samReporter = Module.resolveModulePlugin(cmdContext, SamReporter.class, samReporterName);
		this.samReporterPreprocessorSession = SamReporterPreprocessor.initPreprocessorSession((ConsoleCommandContext) cmdContext, samFilePath, samReporter);
		this.samReporterPreprocessorSession.setStoredInCmdContext(true);
	}

	@Override
	public void close() {
		this.samReporterPreprocessorSession.cleanup();
	}

	public SamReporterPreprocessorSession getSamReporterPreprocessorSession() {
		return samReporterPreprocessorSession;
	}
	
}
