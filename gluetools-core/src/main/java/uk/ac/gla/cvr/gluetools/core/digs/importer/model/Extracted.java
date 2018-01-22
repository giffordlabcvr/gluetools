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
package uk.ac.gla.cvr.gluetools.core.digs.importer.model;

import uk.ac.gla.cvr.gluetools.core.digs.importer.model.auto._Extracted;

public class Extracted extends _Extracted {

	public static final String[] ALL_PROPERTIES = new String[] {
		    _Extracted.ALIGN_LEN_PROPERTY,
		    _Extracted.ASSIGNED_GENE_PROPERTY,
		    _Extracted.ASSIGNED_NAME_PROPERTY,
		    _Extracted.BIT_SCORE_PROPERTY,
		    _Extracted.BLAST_ID_PROPERTY,
		    _Extracted.DATA_TYPE_PROPERTY,
		    _Extracted.E_VALUE_EXP_PROPERTY,
		    _Extracted.E_VALUE_NUM_PROPERTY,
		    _Extracted.EXTRACT_END_PROPERTY,
		    _Extracted.EXTRACT_START_PROPERTY,
		    _Extracted.GAP_OPENINGS_PROPERTY,
		    _Extracted.IDENTITY_PROPERTY,
		    _Extracted.MISMATCHES_PROPERTY,
		    _Extracted.ORGANISM_PROPERTY,
		    _Extracted.ORIENTATION_PROPERTY,
		    _Extracted.PROBE_TYPE_PROPERTY,
		    _Extracted.QUERY_END_PROPERTY,
		    _Extracted.QUERY_START_PROPERTY,
		    _Extracted.SCAFFOLD_PROPERTY,
		    _Extracted.SUBJECT_END_PROPERTY,
		    _Extracted.SUBJECT_START_PROPERTY,
		    _Extracted.TARGET_NAME_PROPERTY,
		    _Extracted.VERSION_PROPERTY,
	};
	
}
