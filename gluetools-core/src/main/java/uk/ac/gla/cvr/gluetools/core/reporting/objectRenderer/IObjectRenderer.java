package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public interface IObjectRenderer {

	public DocumentResult render(CommandContext cmdContext, GlueDataObject renderableObject);

	public static DocumentResult documentResultFromBytes(byte[] resultXmlBytes) {
		return new DocumentResult(GlueXmlUtils.documentFromBytes(resultXmlBytes));
	}

}
