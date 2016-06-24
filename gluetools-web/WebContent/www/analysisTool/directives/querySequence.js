analysisTool.directive('querySequence', function(glueWebToolConfig, dialogs, glueWS) {
	  return {
		    restrict: 'E',
		    replace: true,
		    controller: function($scope) {
		    	var params = $scope.svgParams;

		    	// not sure if this first watch is necessary.
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.initVarProps();
		    		$scope.updateDiffs();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedQueryFeatAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.initVarProps();
		    		$scope.updateDiffs();
		    	}, false);

		    	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
		    		$scope.updateDiffs();
		    	}, false);

		    	$scope.initProps = function() {
		    		$scope.y = params.sequenceY($scope.sequenceIndex);
		    		if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
			    		console.log("$scope.selectedQueryFeatAnalysis", $scope.selectedQueryFeatAnalysis);
			    		console.log("$scope.selectedFeatureAnalysis", $scope.selectedFeatureAnalysis);
			    		console.log("initProps query start");
				    	$scope.featureAas = params.initFeatureAas($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.aaProps = params.initAaProps($scope.featureAas, $scope.selectedFeatureAnalysis);
				    	$scope.featureNtSegs = params.initFeatureNtSegs($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.ntSegProps = params.initNtSegProps($scope.featureNtSegs, $scope.selectedFeatureAnalysis);
			    		console.log("initProps query finish");
			    	}
		    	};
		    	
		    	$scope.updateDiffs = function() {
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedRefName) {
			    		console.log("updateDiffs query start");
			    		for(var i = 0; i < $scope.featureAas.length; i++) {
			    			var queryAa = $scope.featureAas[i];
			    			$scope.aaProps[i].diff = queryAa.referenceDiffs != null && queryAa.referenceDiffs.indexOf($scope.selectedRefName) != -1;
			    		}
			    		for(var i = 0; i < $scope.featureNtSegs.length; i++) {
			    			var ntSeg = $scope.featureNtSegs[i];
			    			var ntSegProp = $scope.ntSegProps[i];
			    			var referenceDiff = _.find(ntSeg.referenceDiffs, function(rDiff) { return rDiff.refName == $scope.selectedRefName; });
				    		for(var j = ntSegProp.truncateLeft; j < (ntSeg.nts.length - ntSegProp.truncateRight); j++) {
				    			ntSegProp.ntProps[(j - ntSegProp.truncateLeft)].diff = referenceDiff.mask[j] == 'X';
				    		}
			    		}
			    		console.log("updateDiffs query finish");
			    	}
		    	};

		    	$scope.initVarProps = function() {
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.varProps = params.initVarProps($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
			    	}
		    	};
		    	
		    	$scope.displayVariationQS = function(varProp) {
		    		varProp.mouseOver = false;
		    		var tooltip = varProp.text;
		    		varProp.text = null;

					var varVCat = _.find(
							$scope.variationCategories, 
							function(vCat) {
								return vCat.name == varProp.variationCategory; } );

					var variationRendererModuleName = varVCat.objectRendererModule;

					var variationRendererDialog = _.find(glueWebToolConfig.getRendererDialogs(), 
							function(rendererDialog) { return rendererDialog.renderer == variationRendererModuleName;});

					console.info('variationRendererDialog', variationRendererDialog);

		    		var glueVariationPath = 
		    			"reference/"+varProp.varReferenceName+
		    			"/feature-location/"+varProp.varFeatureName+
		    			"/variation/"+varProp.variationName;
					glueWS.runGlueCommand(glueVariationPath, {
				    	"render-object": { "rendererModuleName": variationRendererModuleName } 
					})
				    .success(function(data, status, headers, config) {
						  console.info('render result', data);
				    		var dlg = dialogs.create(variationRendererDialog.dialogURL,
				    				variationRendererDialog.dialogController, 
				    				{ renderedVariation: data,
				    				  variationCategory: varVCat, 
				    				  ancestorAlmtNames: _.uniq($scope.selectedQueryAnalysis.ancestorAlmtName)
				    				}, {});
				    		dlg.result.then(function() {
				    			// completion handler
				    		}, function() {
				    		    // Error handler
				    		}).finally(function() {
				    			varProp.text = tooltip;
				    		});
				    })
				    .error(function() {
		    			varProp.text = tooltip;
				    	glueWS.raiseErrorDialog(dialogs, "rendering variation "+varProp.variationName);
				    });
		    	}
		    	
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefName: '=',
		      selectedQueryFeatAnalysis: '=',
		      selectedQueryAnalysis: '=',
		      sequenceIndex: '=',
		      variationCategories: '='
		    },
		    templateNamespace: 'svg',
		    templateUrl: glueWebToolConfig.getAnalysisToolURL()+'/views/querySequence.html'
		  };
		});