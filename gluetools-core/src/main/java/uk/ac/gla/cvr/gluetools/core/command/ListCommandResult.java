package uk.ac.gla.cvr.gluetools.core.command;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

// TODO -- List command input should be able to specify field subset.
// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.
// TODO -- list commands should take an optional cayenne where query.

public class ListCommandResult<D extends GlueDataObject> extends CommandResult {

	private List<D> results;
	private Class<D> resultClass;
	private String[] columnPropertyNames;
	
	public ListCommandResult(Class<D> resultClass, List<D> results) {
		this(resultClass, results,
				resultClass.getAnnotation(GlueDataClass.class).defaultListColumns());
	}

	public String[] getColumnPropertyNames() {
		return columnPropertyNames;
	}

	public ListCommandResult(Class<D> resultClass, List<D> results, 
			String[] columnPropertyNames) {
		super();
		this.results = results;
		this.resultClass = resultClass;
		this.columnPropertyNames = columnPropertyNames;
	}



	public List<D> getResults() {
		return results;
	}

	public Class<D> getResultClass() {
		return resultClass;
	}
}
