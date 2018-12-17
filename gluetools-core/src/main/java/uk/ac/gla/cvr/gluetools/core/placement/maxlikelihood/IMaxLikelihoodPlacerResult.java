package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

public interface IMaxLikelihoodPlacerResult {

	public PhyloTree getLabelledPhyloTree();

	public List<MaxLikelihoodSingleQueryResult> getQueryResults();

	public static IMaxLikelihoodPlacerResult fromCommandDocument(CommandDocument cmdDocument) {
		String rootName = cmdDocument.getRootName();
		if(rootName.equals(DetailedMaxLikelihoodPlacerResult.DOCUMENT_ROOT_NAME)) {
			return DetailedMaxLikelihoodPlacerResult.fromCommandDocument(cmdDocument);
		} else if(rootName.equals("maxLikelihoodPlacerResult")) {
			return PojoDocumentUtils.commandObjectToPojo(cmdDocument, MaxLikelihoodPlacerResult.class);
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unknown root document name: "+rootName);
		}
	}

	public static CommandDocument toCommandDocument(IMaxLikelihoodPlacerResult iMaxLikelihoodPlacerResult) {
		if(iMaxLikelihoodPlacerResult instanceof MaxLikelihoodPlacerResult) {
			return PojoDocumentUtils.pojoToCommandDocument((MaxLikelihoodPlacerResult) iMaxLikelihoodPlacerResult);
		} else if(iMaxLikelihoodPlacerResult instanceof DetailedMaxLikelihoodPlacerResult) {
			return DetailedMaxLikelihoodPlacerResult.toCommandDocument((DetailedMaxLikelihoodPlacerResult) iMaxLikelihoodPlacerResult);
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unknown subtype "+iMaxLikelihoodPlacerResult.getClass().getSimpleName()+
					" of "+IMaxLikelihoodPlacerResult.class.getSimpleName());
		}
	}
}
