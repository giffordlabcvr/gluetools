
function updateDifferenceStyle(oldDifferenceStyle, newDifferenceStyle) {
	if(newDifferenceStyle == "difference_NOT_NOTIFIABLE" && oldDifferenceStyle == "difference_NOTIFIABLE") {
		return oldDifferenceStyle;
	}
	return newDifferenceStyle;
}

function generateAlignmentDifferenceSummaries(sequenceResult) {
	var alignmentDifferenceSummaries = [];
	for(var x = 0; x < sequenceResult.sequenceAlignmentResult.length; x++) {
		var sequenceAlignmentResult = sequenceResult.sequenceAlignmentResult[x];
		var differenceSummariesForAlmtResult = 
			generateDifferenceSummariesForAlmtResult(sequenceAlignmentResult);
		for(var y = 0; y < differenceSummariesForAlmtResult.length; y++) {
			alignmentDifferenceSummaries.push(differenceSummariesForAlmtResult[y]);
		}
	}
	return alignmentDifferenceSummaries;
}

function generateDifferenceSummariesForAlmtResult(sequenceAlignmentResult) {
	var differenceSummariesForAlmtResult = [];
	var firstDiffSummary = null;
	for(var i = 0; i < sequenceAlignmentResult.sequenceFeatureResult.length; i++) {
		var sequenceFeatureResult = sequenceAlignmentResult.sequenceFeatureResult[i];
		if(!sequenceFeatureResult.aaReferenceDifferenceNote) {
			continue;
		}
		var differenceSummary = {
			"featureName":sequenceFeatureResult.featureName,
			"referenceName":sequenceAlignmentResult.referenceName
		};
		var differences = [];
		for(var j = 0; j < sequenceFeatureResult.aaReferenceDifferenceNote.length; j++) {
			aaReferenceDifferenceNote = sequenceFeatureResult.aaReferenceDifferenceNote[j];
			if(aaReferenceDifferenceNote.differenceSummaryNote) {
				for(var k = 0; k < aaReferenceDifferenceNote.differenceSummaryNote.length; k++) {
					differences.push(aaReferenceDifferenceNote.differenceSummaryNote[k]);
				}
			}
		}
		if(differences.length > 0) {
			if(firstDiffSummary == null) {
				firstDiffSummary = differenceSummary;
			}
			differenceSummary["differences"] = differences;
			differenceSummariesForAlmtResult.push(differenceSummary);
		}
	}
	firstDiffSummary["rowspan"] = differenceSummariesForAlmtResult.length;
	return differenceSummariesForAlmtResult;
}

