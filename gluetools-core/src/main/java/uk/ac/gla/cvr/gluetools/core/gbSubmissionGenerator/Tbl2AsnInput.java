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
package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider.GbFeatureSpecification;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public class Tbl2AsnInput {

	private String sourceName;
	private String sequenceID;
	private String id;
	private DNASequence fastaSequence;
	private Map<String, String> sourceInfoMap;
	private List<GbFeatureSpecification> gbFeatureSpecifications;
	
	public Tbl2AsnInput(String sourceName, String sequenceID, String id, DNASequence fastaSequence, Map<String, String> sourceInfoMap, 
			List<GbFeatureSpecification> gbFeatureSpecifications) {
		super();
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
		this.id = id;
		this.fastaSequence = fastaSequence;
		this.sourceInfoMap = sourceInfoMap;
		this.gbFeatureSpecifications = gbFeatureSpecifications;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public String getId() {
		return id;
	}

	public DNASequence getFastaSequence() {
		return fastaSequence;
	}

	public Map<String, String> getSourceInfoMap() {
		return sourceInfoMap;
	}

	public List<GbFeatureSpecification> getGbFeatureSpecifications() {
		return gbFeatureSpecifications;
	}
}
