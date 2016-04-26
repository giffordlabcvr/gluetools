'use strict';

var submitSequencesAnalysis = angular.module('submitSequencesAnalysis', ['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main']);


submitSequencesAnalysis
.controller('submitSequencesAnalysisCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', function($scope, glueWS, FileUploader, dialogs) {

	$scope.selectFilesHeader = "Select sequence files";
	$scope.dropZoneText = "Drag sequence files here";
	$scope.browseAndSelectHeader = "Or browse and select multiple files";
	$scope.selectFilesButtonText = "Select files";
		

	$scope.fileItemUnderAnalysis = null;
	$scope.selectedQueryAnalysis = null;
	$scope.selectedRefName = null;
	$scope.selectedFeatureAnalysis = null;
	$scope.selectedReferenceAnalysis = null;
	$scope.selectedRefFeatAnalysis = null;
	$scope.selectedQueryFeatAnalysis = null;
	
	
	$scope.seqPrepDialog = function() {
		dialogs.create('hcvApp/dialogs/seqPrepDialog.html','seqPrepDialog',{},{});
	}

	$scope.updateSelectedRefSeqFeatAnalysis = function(){
		if($scope.selectedReferenceAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedRefFeatAnalysis = _.find(
				$scope.selectedReferenceAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
			console.log("selected ref feature analysis: ", $scope.selectedRefFeatAnalysis);
		}
	}

	$scope.updateSelectedQueryFeatAnalysis = function(){
		if($scope.selectedQueryAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedQueryFeatAnalysis = _.find(
				$scope.selectedQueryAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
			console.log("selected query feature analysis: ", $scope.selectedQueryFeatAnalysis);
		}
	}

	$scope.selectedQueryAnalysisChanged = function(){
		if($scope.selectedQueryAnalysis) {
		    $scope.selectedRefName = $scope.selectedQueryAnalysis.ancestorRefName[0];
			console.log("selected query analysis: ", $scope.selectedQueryAnalysis);
		}
	}

	$scope.selectedFeatureAnalysisChanged = function(){
		if($scope.selectedFeatureAnalysis) {
			console.log("selected feature analysis: ", $scope.selectedFeatureAnalysis);
			$scope.updateSelectedRefSeqFeatAnalysis();
			$scope.updateSelectedQueryFeatAnalysis();
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

	
	$scope.$watch( 'selectedQueryAnalysis', function(newObj, oldObj) {
		$scope.selectedQueryAnalysisChanged();
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
		if($scope.fileItemUnderAnalysis.webAnalysisResult.queryAnalysis.length >= 0) {
			$scope.selectedQueryAnalysis = $scope.fileItemUnderAnalysis.webAnalysisResult.queryAnalysis[0];
			console.log("selected query analysis: ", $scope.selectedQueryAnalysis);
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
		});
	}
	
	$scope.svgParams = {
			sequenceLabelWidth: 150,
			svgHeight: 300,
			ntWidth: 16,
			ntHeight: 16,
			ntGap: 4,
			codonLabelHeight: 35,
			aaHeight: 25
	}
	
	$scope.svgHeight = function() {
		return 
			$scope.svgParams.codonLabelHeight + // codon label 
			$scope.svgParams.aaHeight + 		// reference AA
			$scope.svgParams.ntHeight + 		// reference NT
			$scope.svgParams.aaHeight + 		// query AA
			$scope.svgParams.ntHeight;			// query NT
	}
	$scope.svgWidth = function() {
		if($scope.selectedFeatureAnalysis) {
			var nts = ($scope.selectedFeatureAnalysis.endUIndex - $scope.selectedFeatureAnalysis.startUIndex) + 1;
			return (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
		} else {
			return 0;
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
.directive('codonLabel', function() {
	  return {
	    restrict: 'E',
	    controller: function($scope) {
	    	$scope.x = ($scope.codonLabel.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
	    			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	    	$scope.y = 0;
    		var nts = ($scope.codonLabel.endUIndex - $scope.codonLabel.startUIndex) + 1;
	    	$scope.width = (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	    	$scope.height = $scope.svgParams.codonLabelHeight;
	    	$scope.dx = $scope.width / 2.0;
	    	$scope.dy = $scope.height / 2.0;
	    },
	    replace: true,
	    scope: {
	      codonLabel: '=',
	      svgParams: '=',
	      selectedFeatureAnalysis: '=',
	    },
	    templateUrl: 'hcvApp/views/codonLabel.html'
	  };
	})
.directive('referenceAa', function() {
	  return {
	    restrict: 'E',
	    controller: function($scope) {
	    	$scope.x = ($scope.referenceAa.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
	    			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	    	$scope.y = $scope.svgParams.codonLabelHeight;
    		var nts = ($scope.referenceAa.endUIndex - $scope.referenceAa.startUIndex) + 1;
	    	$scope.width = (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	    	$scope.height = $scope.svgParams.aaHeight;
	    	$scope.dx = $scope.width / 2.0;
	    	$scope.dy = $scope.height / 2.0;
	    },
	    replace: true,
	    scope: {
	      referenceAa: '=',
	      svgParams: '=',
	      selectedFeatureAnalysis: '=',
	    },
	    templateUrl: 'hcvApp/views/referenceAa.html'
	  };
	})
.directive('referenceNt', function() {
	  return {
	    restrict: 'E',
	    controller: function($scope) {
	    	$scope.x = ($scope.referenceNt.uIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
	    			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	    	$scope.y = $scope.svgParams.codonLabelHeight + $scope.svgParams.aaHeight;
	    	$scope.width = $scope.svgParams.ntWidth;
	    	$scope.height = $scope.svgParams.ntHeight;
	    	$scope.dx = $scope.width / 2.0;
	    	$scope.dy = $scope.height / 2.0;
	    },
	    replace: true,
	    scope: {
	      referenceNt: '=',
	      svgParams: '=',
	      selectedFeatureAnalysis: '=',
	    },
	    templateUrl: 'hcvApp/views/referenceNt.html'
	  };
	})
.directive('queryAa', function() {
	  return {
	    restrict: 'E',
	    controller: function($scope) {
	    	$scope.x = ($scope.queryAa.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
	    			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	    	$scope.y = $scope.svgParams.codonLabelHeight + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight;
    		var nts = ($scope.queryAa.endUIndex - $scope.queryAa.startUIndex) + 1;
	    	$scope.width = (nts * $scope.svgParams.ntWidth) + ( (nts-1) * $scope.svgParams.ntGap );
	    	$scope.height = $scope.svgParams.aaHeight;
	    	$scope.dx = $scope.width / 2.0;
	    	$scope.dy = $scope.height / 2.0;


	    	$scope.updateDiffs = function() {
		    	if($scope.queryAa.referenceDiffs && $scope.queryAa.referenceDiffs.indexOf($scope.selectedRefName) != -1) {
		    		$scope.textClass = "queryAaDiff";
		    		$scope.textBgClass = "queryAaDiffBackground";
		    	} else {
		    		$scope.textClass = "queryAa";
		    		$scope.textBgClass = "queryAaBackground";
		    	}
	    	};
	    	
	    	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
	    		$scope.updateDiffs();
	    	}, false);

	    	$scope.updateDiffs();
	    	
	    },
	    replace: true,
	    scope: {
	      queryAa: '=',
	      svgParams: '=',
	      selectedFeatureAnalysis: '=',
	      selectedRefName: '=',
	    },
	    templateUrl: 'hcvApp/views/queryAa.html'
	  };
	})
.directive('queryNt', function() {
	  return {
	    restrict: 'E',
	    controller: function($scope) {
	    	$scope.x = ($scope.queryNt.uIndex - $scope.selectedFeatureAnalysis.startUIndex) * 
	    			($scope.svgParams.ntWidth + $scope.svgParams.ntGap);
	    	$scope.y = $scope.svgParams.codonLabelHeight + 
		    	$scope.svgParams.aaHeight+
		    	$scope.svgParams.ntHeight+
		    	$scope.svgParams.aaHeight;
	    	$scope.width = $scope.svgParams.ntWidth;
	    	$scope.height = $scope.svgParams.ntHeight;
	    	$scope.dx = $scope.width / 2.0;
	    	$scope.dy = $scope.height / 2.0;
	    },
	    replace: true,
	    scope: {
	      queryNt: '=',
	      svgParams: '=',
	      selectedFeatureAnalysis: '=',
	    },
	    templateUrl: 'hcvApp/views/queryNt.html'
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

}).controller('seqPrepDialog',function($scope,$modalInstance,data){
	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 
});



