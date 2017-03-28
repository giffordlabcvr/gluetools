package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResultRow;

public class AlignmentVariationMemberScanResult extends BaseTableResult<MemberVariationScanResult> {


	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID";


	public AlignmentVariationMemberScanResult(VariationScanRenderHints variationScanRenderHints, List<MemberVariationScanResult> rowData) {
		super("alignmentVariationMemberScanResult", rowData, columns(variationScanRenderHints));
	}

	@SuppressWarnings("unchecked")
	private static TableColumn<MemberVariationScanResult>[] columns(VariationScanRenderHints variationScanRenderHints) {
		TableColumn<VariationScanResultRow>[] vsrrColumns = variationScanRenderHints.generateResultColumns();
		List<TableColumn<MemberVariationScanResult>> mvsrColumns = new ArrayList<TableColumn<MemberVariationScanResult>>();
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(ALIGNMENT_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)));
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(SOURCE_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)));
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(SEQUENCE_ID, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)));
		for(TableColumn<VariationScanResultRow> vsrrColumn: vsrrColumns) {
			mvsrColumns.add(new TableColumn<MemberVariationScanResult>(vsrrColumn.getColumnHeader(), mvsr -> vsrrColumn.populateColumn(mvsr.getVariationScanResultRow())));
		}
		return mvsrColumns.toArray(new TableColumn[]{});
	}

}
