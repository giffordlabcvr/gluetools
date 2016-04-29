'use strict';

var analysisTool = angular.module('analysisTool', 
		['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main', 'moduleURLs']);

analysisTool
.controller('analysisToolCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', 'moduleURLs',
    function($scope, glueWS, FileUploader, dialogs, moduleURLs) {

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
	$scope.analysisToolURL = moduleURLs.getAnalysisToolURL();
	
	$scope.seqPrepDialog = function() {
		dialogs.create($scope.analysisToolURL+'/dialogs/seqPrepDialog.html','seqPrepDialog',{},{});
	}

	$scope.updateSelectedRefSeqFeatAnalysis = function(){
		if($scope.selectedReferenceAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedRefFeatAnalysis = _.find(
				$scope.selectedReferenceAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
		} else {
			$scope.selectedRefFeatAnalysis = null;
		}
		console.log("updated ref feature analysis: ", $scope.selectedRefFeatAnalysis);
	}

	$scope.updateSelectedRefName = function(){
		if($scope.selectedQueryAnalysis != null) {
			if($scope.selectedRefName == null || $scope.selectedQueryAnalysis.ancestorRefName.indexOf($scope.selectedRefName) == -1) {
				// current reference is not an ancestor reference of new selected query
				$scope.selectedRefName = $scope.selectedQueryAnalysis.ancestorRefName[0];
			}
		} else {
			$scope.selectedRefName = null;
		}
		console.log("updated ref name: ", $scope.selectedRefName);
	}

	
	$scope.updateSelectedQueryFeatAnalysis = function(){
		if($scope.selectedQueryAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			$scope.selectedQueryFeatAnalysis = _.find(
				$scope.selectedQueryAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == $scope.selectedFeatureAnalysis.featureName;} );
		} else {
			$scope.selectedQueryFeatAnalysis = null;
		}
		console.log("updated query feature analysis: ", $scope.selectedQueryFeatAnalysis);
	}

	$scope.selectedQueryAnalysisChanged = function(){
		console.log("selected query analysis: ", $scope.selectedQueryAnalysis);
		$scope.updateSelectedQueryFeatAnalysis();
		$scope.updateSelectedRefName();
	}

	$scope.selectedFeatureAnalysisChanged = function(){
		console.log("selected feature analysis: ", $scope.selectedFeatureAnalysis);
		$scope.updateSelectedRefSeqFeatAnalysis();
		$scope.updateSelectedQueryFeatAnalysis();
	}

	$scope.selectedReferenceAnalysisChanged = function(){
		console.log("selected reference analysis: ", $scope.selectedReferenceAnalysis);
		$scope.updateSelectedRefSeqFeatAnalysis();
	}

	$scope.selectedRefNameChanged = function(){
		console.log("selected refName: ", $scope.selectedRefName);
		if($scope.fileItemUnderAnalysis) {
			$scope.selectedReferenceAnalysis = _.find(
					$scope.fileItemUnderAnalysis.webAnalysisResult.referenceAnalysis, 
					function(refAnalysis) {return refAnalysis.refName == $scope.selectedRefName;} );
		} else {
			$scope.selectedReferenceAnalysis = null;
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

	$scope.$watch( 'selectedReferenceAnalysis', function(newObj, oldObj) {
		$scope.selectedReferenceAnalysisChanged();
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
		var dlg = dialogs.create('dialogs/selectGenomeFeature.html','selectGenomeFeatureCtrl',$scope.selectedSequenceResult,{});
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
			aaHeight: 25,
			
			initAaProps: function(seqFeatAnalysis, featAnalysis) {
				var params = $scope.svgParams;
				return _.map(seqFeatAnalysis.aas, function(aa) {
	        		var nts = (aa.endUIndex - aa.startUIndex) + 1;
	    			var aaWidth = (nts * params.ntWidth) + ( (nts-1) * params.ntGap );
	    			var aaHeight = params.aaHeight;
		    		return {
		    			x: (aa.startUIndex - featAnalysis.startUIndex) * (params.ntWidth + params.ntGap),
		    			width: aaWidth,
		    			height: aaHeight,
		    			dx: aaWidth / 2.0,
		    			dy: aaHeight / 2.0,
		    			text: aa.aa
		    		};
		    	});
			},
			initNtSegProps: function(seqFeatAnalysis, featAnalysis) {
				var params = $scope.svgParams;
				return _.map(seqFeatAnalysis.nts, function(queryNtSeg) {
		    		var ntProps = _.map(queryNtSeg.nts, function(nt) {
		    			var ntWidth = params.ntWidth;
		    			var ntHeight = params.ntHeight;
	    				return {
			    			width: ntWidth,
			    			height: ntHeight,
			    			dx: ntWidth / 2.0,
			    			dy: ntHeight / 2.0,
	    					text: nt 
	    				};
	    			});
		    		for(var i = 0; i < ntProps.length; i++) {
		    			ntProps[i].x = ( (queryNtSeg.startUIndex + i) - featAnalysis.startUIndex) * 
		    				(params.ntWidth + params.ntGap);
		    		}
		    		return {
		    			ntProps: ntProps
		    		}
				});
			},
			codonLabelLineY: function() {
				return 0;
			},
			codonLabelLineHeight: function() {
				return $scope.svgParams.codonLabelHeight;
			},
			sequenceY: function(sequenceIndex) {
				var params = $scope.svgParams;
				var result =  
					params.codonLabelLineY() + 
					params.codonLabelLineHeight() + 
					(sequenceIndex * params.sequenceHeight());
				return result;
			},
			sequenceHeight: function() {
				var params = $scope.svgParams;
				return params.aaHeight + params.ntHeight;
			}
	}
	
	$scope.svgHeight = function() {
		var params = $scope.svgParams;
		return 
			params.codonLabelLineY() + 
			params.codonLabelLineHeight() + // codon label 
			params.sequenceHeight()+	// reference
			params.sequenceHeight();	// query
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
	
	console.log("init analysisTool controller");

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

})
;

