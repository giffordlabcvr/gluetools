'use strict';

var submitSequencesAnalysis = angular.module('submitSequencesAnalysis', ['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main']);


submitSequencesAnalysis
.controller('submitSequencesAnalysisCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', function($scope, glueWS, FileUploader, dialogs) {

	$scope.selectFilesHeader = "Select sequence files";
	$scope.dropZoneText = "Drag sequence files here";
	$scope.browseAndSelectHeader = "Or browse and select multiple files";
	$scope.selectFilesButtonText = "Select files";
		

	$scope.fileItemUnderAnalysis = null;
	$scope.selectedSequenceAnalysis = null;
	$scope.selectedRefName = null;
	$scope.selectedFeatureAnalysis = null;
	$scope.selectedReferenceAnalysis = null;
	$scope.selectedRefSeqFeatAnalysis = null;
	$scope.selectedSeqFeatAnalysis = null;
	
	
	$scope.seqPrepDialog = function() {
		dialogs.create('hcvApp/dialogs/seqPrepDialog.html','seqPrepDialog',{},{});
	}

	$scope.updateSelectedRefSeqFeatAnalysis = function(){
		if($scope.selectedReferenceAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedRefSeqFeatAnalysis = _.find(
				$scope.selectedReferenceAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
			console.log("selected ref sequence feature analysis: ", $scope.selectedRefSeqFeatAnalysis);
		}
	}

	$scope.updateSelectedSeqFeatAnalysis = function(){
		if($scope.selectedSequenceAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedSeqFeatAnalysis = _.find(
				$scope.selectedSequenceAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
			console.log("selected seq feature analysis: ", $scope.selectedSeqFeatAnalysis);
		}
	}

	$scope.selectedSequenceAnalysisChanged = function(){
		if($scope.selectedSequenceAnalysis) {
		    $scope.selectedRefName = $scope.selectedSequenceAnalysis.ancestorRefName[0];
			console.log("selected ref name: ", $scope.selectedRefName);
		}
	}

	$scope.selectedFeatureAnalysisChanged = function(){
		if($scope.selectedFeatureAnalysis) {
			console.log("selected feature analysis: ", $scope.selectedFeatureAnalysis);
			$scope.updateSelectedRefSeqFeatAnalysis();
			$scope.updateSelectedSeqFeatAnalysis();
		}
	}

	$scope.selectedRefNameChanged = function(){
		if($scope.selectedRefName && $scope.fileItemUnderAnalysis) {
			$scope.selectedReferenceAnalysis = _.find(
					$scope.fileItemUnderAnalysis.webAnalysisResult.referenceAnalysis, 
					function(refAnalysis) {return refAnalysis.refName == $scope.selectedRefName;} );
			console.log("selected ref analysis: ", $scope.selectedReferenceAnalysis);
			$scope.updateSelectedRefSeqFeatAnalysis();
		}
	}

	
	$scope.$watch( 'selectedSequenceAnalysis', function(newObj, oldObj) {
		$scope.selectedSequenceAnalysisChanged();
	}, false);

	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
		$scope.selectedRefNameChanged();
	}, false);

	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		$scope.selectedFeatureAnalysisChanged();
	}, false);


	// invoked when "Analysis" button is pressed
	$scope.showAnalysisResults = function(item) {
		console.log("show analysis : ", item);
		$scope.fileItemUnderAnalysis = item;
		if($scope.fileItemUnderAnalysis.webAnalysisResult.sequenceAnalysis.length >= 0) {
			$scope.selectedSequenceAnalysis = $scope.fileItemUnderAnalysis.webAnalysisResult.sequenceAnalysis[0];
			console.log("selected seq analysis: ", $scope.selectedSequenceAnalysis);
		}
		if($scope.fileItemUnderAnalysis.webAnalysisResult.featureAnalysis.length >= 0) {
			$scope.selectedFeatureAnalysis = $scope.fileItemUnderAnalysis.webAnalysisResult.featureAnalysis[0];
			console.log("selected feature analysis: ", $scope.selectedFeatureAnalysis);
		}
	}

	$scope.selectGenomeFeature = function(selectedSequenceResult) {
		// remove popovers somehow
		var dlg = dialogs.create('hcvApp/dialogs/selectGenomeFeature.html','selectGenomeFeatureCtrl',$scope.selectedSequenceResult,{});
		dlg.result.then(function(feature){
			if(feature != $scope.selectedSequenceResult.selectedFeature) {
				$scope.selectedSequenceResult.selectedFeature = feature;
				$scope.updateSequenceAnalysis();
			}
		});
	}

	
	$scope.svgParams = {
			sequenceLabelWidth: 150,

			svgHeight: 300,

			ntWidth: 16,
			ntGap: 4,

			codonLabelHeight: 35,

			aaHeight: 25
	}
	
	$scope.svgHeight = function() {
		return $scope.codonLabelHeight() + $scope.referenceAaHeight();
	}
	$scope.svgWidth = function() {
		if($scope.selectedFeatureAnalysis) {
			var nts = ($scope.selectedFeatureAnalysis.endUIndex - $scope.selectedFeatureAnalysis.startUIndex) + 1;
			return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
		} else {
			return 0;
		}
	}
	$scope.sequenceLabelWidth = function(featureAnalysis) {
		return $scope.svgParams.sequenceLabelWidth;
	}
	
	$scope.referenceLabelX = function() {
		return 0;
	}
	$scope.referenceLabelY = function() {
		return $scope.codonLabelY() +$scope.codonLabelHeight();
	}
	$scope.referenceLabelWidth = function() {
		return $scope.svgParams.sequenceLabelWidth;
	}
	$scope.referenceLabelHeight = function() {
		return $scope.referenceAaHeight();
	}
	$scope.codonLabelX = function(codonLabel) {
		return (codonLabel.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	}
	$scope.codonLabelY = function() {
		return 0;
		// return $scope.codonLabelHeight();
	}
	$scope.codonLabelWidth = function(codonLabel) {
		var nts = (codonLabel.endUIndex - codonLabel.startUIndex) + 1;
		return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	}
	$scope.codonLabelHeight = function() {
		return $scope.svgParams.codonLabelHeight;
	}

	$scope.referenceAaX = function(referenceAa) {
		return (referenceAa.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	}
	$scope.referenceAaY = function() {
		return $scope.codonLabelY() + $scope.codonLabelHeight();
	}
	$scope.referenceAaWidth = function(referenceAa) {
		var nts = (referenceAa.endUIndex - referenceAa.startUIndex) + 1;
		return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	}
	$scope.referenceAaHeight = function() {
		return $scope.svgParams.aaHeight;
	}

	$scope.sequenceAaX = function(sequenceAa) {
		return (sequenceAa.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	}
	$scope.sequenceAaY = function() {
		return $scope.referenceAaY() + $scope.sequenceAaHeight();
	}
	$scope.sequenceAaWidth = function(sequenceAa) {
		var nts = (sequenceAa.endUIndex - sequenceAa.startUIndex) + 1;
		return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	}
	$scope.sequenceAaHeight = function() {
		return $scope.svgParams.aaHeight;
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

	// executed after the project URL is set
	glueWS.addProjectUrlListener( {
		reportProjectURL: function(projectURL) {

			// leaving this in as an example of a glue command.
			glueWS.runGlueCommand("", {
		    	list: { alignment: {} }
		    }).success(function(data, status, headers, config) {
				  console.info('result', data);
				  $scope.alignmentNames = tableResultGetColumn(data, "name");
				  console.info('alignmentNames', $scope.alignmentNames);
			}).
			error(glueWS.raiseErrorDialog(dialogs, "listing alignments"));

		    
		    $scope.uploader.url = projectURL+"/module/webAnalysisTool";

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
    uploader.onBeforeUploadItem = function(item) {
		var commandObject;
		commandObject = {
				"web-analysis": {
					// any args would go here.
				}
			};
    	item.formData = [{command: JSON.stringify(commandObject)}];
        console.info('formData', JSON.stringify(item.formData));
        console.info('onBeforeUploadItem', item);
    };
    uploader.onSuccessItem = function(fileItem, response, status, headers) {
        console.info('onSuccessItem', fileItem, response, status, headers);
        fileItem.webAnalysisResult = response.webAnalysisResult;
        
    };
    uploader.onErrorItem = function(fileItem, response, status, headers) {
        console.info('onErrorItem', fileItem, response, status, headers);
        var errorFn = glueWS.raiseErrorDialog(dialogs, "processing sequence file \""+fileItem.file.name+"\"");
        errorFn(response, status, headers, {});
    };

    // other callbacks
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
    uploader.onProgressItem = function(fileItem, progress) {
        console.info('onProgressItem', fileItem, progress);
    };
    uploader.onProgressAll = function(progress) {
        console.info('onProgressAll', progress);
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

}).controller('seqPrepDialog',function($scope,$modalInstance,data){
	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 
});




