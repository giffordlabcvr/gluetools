package uk.ac.gla.cvr.gluetools.core.validation;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class ValidateResult extends BaseTableResult<ValidateException> {

	public static final String 
		OBJECT_PATH = "objectPath",
		CLASS = "class",
		CODE = "code",
		ERROR_TEXT = "errorTxt";


	public ValidateResult(List<ValidateException> rowData) {
		super("validateResult", rowData,
				column(OBJECT_PATH, ve -> ve.getErrorArgs()[0]),
				//column(CLASS, ve -> ve.getErrorArgs()[1]),
				//column(CODE, ve -> ve.getErrorArgs()[2]),
				column(ERROR_TEXT, ve -> ve.getErrorArgs()[3]));
	}

}

