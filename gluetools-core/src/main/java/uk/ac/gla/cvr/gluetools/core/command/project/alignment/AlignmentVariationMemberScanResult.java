/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanMatchResultRow;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;

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
		/* RESTORE_XXXX
		TableColumn<VariationScanMatchResultRow>[] vsrrColumns = variationScanRenderHints.generateResultColumns();
		List<TableColumn<MemberVariationScanResult>> mvsrColumns = new ArrayList<TableColumn<MemberVariationScanResult>>();
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(ALIGNMENT_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)));
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(SOURCE_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)));
		mvsrColumns.add(new TableColumn<MemberVariationScanResult>(SEQUENCE_ID, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)));
		for(TableColumn<VariationScanMatchResultRow> vsrrColumn: vsrrColumns) {
			mvsrColumns.add(new TableColumn<MemberVariationScanResult>(vsrrColumn.getColumnHeader(), mvsr -> vsrrColumn.populateColumn(mvsr.getVariationScanResultRow())));
		}
		return mvsrColumns.toArray(new TableColumn[]{});
		*/ return null;
	}

}
