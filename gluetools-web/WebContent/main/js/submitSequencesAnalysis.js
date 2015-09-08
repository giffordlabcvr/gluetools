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
		dialogs.create('dialogs/seqFmtDialog.html','seqFmtDialogCtrl',$scope.sequenceFormats,{});
	}
	
	$scope.showAnalysisResults = function(item) {
		console.log("show analysis : ", item);
		console.log("resultArray : ", item.transientAnalysisResult.sequenceResult);
		$scope.analysisResults = item;
	}
	
	$scope.showAlignmentDetails = function(sequenceResult) {
		dialogs.create('dialogs/alignmentDetails.html','alignmentDetailsCtrl',sequenceResult,{});
	}
	
	
	$scope.analysisResultsCtrl = function($scope) {
		console.log('created controller for ', $scope.sequenceResult);

		$scope.alignmentsWithResults = {};
		$scope.alignmentOptions = [];
		$scope.noneSelected = "None selected";
		$scope.numAlignmentsWithResults = 0;
		for(var i = 0; i < $scope.sequenceResult.alignmentAnalysis.length; i++) {
			var alignmentAnalysis = $scope.sequenceResult.alignmentAnalysis[i];
			if(alignmentAnalysis.featureAnalysisTree) {
				$scope.alignmentsWithResults[alignmentAnalysis.alignmentName] = alignmentAnalysis;
				$scope.numAlignmentsWithResults ++;
				$scope.alignmentOptions.push(alignmentAnalysis.alignmentName);
			}
		}
		if($scope.numAlignmentsWithResults != 1) {
			$scope.alignmentOptions.unshift($scope.noneSelected);
		}
		
		$scope.$watch( 'selectedAlignment', function( newObj, oldObj ) {
			console.log("selectedAlignment for "+$scope.sequenceResult.sequenceID+" updated to: ", newObj);
			if(newObj != $scope.noneSelected) {
				$scope.sequenceResult.featureAnalysisTree = $scope.alignmentsWithResults[newObj].featureAnalysisTree;
			} else {
				$scope.sequenceResult.featureAnalysisTree = null;
			}
			$scope.sequenceResult.selectedFeature = $scope.noneSelected;
		}, false);
		
		if($scope.numAlignmentsWithResults == 1) {
			$scope.selectedAlignment = _.pairs($scope.alignmentsWithResults)[0][0]; 
		} else {
			$scope.selectedAlignment = $scope.noneSelected;
		}
		
		$scope.selectGenomeFeature = function(sequenceResult) {
			var dlg = dialogs.create('dialogs/selectGenomeFeature.html','selectGenomeFeatureCtrl',$scope.sequenceResult,{});
			dlg.result.then(function(featureName){
				$scope.sequenceResult.selectedFeature = featureName;
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
            return this.queue.length < 10;
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
	$scope.defaultOpenDepth = 2;
	$scope.defaultSelectedId = data.selectedFeature;
	addUtilsToScope($scope);
	
	console.log("select genome feature, sequenceResult", $scope.sequenceResult)
	
	$scope.select = function(selectedNode){
		console.log("selectedNode", selectedNode);
		$modalInstance.close(selectedNode.featureName);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

});




