'use strict';

var submitSequencesAnalysis = angular.module('submitSequencesAnalysis', ['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main']);


submitSequencesAnalysis
.controller('submitSequencesAnalysisCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', function($scope, glueWS, FileUploader, dialogs) {
	$scope.pageTitle = "Submit sequences for analysis";
	$scope.pageExplanation = "Submit sequences for analysis of mutations at the amino-acid level.";

	$scope.selectFilesHeader = "Select sequence files";
	$scope.dropZoneText = "Drag sequence files here";
	$scope.browseAndSelectHeader = "Or browse and select multiple files";
	$scope.selectFilesButtonText = "Select files";
	$scope.autoDetectFormat = "Auto-detect"
	$scope.headerDetectAlmtName = "Header-detect"
		
	$scope.sequenceFormats = [];
	$scope.alignmentNames = [];

	$scope.showSupportedFormats = function() {
		dialogs.create('hcvApp/dialogs/seqFmtDialog.html','seqFmtDialogCtrl',$scope.sequenceFormats,{});
	}
	
	$scope.showAnalysisResults = function(item) {
		console.log("show analysis : ", item);
		console.log("resultArray : ", item.transientAnalysisResult.sequenceResult);
		$scope.analysisResults = item;
	}
	
	$scope.findFeature = function(featureTree, featureName) {
		if(featureTree.featureName == featureName) {
			return featureTree;
		}
		if(featureTree.features) {
			for(var i = 0; i < featureTree.features.length; i++) {
				var chResult = $scope.findFeature(featureTree.features[i], featureName); 
				if(chResult != null) {
					return chResult;
				}
			}
		}
		return null;
	}
	
	$scope.showAlignmentDetails = function(sequenceResult) {
		dialogs.create('hcvApp/dialogs/alignmentDetails.html','alignmentDetailsCtrl',sequenceResult,{});
	}
	
	
	$scope.analysisResultsCtrl = function($scope) {
		console.log('created controller for ', $scope.sequenceResult);

		console.log('refToFeatureTreeMap:', $scope.sequenceResult.refToFeatureTreeMap);
		
		$scope.nullifyFeatureOnRefChange = true;
		$scope.referenceOptions = [];
		$scope.noneSelected = "None selected";
		$scope.numReferenceOptions = 0;
		$scope.sequenceResult.showNucleotides = "show";
		$scope.sequenceResult.showMinorityVariants = "show";
		$scope.sequenceResult.differenceView = "differenceSummary";
		$scope.sequenceResult.selectedFeature = {
				featureDescription: $scope.noneSelected
		};
		
		var refToFeatureTreeMap = $scope.sequenceResult.refToFeatureTreeMap;

		$scope.updateSequenceAnalysis = function() {
			if($scope.sequenceResult.differenceView == "differenceSummary") {
				$scope.sequenceResult.alignmentDifferenceSummaries = 
					generateAlignmentDifferenceSummaries($scope.variationCategories, $scope.sequenceResult);
				console.log("alignmentDifferenceSummaries ", $scope.sequenceResult.alignmentDifferenceSummaries);
			} else {
				console.log("sequenceResult ", $scope.sequenceResult);
				$scope.sequenceResult.analysisSequenceRows = [];
				if($scope.sequenceResult.selectedFeature.featureDescription == $scope.noneSelected) {
					return;
				}
				var sequenceFeatureResult;
				var sequenceAlignmentResult;
				for(var i = 0; i < $scope.sequenceResult.sequenceAlignmentResult.length; i++) {
					if($scope.sequenceResult.sequenceAlignmentResult[i].referenceName == $scope.sequenceResult.selectedReference) {
						sequenceAlignmentResult = $scope.sequenceResult.sequenceAlignmentResult[i];
					}
				}
				console.log("sequenceAlignmentResult ", sequenceAlignmentResult);
				var sequenceFeatureResult;
				for(var i = 0; i < sequenceAlignmentResult.sequenceFeatureResult.length; i++) {
					if(sequenceAlignmentResult.sequenceFeatureResult[i].featureName == $scope.sequenceResult.selectedFeature.featureName) {
						sequenceFeatureResult = sequenceAlignmentResult.sequenceFeatureResult[i];
					}
				}
				console.log("sequenceFeatureResult ", sequenceFeatureResult);
				$scope.sequenceResult.analysisSequenceRows = generateAnalysisSequenceRows(
						$scope.variationCategories,
						$scope.sequenceResult.selectedFeature, 
						sequenceFeatureResult);
				console.log("analysisSequenceRows ", $scope.sequenceResult.analysisSequenceRows);
			}
		}

		$scope.updateSequenceAnalysis();

		$scope.switchToDetailView = function(referenceName, featureName) {
			$scope.nullifyFeatureOnRefChange = false;
			$scope.sequenceResult.selectedReference = referenceName;
			$scope.sequenceResult.selectedFeature = $scope.findFeature(refToFeatureTreeMap[referenceName], featureName);
			console.log('updated feature to:', $scope.sequenceResult.selectedFeature);
			$scope.sequenceResult.differenceView = "genomeDetail";
		}

		$scope.$watch( 'sequenceResult.differenceView', function( newObj, oldObj ) {
			$scope.updateSequenceAnalysis();
		}, false);

		
		
		// for now, only show the master reference.
		for(var i = 0; i < $scope.sequenceResult.sequenceAlignmentResult.length; i++) {
			var sequenceAlignmentResult = $scope.sequenceResult.sequenceAlignmentResult[i];
			if(refToFeatureTreeMap[sequenceAlignmentResult.referenceName].features
					&& i == $scope.sequenceResult.sequenceAlignmentResult.length-1) {
				$scope.numReferenceOptions ++;
				$scope.referenceOptions.push(sequenceAlignmentResult.referenceName);
			}
		}
		if($scope.numReferenceOptions != 1) {
			// add to front.
			$scope.referenceOptions.unshift($scope.noneSelected);
		}
		
		$scope.$watch( 'sequenceResult.selectedReference', function( newObj, oldObj ) {
			console.log("selectedReference for "+$scope.sequenceResult.sequenceID+" updated to: ", newObj);
			if(newObj != $scope.noneSelected) {
				$scope.sequenceResult.featureTreeResult = $scope.sequenceResult.refToFeatureTreeMap[newObj].features;
			} else {
				$scope.sequenceResult.featureTreeResult = null;
			}
			if($scope.nullifyFeatureOnRefChange) {
				$scope.sequenceResult.selectedFeature = {
						featureDescription: $scope.noneSelected
				};
				$scope.updateSequenceAnalysis();
			}
			$scope.nullifyFeatureOnRefChange = true;
		}, false);
		
		if($scope.numReferenceOptions == 1) {
			$scope.sequenceResult.selectedReference = $scope.referenceOptions[0]; 
		} else {
			$scope.sequenceResult.selectedReference = $scope.noneSelected;
		}
		
		$scope.selectGenomeFeature = function(sequenceResult) {
			var dlg = dialogs.create('hcvApp/dialogs/selectGenomeFeature.html','selectGenomeFeatureCtrl',$scope.sequenceResult,{});
			dlg.result.then(function(feature){
				if(feature != $scope.sequenceResult.selectedFeature) {
					$scope.sequenceResult.selectedFeature = feature;
					$scope.updateSequenceAnalysis();
				}
			});
		}

		$scope.selectVariationCategories = function() {
			var dlg = dialogs.create('hcvApp/dialogs/selectVariationCategories.html','selectVariationCategoriesCtrl',$scope.variationCategories,{});
			dlg.result.then(function(updatedCategories) {
				$scope.variationCategories = updatedCategories;
				$scope.updateSequenceAnalysis();
			});
		}

		
	}
	
	
	$scope.removeAll = function() {
		$scope.uploader.clearQueue();
		$scope.analysisResults = null;
	}

	$scope.removeItem = function(item) {
		if($scope.analysisResults == item) {
			$scope.analysisResults = null;
		}
		item.remove();
	}

	addUtilsToScope($scope);
	
	console.log("init submitSequencesAnalysis controller");

	var uploader = $scope.uploader = new FileUploader({});

	glueWS.addProjectUrlListener( {
		reportProjectURL: function(projectURL) {
		    $scope.uploader.url = projectURL+"/module/mutationFrequencies";
		    
		    glueWS.runGlueCommand("", {
		    	list: { format : { sequence: {} } }
		    }).success(function(data, status, headers, config) {
				  console.info('result', data);
				  $scope.sequenceFormats = tableResultAsObjectList(data);
				  console.info('sequenceFormats', $scope.sequenceFormats);
			}).
			error(glueWS.raiseErrorDialog(dialogs, "listing sequence formats"));

		    glueWS.runGlueCommand("", {
		    	list: { "variation-category": {} }
		    }).success(function(data, status, headers, config) {
				  console.info('result', data);
				  var vcatList = tableResultAsObjectList(data);

				  $scope.variationCategories = {};
				  _.each(vcatList, function(vcatObj, idx, list) { $scope.variationCategories[vcatObj.name] = vcatObj; } ); 
				  console.info('variationCategories', $scope.variationCategories);
			}).
			error(glueWS.raiseErrorDialog(dialogs, "listing variation-categories"));

		    glueWS.runGlueCommand("", {
		    	list: { alignment: {} }
		    }).success(function(data, status, headers, config) {
				  console.info('result', data);
				  $scope.alignmentNames = tableResultGetColumn(data, "name");
				  console.info('alignmentNames', $scope.alignmentNames);
			}).
			error(glueWS.raiseErrorDialog(dialogs, "listing alignments"));
		    console.info('uploader', uploader);
		}
	});
	
    uploader.filters.push({
        name: 'customFilter',
        fn: function(item /*{File|FileLikeObject}*/, options) {
            return this.queue.length < 200;
        }
    });

    // CALLBACKS

    uploader.onWhenAddingFileFailed = function(item /*{File|FileLikeObject}*/, filter, options) {
        console.info('onWhenAddingFileFailed', item, filter, options);
    };
    uploader.onAfterAddingFile = function(fileItem) {
        fileItem.alignmentName = $scope.headerDetectAlmtName;
        console.info('onAfterAddingFile', fileItem);
    };
    uploader.onAfterAddingAll = function(addedFileItems) {
        console.info('onAfterAddingAll', addedFileItems);
    };
    uploader.onBeforeUploadItem = function(item) {
		var commandObject;
		if(item.alignmentName == $scope.headerDetectAlmtName) {
			commandObject = {
					"transient": {
						analysis: {
							headerDetect: true
						}
					}
				};
			
		} else {
			commandObject = {
					"transient": {
						analysis: {
							alignmentName: item.alignmentName,
							headerDetect: false
						}
					}
				};
		}
    	item.formData = [{command: JSON.stringify(commandObject)}];
        console.info('formData', JSON.stringify(item.formData));
        console.info('onBeforeUploadItem', item);
    };
    uploader.onProgressItem = function(fileItem, progress) {
        console.info('onProgressItem', fileItem, progress);
    };
    uploader.onProgressAll = function(progress) {
        console.info('onProgressAll', progress);
    };
    uploader.onSuccessItem = function(fileItem, response, status, headers) {
        console.info('onSuccessItem', fileItem, response, status, headers);
        fileItem.transientAnalysisResult = response.transientAnalysisResult;
        // create a map from the ref results.
        var refToFeatureTreeMap = {};
        var alignmentResultArray = fileItem.transientAnalysisResult.alignmentResult;
        for(var i = 0; i < alignmentResultArray.length; i++) {
        	refToFeatureTreeMap[alignmentResultArray[i].referenceName] = alignmentResultArray[i].featureTreeResult;
        }
        var seqResultArray = fileItem.transientAnalysisResult.sequenceResult;
        for(var i = 0; i < seqResultArray.length; i++) {
            // add a reference to the ref results for each seqResult.
        	seqResultArray[i].refToFeatureTreeMap = refToFeatureTreeMap;
        }
        
    };
    uploader.onErrorItem = function(fileItem, response, status, headers) {
        console.info('onErrorItem', fileItem, response, status, headers);
        var errorFn = glueWS.raiseErrorDialog(dialogs, "processing sequence file \""+fileItem.file.name+"\"");
        errorFn(response, status, headers, {});
    };
    uploader.onCancelItem = function(fileItem, response, status, headers) {
        console.info('onCancelItem', fileItem, response, status, headers);
    };
    uploader.onCompleteItem = function(fileItem, response, status, headers) {
        console.info('onCompleteItem', fileItem, response, status, headers);
    };
    uploader.onCompleteAll = function() {
        console.info('onCompleteAll');
    };
}])
.controller('seqFmtDialogCtrl',function($scope,$modalInstance,data){
	$scope.sequenceFormats = data;
	
	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 
})
.controller('alignmentDetailsCtrl',function($scope,$modalInstance,data){
	$scope.sequenceResult = data;
	addUtilsToScope($scope);
	
	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 
})
.controller('selectGenomeFeatureCtrl',function($scope,$modalInstance,data){
	$scope.sequenceResult = data;
	$scope.defaultOpenDepth = 99;
	$scope.defaultSelectedId = data.selectedFeature.featureName;
	addUtilsToScope($scope);
	
	console.log("select genome feature, sequenceResult", $scope.sequenceResult)
	
	$scope.select = function(selectedNode){
		console.log("selectedNode", selectedNode);
		$modalInstance.close(selectedNode);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

})
.controller('selectVariationCategoriesCtrl',function($scope,$modalInstance,data){
	$scope.variationCategories = data;
	$scope.included = [];
	$scope.excluded = [];
	addUtilsToScope($scope);

	console.log("variation categories: ", $scope.variationCategories);
	
	_.each($scope.variationCategories, function(vcat, key, list) {
		if(vcat.excluded && vcat.excluded == true) {
			console.log("excluded vcat: ", vcat.name);
			$scope.excluded.push(vcat);
		} else {
			console.log("included vcat: ", vcat.name);
			$scope.included.push(vcat);
		}
	});
	
	$scope.include = function(vcat) {
		$scope.excluded = _.without($scope.excluded, vcat);
		$scope.included.push(vcat);
	}

	$scope.exclude = function(vcat) {
		$scope.included = _.without($scope.included, vcat);
		$scope.excluded.push(vcat);
	}

	$scope.select = function(){
		var updatedCategories = {};
		_.each($scope.included, function(vcat, idx, list) {
			vcat.excluded = false;
			updatedCategories[vcat.name] = vcat;
		});
		_.each($scope.excluded, function(vcat, idx, list) {
			vcat.excluded = true;
			updatedCategories[vcat.name] = vcat;
		});
		console.log("updatedCategories: ", updatedCategories);
		$modalInstance.close(updatedCategories);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

});





