package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePluginException;

@SuppressWarnings("serial")
public class NcbiImporterException extends ModulePluginException {

	public enum Code implements GlueErrorCode {
		IO_ERROR("requestName", "errorTxt"), 
		FORMATTING_ERROR("requestName", "errorTxt"),
		SEARCH_ERROR("errorTxt"),
		CONFIG_ERROR("errorTxt"),
		PROTOCOL_ERROR("requestName", "errorTxt"),
		PROXY_ERROR("errorTxt"),
		CANNOT_PROCESS_SEQUENCE_FORMAT("formatName"), 
		NULL_SEQUENCE_ID("xmlDoc");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public NcbiImporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public NcbiImporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	

}
