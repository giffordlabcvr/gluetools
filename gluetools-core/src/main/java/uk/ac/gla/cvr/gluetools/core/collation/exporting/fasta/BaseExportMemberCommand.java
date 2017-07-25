package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class BaseExportMemberCommand<R extends CommandResult> extends ModulePluginCommand<R, FastaExporter> implements ProvidedProjectModeCommand {
	
	public static final String LINE_FEED_STYLE = "lineFeedStyle";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String RECURSIVE = "recursive";
	public static final String ALIGNMENT_NAME = "alignmentName";

	private Expression whereClause;
	private String alignmentName;
	private Boolean recursive;
	private LineFeedStyle lineFeedStyle;


	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		lineFeedStyle = Optional.ofNullable(PluginUtils.configureEnumProperty(LineFeedStyle.class, configElem, LINE_FEED_STYLE, false)).orElse(LineFeedStyle.LF);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false);
		recursive = PluginUtils.configureBooleanProperty(configElem, "recursive", true);
		alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
	}

	protected Expression getWhereClause() {
		return whereClause;
	}

	protected LineFeedStyle getLineFeedStyle() {
		return lineFeedStyle;
	}

	protected Boolean getRecursive() {
		return recursive;
	}

	protected String getAlignmentName() {
		return alignmentName;
	}
	
	
	
}