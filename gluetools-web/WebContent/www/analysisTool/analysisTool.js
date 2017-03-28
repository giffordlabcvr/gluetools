'use strict';

console.log("before analysisTool module definition");


var userAgent = detect.parse(navigator.userAgent);

console.log("userAgent.browser.family", userAgent.browser.family);
console.log("userAgent.browser.name", userAgent.browser.name);
console.log("userAgent.browser.version", userAgent.browser.version);


var analysisTool = angular.module('analysisTool', 
		['angularFileUpload', 'glueWS', 'ui.bootstrap','dialogs.main', 'glueWebToolConfig',
		    'angulartics',
		    'angulartics.google.analytics']);

console.log("after analysisTool module definition");


analysisTool.controller('analysisToolCtrl', [ '$scope', 'glueWS', 'FileUploader', 'dialogs', 'glueWebToolConfig', '$analytics',
    function($scope, glueWS, FileUploader, dialogs, glueWebToolConfig, $analytics) {

	addUtilsToScope($scope);

	$scope.analytics = $analytics;
	$scope.analysisToolURL = glueWebToolConfig.getAnalysisToolURL();
	$scope.analysisModuleName = glueWebToolConfig.getAnalysisModuleName()

	if($scope.analysisModuleName == null) {
		$scope.analysisModuleName = "webAnalysisTool";
	}
	
	$scope.variationsPerRow = 8;
	$scope.range = function(n) {
		return new Array(n);
	}

	$scope.resetSelections = function() {
		$scope.fileItemUnderAnalysis = null;
		$scope.selectedQueryAnalysis = null;
		$scope.selectedRefName = null;
		$scope.selectedFeatureAnalysis = null;
		$scope.selectedReferenceAnalysis = null;
		$scope.selectedRefFeatAnalysis = null;
		$scope.selectedQueryFeatAnalysis = null;
		$scope.selectedVariationCategoryResult = null;
		$scope.resultVariationMatchesPresent = null;
		$scope.resultVariationMatchesAbsent = null;
	}
	
	$scope.analysisView = 'typingSummary';
	$scope.enableVariationSummary = true;

	$scope.nonZeroCladeWeighting = function(cladeWeighting) {
		return cladeWeighting.percentScore >= 0.01;
	}
	
	$scope.updateSelectedRefSeqFeatAnalysis = function(){
		if($scope.selectedReferenceAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			var contentFeatureName;
			if($scope.selectedFeatureAnalysis.includesSequenceContent) {
				contentFeatureName = $scope.selectedFeatureAnalysis.featureName;
			} else {
				contentFeatureName = $scope.selectedFeatureAnalysis.deriveSequenceAnalysisFrom;
			}
			$scope.selectedRefFeatAnalysis = _.find(
				$scope.selectedReferenceAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == contentFeatureName;} );
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

	$scope.updateSelectedVariationCategoryResult = function(){
		if($scope.selectedVariationCategoryResult == null &&
				$scope.selectedQueryAnalysis != null && $scope.fileItemUnderAnalysis && 
				$scope.fileItemUnderAnalysis.webAnalysisResult.variationCategoryResult &&
				$scope.fileItemUnderAnalysis.webAnalysisResult.variationCategoryResult.length > 0) {
			$scope.selectedVariationCategoryResult = $scope.fileItemUnderAnalysis.webAnalysisResult.variationCategoryResult[0];
		} 
		console.log("updated selected variationCategoryResult: ", $scope.selectedVariationCategoryResult);
	}
	
	

	$scope.displayVariation = function(vCatName, referenceName, featureName, variationName, pLocMatches) {
		var varVCat = _.find(
				$scope.variationCategories, 
				function(vCat) {
					return vCat.name == vCatName; } );

		var variationRendererModuleName = varVCat.objectRendererModule;

		var variationRendererDialog = _.find(glueWebToolConfig.getRendererDialogs(), 
				function(rendererDialog) { return rendererDialog.renderer == variationRendererModuleName;});

		console.info('variationRendererDialog', variationRendererDialog);

		var glueVariationPath = 
			"reference/"+referenceName+
			"/feature-location/"+featureName+
			"/variation/"+variationName;
		glueWS.runGlueCommand(glueVariationPath, {
	    	"render-object": { "rendererModuleName": variationRendererModuleName } 
		})
	    .success(function(data, status, headers, config) {
			  console.info('render result', data);
	    		var dlg = dialogs.create(variationRendererDialog.dialogURL,
	    				variationRendererDialog.dialogController, 
	    				{ renderedVariation: data,
	    				  variationCategory: varVCat, 
	    				  ancestorAlmtNames: _.uniq($scope.selectedQueryAnalysis.ancestorAlmtName),
	    				  pLocMatches: pLocMatches
	    				}, {});
	    		dlg.result.then(function() {
	    			// completion handler
	    		}, function() {
	    		    // Error handler
	    		}).finally(function() {
	    		    // Finally handler
	    		});
	    })
	    .error(glueWS.raiseErrorDialog(dialogs, "rendering variation "+variationName));
	}

	
	$scope.updateSelectedQueryFeatAnalysis = function(){
		if($scope.selectedQueryAnalysis != null && $scope.selectedFeatureAnalysis != null) {
			var contentFeatureName;
			if($scope.selectedFeatureAnalysis.includesSequenceContent) {
				contentFeatureName = $scope.selectedFeatureAnalysis.featureName;
			} else {
				contentFeatureName = $scope.selectedFeatureAnalysis.deriveSequenceAnalysisFrom;
			}
			$scope.selectedQueryFeatAnalysis = _.find(
				$scope.selectedQueryAnalysis.sequenceFeatureAnalysis, 
				function(seqFeatureAnalysis) {
					return seqFeatureAnalysis.featureName == contentFeatureName; } );
		} else {
			$scope.selectedQueryFeatAnalysis = null;
		}
		console.log("updated query feature analysis: ", $scope.selectedQueryFeatAnalysis);
	}

	$scope.selectedQueryAnalysisChanged = function(){
		console.log("selected query analysis: ", $scope.selectedQueryAnalysis);
		$scope.updateSelectedQueryFeatAnalysis();
		$scope.updateSelectedRefName();
		$scope.updateSelectedVariationCategoryResult();
		$scope.updateResultVariationMatchesPresent();
		$scope.updateResultVariationMatchesAbsent();
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
	
	$scope.analysisViewChanged = function() {
	}
	
	$scope.switchToFeatureDetail = function(refName, featureName) {
		$scope.selectedRefName = refName;
		$scope.selectedFeatureAnalysis = _.find(
				$scope.fileItemUnderAnalysis.webAnalysisResult.featureAnalysis, 
				function(featureAnalysis) {return featureAnalysis.featureName == featureName;} );
		$scope.analysisView = 'genomeDetail';
	}
	
	$scope.selectedVariationCategoryResultChanged = function() {
		$scope.updateResultVariationMatchesPresent();
		$scope.updateResultVariationMatchesAbsent();
	}

	$scope.updateResultVariationMatchesAbsent = function() {
		if($scope.selectedVariationCategoryResult != null &&
				$scope.selectedVariationCategoryResult.reportAbsence) {
			$scope.resultVariationMatchesAbsent = $scope.updateResultVariationMatches(false);
		} else {
			$scope.resultVariationMatchesAbsent = null;
		}
		console.log("updated resultVariationMatchesAbsent", $scope.resultVariationMatchesAbsent);
	}

	$scope.updateResultVariationMatchesPresent = function() {
		$scope.resultVariationMatchesPresent = $scope.updateResultVariationMatches(true);
		console.log("updated resultVariationMatchesPresent", $scope.resultVariationMatchesPresent);
	}
	
	$scope.updateResultVariationMatches = function(present) {
		if($scope.selectedVariationCategoryResult != null && $scope.selectedQueryAnalysis != null) {
			var number = 0;
			var resultVariationMatches = [];
			if($scope.selectedQueryAnalysis.sequenceFeatureAnalysis) {
				for(var i = 0; i < $scope.selectedQueryAnalysis.sequenceFeatureAnalysis.length; i++) {
					var sequenceFeatureAnalysis = $scope.selectedQueryAnalysis.sequenceFeatureAnalysis[i];
					var variationMatchGroupList;
					if(present) {
						variationMatchGroupList = sequenceFeatureAnalysis.variationMatchGroupPresent;
					} else {
						variationMatchGroupList = sequenceFeatureAnalysis.variationMatchGroupAbsent;
					}
					if(variationMatchGroupList) {
						for(var j = 0; j < variationMatchGroupList.length; j++) {
							var variationMatchGroup = variationMatchGroupList[j];
							if(variationMatchGroup.variationCategory != $scope.selectedVariationCategoryResult.name) {
								continue;
							}
							var definingReferenceName = variationMatchGroup.referenceName;
							var definingFeatureName = variationMatchGroup.featureName;
							var currentResultVariationMatch = null;
							var firstResultVariationMatch = null;
							for(var k = 0; k < variationMatchGroup.variationMatch.length; k++) {
								var variationMatch = variationMatchGroup.variationMatch[k];
								if(currentResultVariationMatch == null) {
									currentResultVariationMatch = {
											definingReferenceName: definingReferenceName,
											definingFeatureName: definingFeatureName,
											showDefining: false,
											variation: []
									};
									resultVariationMatches.push(currentResultVariationMatch);
									if(firstResultVariationMatch == null) {
										firstResultVariationMatch = currentResultVariationMatch;
										firstResultVariationMatch.showDefining = true;
										firstResultVariationMatch.rowspan = 1;
									} else {
										firstResultVariationMatch.rowspan = firstResultVariationMatch.rowspan+1;
									}
								}
								currentResultVariationMatch.variation.push({
									name: variationMatch.variationName,
									renderedName: variationMatch.variationRenderedName,
									pLocMatches: variationMatch.pLocMatches
								});
								number++;
								if(currentResultVariationMatch.variation.length == $scope.variationsPerRow) {
									currentResultVariationMatch = null;
								}
							}
						}
					}
				}
			}
			return {number: number, matches: resultVariationMatches};
		} else {
			return {number: 0, matches: []};
		}
	}

	$scope.$watch( 'analysisView', function(newObj, oldObj) {
		$scope.analysisViewChanged();
	}, false);

	
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

	$scope.$watch( 'selectedVariationCategoryResult', function(newObj, oldObj) {
		$scope.selectedVariationCategoryResultChanged();
	}, false);

	
	$scope.selectVariationCategories = function(item) {
		var included = _(item.variationCategorySelection).clone();
		var variationCategories = $scope.variationCategories;
		
		var dlg = dialogs.create($scope.analysisToolURL+'/dialogs/selectVariationCategories.html','selectVariationCategoriesCtrl',
				{ included: included,
				  variationCategories: variationCategories }, {});
		dlg.result.then(function(data){
			item.variationCategorySelection = data.included;
			console.log("variation categories updated to: ", item.variationCategorySelection);
		});

	}
	
	// invoked when "Analysis" button is pressed
	$scope.showAnalysisResults = function(item) {
		if( (!$scope.fileItemUnderAnalysis) || ($scope.fileItemUnderAnalysis != item) ) {
			$scope.resetSelections();
			
			console.log("show analysis : ", item);
			$scope.fileItemUnderAnalysis = item;

			if($scope.fileItemUnderAnalysis.webAnalysisResult.variationCategoryResult &&
					$scope.fileItemUnderAnalysis.webAnalysisResult.variationCategoryResult.length > 0) {
				$scope.enableVariationSummary = true;
			} else {
				$scope.enableVariationSummary = false;
			}
			$scope.analysisView = 'typingSummary';
			if($scope.fileItemUnderAnalysis.webAnalysisResult.queryAnalysis.length >= 0) {
				$scope.selectedQueryAnalysis = $scope.fileItemUnderAnalysis.webAnalysisResult.queryAnalysis[0];
				console.log("selected query analysis: ", $scope.selectedQueryAnalysis);
			}
			if($scope.fileItemUnderAnalysis.webAnalysisResult.featureAnalysis.length >= 0) {
				$scope.selectedFeatureAnalysis = $scope.fileItemUnderAnalysis.webAnalysisResult.featureAnalysis[0];
				console.log("selected feature analysis: ", $scope.selectedFeatureAnalysis);
			}
		}
	}

	$scope.selectGenomeFeature = function(selectedSequenceResult) {
		// remove popovers somehow
		var dlg = dialogs.create('dialogs/selectGenomeFeature.html','selectGenomeFeatureCtrl',$scope.selectedSequenceResult,{});
		dlg.result.then(function(feature){
		});
	}
	
	$scope.removeAll = function() {
		$scope.uploader.clearQueue();
		$scope.fileItemUnderAnalysis = null;
	}

	$scope.removeItem = function(item) {
		if($scope.fileItemUnderAnalysis == item) {
			$scope.fileItemUnderAnalysis = null;
		}
		item.remove();
	}

	var uploader = $scope.uploader = new FileUploader({});

	// executed after the project URL is set
	glueWS.addProjectUrlListener( {
		reportProjectURL: function(projectURL) {

			var moduleModePath = "module/"+$scope.analysisModuleName;
			
			glueWS.runGlueCommand(moduleModePath, {
		    	"list": { "variation-category": {} } 
			})
		    .success(function(data, status, headers, config) {
				  console.info('result', data);
				  $scope.variationCategories = tableResultAsObjectList(data);
				  console.info('variation categories:', $scope.variationCategories); 
		    })
		    .error(glueWS.raiseErrorDialog(dialogs, "listing variation categories"));
		    
		    $scope.uploader.url = projectURL + "/" + moduleModePath;

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
		var vCatNames = _.map(item.variationCategorySelection, function(vCat) { return vCat.name; });
		commandObject = {
				"web-analysis": {
					"vCategory": vCatNames
				}
			};
    	item.formData = [{command: JSON.stringify(commandObject)}];
        console.info('formData', JSON.stringify(item.formData));
        console.info('onBeforeUploadItem', item);
		$scope.analytics.eventTrack("submitSequence", 
				{   category: 'analysisTool', 
					label: 'fileName:'+item.file.name+',fileSize:'+item.file.size+',vCatNames:'+vCatNames.join('&') });


    };
    uploader.onSuccessItem = function(fileItem, response, status, headers) {
        console.info('onSuccessItem', fileItem, response, status, headers);
		$scope.analytics.eventTrack("sequenceResult", 
				{  category: 'analysisTool', 
					label: 'fileName:'+fileItem.file.name+',fileSize:'+fileItem.file.size });
		fileItem.webAnalysisResult = response.webAnalysisResult;
        for(var i = 0 ; i < fileItem.webAnalysisResult.queryAnalysis.length; i++) {
        	var queryAnalysis = fileItem.webAnalysisResult.queryAnalysis[i];
        	var fastaId = queryAnalysis.fastaId;
        	var assignment = '';
        	for(var j = 0 ; j < queryAnalysis.queryCladeCategoryResult.length; j++) {
        		var queryCladeCategoryResult = queryAnalysis.queryCladeCategoryResult[j]; 
        		var categoryName = queryCladeCategoryResult.categoryName;
        		var finalClade = queryCladeCategoryResult.finalClade;
        		if(finalClade == null) {
        			finalClade = 'unknown';
        		}
        		if(j > 0) {
        			assignment += ','
        		}
        		assignment += categoryName+':'+finalClade;
        	}
    		$scope.analytics.eventTrack("cladeAssignment", 
    				{  category: 'analysisTool', 
    					label: 'fileName:'+fileItem.file.name+
    							',fastaId:'+fastaId+
    							','+assignment});
        }
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
		fileItem.variationCategorySelection = 
			_.filter($scope.variationCategories, function(vCat) { return vCat.selectedByDefault; });
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

