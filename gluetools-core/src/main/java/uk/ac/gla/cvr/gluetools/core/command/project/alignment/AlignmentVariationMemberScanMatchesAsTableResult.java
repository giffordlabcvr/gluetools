package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScannerMatchResult;

public class AlignmentVariationMemberScanMatchesAsTableResult extends BaseTableResult<MemberVariationScannerMatchResult> {

	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID";

	
	public <M extends VariationScannerMatchResult> AlignmentVariationMemberScanMatchesAsTableResult(Class<M> matchResultClass, List<MemberVariationScannerMatchResult> mvsmrs) {
		super("alignmentVariationMemberScanMatchesAsTableResult", mvsmrs,
				generateResultColumns(matchResultClass, 
						column(ALIGNMENT_NAME, mvsmr -> mvsmr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)), 
						column(SOURCE_NAME, mvsmr -> mvsmr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)),
						column(SEQUENCE_ID, mvsmr -> mvsmr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH))
				));
	}
	
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static final <M extends VariationScannerMatchResult> TableColumn<MemberVariationScannerMatchResult>[] generateResultColumns(
			Class<M> matchResultClass, 
			TableColumn<MemberVariationScannerMatchResult>... mvsmrColumns) {
		List<TableColumn<MemberVariationScannerMatchResult>> columns = 
				new ArrayList<TableColumn<MemberVariationScannerMatchResult>>(Arrays.asList(mvsmrColumns));
		List<TableColumn<? extends VariationScannerMatchResult>> vsmrColumns = VariationScannerMatchResult.getColumnsForMatchResultClass(matchResultClass);
		for(TableColumn<? extends VariationScannerMatchResult> vsmrColumn: vsmrColumns) {
			TableColumn<M> castVsmrColumn = (TableColumn<M>) vsmrColumn;
			columns.add(new TableColumn<MemberVariationScannerMatchResult>(
					vsmrColumn.getColumnHeader(), mvsmr -> castVsmrColumn.populateColumn((M) mvsmr.getVariationScannerMatchResult())));
		}
		return columns.toArray(new TableColumn[]{});
	}

	
}
