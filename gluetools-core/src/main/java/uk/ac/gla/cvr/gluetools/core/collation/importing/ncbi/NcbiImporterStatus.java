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
package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NcbiImporterStatus {
	// map of GI Number -> Seq ID, sequences which both match the search spec and exist in the source.
	private Map<String, String>  presentGiNumbers;

	// the set of GI Numbers which match the search spec but are not in the source
	private Set<String> missingGiNumbers;

	// map of GI Number -> Seq ID, sequences which should be deleted from the source
	private Map<String, String>  surplusGiNumbers;

	// map of GI Number -> Seq ID, sequences which have been downloaded to the source by this command invocation
	private Map<String, String> downloadedGiNumbers;

	// the set of GI Numbers which have been deleted from the source by this command invocation
	private Set<String> deletedGiNumbers;

	private int totalMatching;

	public NcbiImporterStatus(int totalMatching, Map<String, String>  presentGiNumbers,
			Set<String> missingGiNumbers, Map<String, String>  surplusGiNumbers,
			Map<String, String> downloadedGiNumbers, Set<String> deletedGiNumbers) {
		super();
		this.totalMatching = totalMatching;
		this.presentGiNumbers = presentGiNumbers;
		this.missingGiNumbers = missingGiNumbers;
		this.surplusGiNumbers = surplusGiNumbers;
		this.downloadedGiNumbers = downloadedGiNumbers;
		this.deletedGiNumbers = deletedGiNumbers;
	}

	public Map<String, String>  getPresentGiNumbers() {
		return presentGiNumbers;
	}

	public Set<String> getMissingGiNumbers() {
		return missingGiNumbers;
	}

	public Map<String, String> getSurplusGiNumbers() {
		return surplusGiNumbers;
	}

	public int getTotalMatching() {
		return totalMatching;
	}

	public void setTotalMatching(int totalMatching) {
		this.totalMatching = totalMatching;
	}

	public Map<String, String> getDownloadedGiNumbers() {
		return downloadedGiNumbers;
	}

	public void setDownloadedGiNumbers(Map<String, String> downloadedGiNumbers) {
		this.downloadedGiNumbers = downloadedGiNumbers;
	}

	public Set<String> getDeletedGiNumbers() {
		return deletedGiNumbers;
	}

	public void setDeletedGiNumbers(Set<String> deletedGiNumbers) {
		this.deletedGiNumbers = deletedGiNumbers;
	}

	public List<SequenceStatus> getSequenceStatusTable() {
		Map<String, SequenceStatus> sequenceStatusTable = new LinkedHashMap<String, SequenceStatus>();
		getMissingGiNumbers().forEach(gi -> sequenceStatusTable.put(gi, new SequenceStatus(gi, null, Status.MISSING)));
		getSurplusGiNumbers().forEach((gi,seqID) -> sequenceStatusTable.put(gi, new SequenceStatus(gi, seqID, Status.SURPLUS)));
		getPresentGiNumbers().forEach((gi,seqID) -> sequenceStatusTable.put(gi, new SequenceStatus(gi, seqID, Status.PRESENT)));
		getDownloadedGiNumbers().forEach((gi,seqID) -> {
			SequenceStatus sequenceStatus = sequenceStatusTable.get(gi);
			sequenceStatus.setAction(Action.DOWNLOADED);
			sequenceStatus.setSequenceID(seqID);
		});
		getDeletedGiNumbers().forEach(gi -> sequenceStatusTable.get(gi).setAction(Action.DELETED));
		return new ArrayList<SequenceStatus>(sequenceStatusTable.values());
	}
	
	public static enum Status {
		MISSING,
		PRESENT,
		SURPLUS
	}

	public static enum Action {
		DOWNLOADED,
		DELETED,
	}
	
	public static class SequenceStatus {
		private String giNumber;
		private String sequenceID;
		private Status status;
		private Action action;
		private SequenceStatus(String giNumber, String sequenceID, Status status) {
			super();
			this.giNumber = giNumber;
			this.sequenceID = sequenceID;
			this.status = status;
		}

		public void setSequenceID(String seqID) {
			this.sequenceID = seqID;
		}

		public String getGiNumber() {
			return giNumber;
		}
		public Status getStatus() {
			return status;
		}

		public Action getAction() {
			return action;
		}

		public String getSequenceID() {
			return sequenceID;
		}

		public void setAction(Action action) {
			this.action = action;
		}
		
	}
	
}
