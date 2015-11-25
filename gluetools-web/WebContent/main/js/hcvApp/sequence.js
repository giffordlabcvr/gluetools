
function updateDifferenceStyle(prefix, oldDifferenceStyle, newDifferenceStyle) {
	if(newDifferenceStyle == prefix+"NOT_NOTIFIABLE" && oldDifferenceStyle == prefix+"NOTIFIABLE") {
		return oldDifferenceStyle;
	}
	return prefix+newDifferenceStyle;
}

function generateAlignmentDifferenceSummaries(variationCategories, sequenceResult) {
	var alignmentDifferenceSummaries = [];
	for(var x = 0; x < sequenceResult.sequenceAlignmentResult.length; x++) {
		// master alignment only.
		if(x == sequenceResult.sequenceAlignmentResult.length - 1) {
			var sequenceAlignmentResult = sequenceResult.sequenceAlignmentResult[x];
			var differenceSummariesForAlmtResult = 
				generateDifferenceSummariesForAlmtResult(variationCategories, sequenceAlignmentResult);
			for(var y = 0; y < differenceSummariesForAlmtResult.length; y++) {
				alignmentDifferenceSummaries.push(differenceSummariesForAlmtResult[y]);
			}
		}
	}
	return alignmentDifferenceSummaries;
}

function generateDifferenceSummariesForAlmtResult(variationCategories, sequenceAlignmentResult) {
	var differenceSummariesForAlmtResult = [];
	var firstDiffSummary = null;
	var lastDiffSummary = null;
	for(var i = 0; i < sequenceAlignmentResult.sequenceFeatureResult.length; i++) {
		var sequenceFeatureResult = sequenceAlignmentResult.sequenceFeatureResult[i];
		if(!sequenceFeatureResult.aaReferenceDifferenceNote) {
			continue;
		}
		var differenceSummary = {
			"featureName":sequenceFeatureResult.featureName,
			"referenceName":sequenceAlignmentResult.referenceName
		};
		lastDiffSummary = differenceSummary;
		var foundVariations = [];
		for(var j = 0; j < sequenceFeatureResult.aaReferenceDifferenceNote.length; j++) {
			aaReferenceDifferenceNote = sequenceFeatureResult.aaReferenceDifferenceNote[j];
			if(aaReferenceDifferenceNote.foundVariation) {
				for(var k = 0; k < aaReferenceDifferenceNote.foundVariation.length; k++) {
					var foundVariation = aaReferenceDifferenceNote.foundVariation[k];
					var differenceStyle = "differenceSummary";
					var popoverContent = "Uncommon / unknown variant";
					if(foundVariation.variationCategory) {
						popoverContent = "";
						for(var c = 0; c < foundVariation.variationCategory.length; c++) {
							var vcatName = foundVariation.variationCategory[c];
							differenceStyle = 
								updateDifferenceStyle("differenceSummary_", 
										differenceStyle,
										variationCategories[vcatName].inheritedNotifiability);
							popoverContent += variationCategories[vcatName].description + "\n";
						}
					}
					var displayFoundVar = {
							name: foundVariation.name,
							differenceStyle: differenceStyle,
							popoverContent: popoverContent};
					foundVariations.push(displayFoundVar);
				}
			}
		}
		if(foundVariations.length > 0) {
			if(firstDiffSummary == null) {
				firstDiffSummary = differenceSummary;
			}
			differenceSummary["foundVariations"] = foundVariations;
			differenceSummariesForAlmtResult.push(differenceSummary);
		}
	}
	if(firstDiffSummary) {
		firstDiffSummary["rowspan"] = differenceSummariesForAlmtResult.length;
	}
	if(lastDiffSummary) {
		lastDiffSummary["master"] = true;
	}
	
	return differenceSummariesForAlmtResult;
}

function generateAnalysisSequenceRows(variationCategories, feature, sequenceFeatureResult) {
	
	var analysisSequenceRows = [];
	
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
					if(ntReferenceDiff && ntReferenceDiff.mask[indexInSeg] != "-") {
						queryNTDifferenceStyle[ntColumn] = "difference"
					}
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
			queryAAPopover = new Array(numAAs);
			minorityVariantAAs = new Array(numAAs);
			minorityVariantAAProportions = new Array(numAAs);

			for(var i = 0; i < numAAs; i++) {
				referenceAAIndices[i] = empty;
				referenceAAs[i] = empty;
				queryAAs[i] = empty;
				queryAADifferenceStyle[i] = "";
				queryAAPopover[i] = null;
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
					
					
					
					var v = 0;
					var foundVariation;
					if(aaReferenceDiff.foundVariation) {
						foundVariation = aaReferenceDiff.foundVariation[v];
					}
					for(var qrySegAAIndex = aaQuerySegment.refStart; 
							qrySegAAIndex <= aaQuerySegment.refEnd; qrySegAAIndex++) {
						var aaColumn = qrySegAAIndex - minAAIndex;
						var indexInSeg = qrySegAAIndex - aaQuerySegment.refStart;
						queryAAs[aaColumn] = aaQuerySegment.aminoAcids.charAt(indexInSeg);
						if(aaReferenceDiff && aaReferenceDiff.mask[indexInSeg] != "-") {
							queryAADifferenceStyle[aaColumn] = "difference";
							queryAAPopover[aaColumn] = {"content":"Uncommon or unknown variant"};
							if(aaReferenceDiff.foundVariation) {
								while(v < aaReferenceDiff.foundVariation.length 
										&& foundVariation.refEnd < qrySegAAIndex) {
									v++;
									foundVariation = aaReferenceDiff.foundVariation[v];
								}
							}
							if(foundVariation && foundVariation.refStart <= qrySegAAIndex && foundVariation.refEnd >= qrySegAAIndex) {
								queryAAPopover[aaColumn]["title"] = foundVariation.name;
								if(foundVariation.variationCategory) {
									var popoverContent = "";
									for(var c = 0; c < foundVariation.variationCategory.length; c++) {
										var vcatName = foundVariation.variationCategory[c];
										queryAADifferenceStyle[aaColumn] = 
											updateDifferenceStyle("difference_", 
													queryAADifferenceStyle[aaColumn],
													variationCategories[vcatName].inheritedNotifiability);
										popoverContent += variationCategories[vcatName].description+"\n";
									}
									queryAAPopover[aaColumn]["content"] = popoverContent;
								}
							}
						}
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
			analysisSequenceRow["queryNTDifferenceStyle"] = queryNTDifferenceStyle.slice(ntStart, ntEnd);

			if(aaReferenceSegmentArray) {
				var aaStart = row*aasPerRow;
				var aaEnd = (row+1)*aasPerRow;
				analysisSequenceRow["referenceAAIndices"] = referenceAAIndices.slice(aaStart, aaEnd);
				analysisSequenceRow["referenceAAs"] = referenceAAs.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAAs"] = queryAAs.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAADifferenceStyle"] = queryAADifferenceStyle.slice(aaStart, aaEnd);
				analysisSequenceRow["queryAAPopover"] = queryAAPopover.slice(aaStart, aaEnd);
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