function generateAnalysisSequenceRows(feature, sequenceFeatureResult) {
	
	var analysisSequenceRows = [];
	
	var variationMap = {};

	if(feature.variation) {
		for(var x = 0; x < feature.variation.length; x++) {
			variationMap[feature.variation[x].name] = feature.variation[x];
		}
	}
	
	var ntReferenceSegmentArray = feature.ntReferenceSegment;
	var aaReferenceSegmentArray = feature.aaReferenceSegment;

	var ntQuerySegmentArray = sequenceFeatureResult.ntQueryAlignedSegment;
	var aaQuerySegmentArray = sequenceFeatureResult.aaQueryAlignedSegment;

	var ntVariationNoteArray = sequenceFeatureResult.ntVariationNote;
	var aaVariationNoteArray = sequenceFeatureResult.aaVariationNote;

	var ntReferenceDiffArray = sequenceFeatureResult.ntReferenceDifferenceNote;
	var aaReferenceDiffArray = sequenceFeatureResult.aaReferenceDifferenceNote;
	
	var aaMinorityVariantArray = sequenceFeatureResult.aaMinorityVariant;
	
	var aaMinorityVariantMap = {};
	
	if(aaMinorityVariantArray) {
		for(var x = 0; x < aaMinorityVariantArray.length; x++) {
			var aaMv = aaMinorityVariantArray[x];
			// update map only if proportion is higher?
			var existing = aaMinorityVariantMap[aaMv.aaIndex];
			if(existing) {
				if(aaMv.proportion > existing.proportion) {
					aaMinorityVariantMap[aaMv.aaIndex] = aaMv;
				}
			} else {
				aaMinorityVariantMap[aaMv.aaIndex] = aaMv;
			}
		}
	}

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
		var queryNTDifferenceStyle = new Array(numNTs);
		
		for(var i = 0; i < numNTs; i++) {
			referenceNTIndices[i] = empty;
			referenceNTs[i] = empty;
			queryNTIndices[i] = empty;
			queryNTs[i] = empty;
			queryNTDifferenceStyle[i] = "";
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
				var ntReferenceDiff;
				if(ntReferenceDiffArray && segIndex < ntReferenceDiffArray.length) {
					var diff = ntReferenceDiffArray[segIndex];
					if(diff.refStart == ntQuerySegment.refStart && 
					   diff.refEnd == ntQuerySegment.refEnd) {
						ntReferenceDiff = diff;
					}
				}
				var qrySegQryNTIndex = ntQuerySegment.queryStart;
				for(var qrySegRefNTIndex = ntQuerySegment.refStart; 
						qrySegRefNTIndex <= ntQuerySegment.refEnd; qrySegRefNTIndex++) {
					var ntColumn = qrySegRefNTIndex - minNTIndex;
					if(qrySegRefNTIndex == ntQuerySegment.refStart) {
						queryNTIndices[ntColumn] = qrySegQryNTIndex;
					} else if(qrySegRefNTIndex == ntQuerySegment.refEnd) {
						queryNTIndices[ntColumn] = qrySegQryNTIndex;
					}
					var indexInSeg = qrySegRefNTIndex - ntQuerySegment.refStart;
					queryNTs[ntColumn] = ntQuerySegment.nucleotides.charAt(indexInSeg);
					if(ntReferenceDiff && ntReferenceDiff.mask[indexInSeg] == "X") {
						queryNTDifferenceStyle[ntColumn] = "difference"
					}
					qrySegQryNTIndex++;
				}
			}
			// commented out until we sort out reporting variation categories through the UI
			/*
			if(ntVariationNoteArray) {
				for(var i = 0 ; i < ntVariationNoteArray.length; i++) {
					var ntVariationNote = ntVariationNoteArray[i];
					var variationDifferenceStyle = "difference_"+variationMap[ntVariationNote.variationName].notifiability;
					for(var variationNtIndex = ntVariationNote.refStart; variationNtIndex <= ntVariationNote.refEnd; variationNtIndex++) {
						var ntColumn = variationNtIndex - minNTIndex;
						queryNTDifferenceStyle[ntColumn] = updateDifferenceStyle(queryNTDifferenceStyle[ntColumn], variationDifferenceStyle);
					}
				}
			}
			*/
		}

		
		
		var minAAIndex;
		var maxAAIndex;
		var numAAs;
		var referenceAAIndices;
		var referenceAAs;
		var queryAAs;
		var minorityVariantAAs;
		var minorityVariantAAProportions;

		
		if(aaReferenceSegmentArray) {
			minAAIndex = aaReferenceSegmentArray[0].refStart;
			maxAAIndex = aaReferenceSegmentArray[aaReferenceSegmentArray.length-1].refEnd;
			numAAs = 1+(maxAAIndex - minAAIndex);
			referenceAAIndices = new Array(numAAs);
			referenceAAs = new Array(numAAs);
			queryAAs = new Array(numAAs);
			queryAADifferenceStyle = new Array(numAAs);
			minorityVariantAAs = new Array(numAAs);
			minorityVariantAAProportions = new Array(numAAs);

			for(var i = 0; i < numAAs; i++) {
				referenceAAIndices[i] = empty;
				referenceAAs[i] = empty;
				queryAAs[i] = empty;
				queryAADifferenceStyle[i] = "";
				minorityVariantAAs[i] = empty;
				minorityVariantAAProportions[i] = empty;
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
					
					var indexInSeg = refSegAAIndex - aaReferenceSegment.refStart;
					referenceAAs[aaColumn] = aaReferenceSegment.aminoAcids.charAt(indexInSeg);
					
					var mvAa = aaMinorityVariantMap[refSegAAIndex];
					if(mvAa) {
						minorityVariantAAs[aaColumn] = mvAa.aaValue;
						minorityVariantAAProportions[aaColumn] = empty+toFixed(100*mvAa.proportion,1);
					}
				}
			}
			if(aaQuerySegmentArray) {
				for(var segIndex = 0; segIndex < aaQuerySegmentArray.length; segIndex++) {
					var aaQuerySegment = aaQuerySegmentArray[segIndex];
					
					var aaReferenceDiff;
					if(aaReferenceDiffArray && segIndex < aaReferenceDiffArray.length) {
						var diff = aaReferenceDiffArray[segIndex];
						if(diff.refStart == aaQuerySegment.refStart && 
						   diff.refEnd == aaQuerySegment.refEnd) {
							aaReferenceDiff = diff;
						}
					}
					

					
					for(var qrySegAAIndex = aaQuerySegment.refStart; 
							qrySegAAIndex <= aaQuerySegment.refEnd; qrySegAAIndex++) {
						var aaColumn = qrySegAAIndex - minAAIndex;
						var indexInSeg = qrySegAAIndex - aaQuerySegment.refStart;
						queryAAs[aaColumn] = aaQuerySegment.aminoAcids.charAt(indexInSeg);
						if(aaReferenceDiff && aaReferenceDiff.mask[indexInSeg] == "X") {
							queryAADifferenceStyle[aaColumn] = "difference"
						}
					}
				}
				// commented out until we sort out reporting variation categories through the UI
				/* if(aaVariationNoteArray) {
					for(var i = 0 ; i < aaVariationNoteArray.length; i++) {
						var aaVariationNote = aaVariationNoteArray[i];
						var variationDifferenceStyle = "difference_"+variationMap[aaVariationNote.variationName].notifiability;
						for(var variationAaIndex = aaVariationNote.refStart; variationAaIndex <= aaVariationNote.refEnd; variationAaIndex++) {
							var aaColumn = variationAaIndex - minAAIndex;
							queryAADifferenceStyle[aaColumn] = updateDifferenceStyle(queryAADifferenceStyle[aaColumn], variationDifferenceStyle);
						}
					}
				} */

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
			analysisSequenceRow["queryNTDifferenceStyle"] = queryNTDifferenceStyle.slice(ntStart, ntEnd);

			if(aaReferenceSegmentArray) {
				var aaStart = row*aasPerRow;
				var aaEnd = (row+1)*aasPerRow;
				analysisSequenceRow["referenceAAIndices"] = referenceAAIndices.slice(aaStart, aaEnd);
				analysisSequenceRow["referenceAAs"] = referenceAAs.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAAs"] = queryAAs.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAADifferenceStyle"] = queryAADifferenceStyle.slice(aaStart, aaEnd);
				if(aaMinorityVariantArray) {
					analysisSequenceRow["minorityVariantAAs"] = minorityVariantAAs.slice(aaStart, aaEnd);
					analysisSequenceRow["minorityVariantAAProportions"] = minorityVariantAAProportions.slice(aaStart, aaEnd);
				}
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