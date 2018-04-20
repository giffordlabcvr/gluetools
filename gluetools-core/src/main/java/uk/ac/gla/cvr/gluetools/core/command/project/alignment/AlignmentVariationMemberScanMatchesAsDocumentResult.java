package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public class AlignmentVariationMemberScanMatchesAsDocumentResult extends CommandResult {

	public AlignmentVariationMemberScanMatchesAsDocumentResult(List<MemberVariationScanResult> mvsrs) {
		super("alignmentVariationMemberScanMatchesAsDocumentResult");
		buildDocumentFromScanResults(getCommandDocument(), mvsrs);
	}

	private void buildDocumentFromScanResults(CommandDocument commandDocument, List<MemberVariationScanResult> mvsrs) {
		CommandArray memberArray = commandDocument.setArray("alignmentMember");
		for(MemberVariationScanResult mvsr: mvsrs) {
			CommandObject memberObject = memberArray.addObject();
			memberObject.setString("alignmentName", mvsr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH));
			memberObject.setString("sourceName", mvsr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH));
			memberObject.setString("sequenceID", mvsr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH));
			CommandArray matchArray = memberObject.setArray("matches");
			for(VariationScannerMatchResult vsmr: mvsr.getVariationScanResult().getVariationScannerMatchResults()) {
				CommandObject matchObject = matchArray.addObject();
				vsmr.populateMatchObject(matchObject);
			}
		}
	}

	
}
