function generateAnalysisSequenceRows(feature, sequenceFeatureResult) {
	
	var analysisSequenceRows = [];
	
	var ntReferenceSegmentArray = feature.ntReferenceSegment;
	var aaReferenceSegmentArray = feature.aaReferenceSegment;

	var ntQuerySegmentArray = sequenceFeatureResult.ntQueryAlignedSegment;
	var aaQuerySegmentArray = sequenceFeatureResult.aaQueryAlignedSegment;
	
	var empty = String.fromCharCode(160); // non breaking space.

	var aasPerRow = 20;
	var ntsPerRow = aasPerRow * 3;
	
	if(ntReferenceSegmentArray) {
		var minNTIndex = ntReferenceSegmentArray[0].refStart;
		var maxNTIndex = ntReferenceSegmentArray[ntReferenceSegmentArray.length-1].refEnd;
		var numNTs = 1+(maxNTIndex - minNTIndex);
		if(numNTs < ntsPerRow) {
			numNTs = ntsPerRow;
		}
		var referenceNTIndices = new Array(numNTs);
		var referenceNTs = new Array(numNTs);
		var queryNTIndices = new Array(numNTs);
		var queryNTs = new Array(numNTs);
		for(var i = 0; i < numNTs; i++) {
			referenceNTIndices[i] = empty;
			referenceNTs[i] = empty;
			queryNTIndices[i] = empty;
			queryNTs[i] = empty;
		}
		for(var segIndex = 0; segIndex < ntReferenceSegmentArray.length; segIndex++) {
			var ntReferenceSegment = ntReferenceSegmentArray[segIndex];
			for(var refSegNTIndex = ntReferenceSegment.refStart; 
					refSegNTIndex <= ntReferenceSegment.refEnd; refSegNTIndex++) {
				var ntColumn = refSegNTIndex - minNTIndex;
				if(refSegNTIndex == ntReferenceSegment.refStart) {
					referenceNTIndices[ntColumn] = refSegNTIndex;
				} else if(refSegNTIndex == ntReferenceSegment.refEnd) {
					referenceNTIndices[ntColumn] = refSegNTIndex;
				}
				referenceNTs[ntColumn] = ntReferenceSegment.nucleotides.charAt(refSegNTIndex - ntReferenceSegment.refStart);
			}
		}

		if(ntQuerySegmentArray) {
			for(var segIndex = 0; segIndex < ntQuerySegmentArray.length; segIndex++) {
				var ntQuerySegment = ntQuerySegmentArray[segIndex];
				var qrySegQryNTIndex = ntQuerySegment.queryStart;
				for(var qrySegRefNTIndex = ntQuerySegment.refStart; 
						qrySegRefNTIndex <= ntQuerySegment.refEnd; qrySegRefNTIndex++) {
					var ntColumn = qrySegRefNTIndex - minNTIndex;
					if(qrySegRefNTIndex == ntQuerySegment.refStart) {
						queryNTIndices[ntColumn] = qrySegQryNTIndex;
					} else if(qrySegRefNTIndex == ntQuerySegment.refEnd) {
						queryNTIndices[ntColumn] = qrySegQryNTIndex;
					}
					queryNTs[ntColumn] = ntQuerySegment.nucleotides.charAt(qrySegRefNTIndex - ntQuerySegment.refStart);
					qrySegQryNTIndex++;
				}
			}
		}

		
		
		var minAAIndex;
		var maxAAIndex;
		var numAAs;
		var referenceAAIndices;
		var referenceAAs;
		var queryAAs;
		if(aaReferenceSegmentArray) {
			minAAIndex = aaReferenceSegmentArray[0].refStart;
			maxAAIndex = aaReferenceSegmentArray[aaReferenceSegmentArray.length-1].refEnd;
			numAAs = 1+(maxAAIndex - minAAIndex);
			referenceAAIndices = new Array(numAAs);
			referenceAAs = new Array(numAAs);
			queryAAs = new Array(numAAs);
			for(var i = 0; i < numAAs; i++) {
				referenceAAIndices[i] = empty;
				referenceAAs[i] = empty;
				queryAAs[i] = empty;
			}
			for(var segIndex = 0; segIndex < aaReferenceSegmentArray.length; segIndex++) {
				var aaReferenceSegment = aaReferenceSegmentArray[segIndex];
				for(var refSegAAIndex = aaReferenceSegment.refStart; 
						refSegAAIndex <= aaReferenceSegment.refEnd; refSegAAIndex++) {
					var aaColumn = refSegAAIndex - minAAIndex;
					if(refSegAAIndex == aaReferenceSegment.refStart) {
						referenceAAIndices[aaColumn] = refSegAAIndex;
					} else if(refSegAAIndex == aaReferenceSegment.refEnd) {
						referenceAAIndices[aaColumn] = refSegAAIndex;
					} else if(refSegAAIndex % 10 == 0) {
						referenceAAIndices[aaColumn] = refSegAAIndex;
					}
					referenceAAs[aaColumn] = aaReferenceSegment.aminoAcids.charAt(refSegAAIndex - aaReferenceSegment.refStart);
				}
			}
			if(aaQuerySegmentArray) {
				for(var segIndex = 0; segIndex < aaQuerySegmentArray.length; segIndex++) {
					var aaQuerySegment = aaQuerySegmentArray[segIndex];
					for(var qrySegAAIndex = aaQuerySegment.refStart; 
							qrySegAAIndex <= aaQuerySegment.refEnd; qrySegAAIndex++) {
						var aaColumn = qrySegAAIndex - minAAIndex;
						queryAAs[aaColumn] = aaQuerySegment.aminoAcids.charAt(qrySegAAIndex - aaQuerySegment.refStart);
					}
				}
			}
		}		

		var row = 0;

		while(row*ntsPerRow <= numNTs) {
			var analysisSequenceRow = {}
			var ntStart = row*ntsPerRow;
			var ntEnd = (row+1)*ntsPerRow;
			analysisSequenceRow["referenceNTIndices"] = referenceNTIndices.slice(ntStart, ntEnd);
			analysisSequenceRow["referenceNTs"] = referenceNTs.slice(ntStart, ntEnd);
			analysisSequenceRow["queryNTIndices"] = queryNTIndices.slice(ntStart, ntEnd);
			analysisSequenceRow["queryNTs"] = queryNTs.slice(ntStart, ntEnd);

			if(aaReferenceSegmentArray) {
				var aaStart = row*aasPerRow;
				var aaEnd = (row+1)*aasPerRow;
				analysisSequenceRow["referenceAAIndices"] = referenceAAIndices.slice(aaStart, aaEnd);
				analysisSequenceRow["referenceAAs"] = referenceAAs.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAAs"] = queryAAs.slice(aaStart, aaEnd);
			}
			analysisSequenceRows.push(analysisSequenceRow);
			row++;
		}
		return analysisSequenceRows;
	}
	
}

function isDifference(reference, query) {
	var empty = String.fromCharCode(160); // non breaking space.
	if(reference == empty) {
		return false;
	}
	if(query == empty) {
		return false;
	}
	if(query == reference) {
		return false;
	}
	return true;
} 