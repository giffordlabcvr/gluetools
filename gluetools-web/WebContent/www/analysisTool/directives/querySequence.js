analysisTool.directive('querySequence', function(glueWebToolConfig, dialogs, glueWS) {
	  return {
		    restrict: 'A',
		    controller: function($scope) {
		    	var params = $scope.svgParams;

		    	$scope.y = 0;
		    	$scope.featureAas = [];
		    	$scope.aaProps = [];
		    	$scope.featureNtSegs = [];
		    	$scope.ntSegProps = [];
		    	
		    	// not sure if this first watch is necessary.
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.initVarProps();
		    		$scope.updateDiffs();
		    		$scope.updateElem();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedQueryFeatAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.initVarProps();
		    		$scope.updateDiffs();
		    		$scope.updateElem();
		    	}, false);

		    	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
		    		$scope.updateDiffs();
		    		$scope.updateElem();
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

		    	$scope.updateElem = function() {
		    		console.log("updating query sequence");
		    		$scope.elem.empty();
		    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+$scope.y+")"}, 
		    				function(g) {
			    		_.each($scope.varProps, function(varProp) {
			    			g.append(svgElem('g', {}, function(g2) {
			    				var locationRect = svgElem('rect', {
				    				"class": "varLocation", 
				    				x: varProp.x,
				    				width: varProp.width,
				    				height: varProp.locationHeight
				    			});
			    				locationRect.addClass("display-hide");
			    				varProp.locationRect = locationRect;
				    			g2.append(locationRect);
			    			}));
			    		});
		    		}));
		    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+$scope.y+")"}, 
		    				function(g) {
			    		_.each($scope.aaProps, function(aaProp) {
			    			g.append(svgElem('g', {"transform":"translate("+aaProp.x+", 0)"}, function(g2) {
				    			g2.append(svgElem('rect', {
				    				"class": aaProp.diff ? "queryAaDiffBackground" : "queryAaBackground", 
				    				width: aaProp.width,
				    				height: aaProp.height
				    			}));
				    			g2.append(svgElem('text', {
				    				"class": aaProp.diff ? "queryAaDiff" : "queryAa", 
				    				width: aaProp.width,
				    				height: aaProp.height,
				    				dx: aaProp.dx,
				    				dy: aaProp.dy
				    			}, function(text) {
				    				text.append(aaProp.text);
				    			}));
			    			}));
			    		});
		    		}));
		    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+ ($scope.y + $scope.svgParams.aaHeight) + ")"}, 
		    				function(g) {
		    			_.each($scope.ntSegProps, function(ntSegProp) {
			    			g.append(svgElem('g', {}, function(g2) {
				    			_.each(ntSegProp.ntProps, function(ntProp) {
					    			g2.append(svgElem('g', {"transform":"translate("+ ntProp.x + ", 0)"}, function(g3) {
					    				if(ntProp.diff) {
							    			g3.append(svgElem('rect', {
							    				"class": "queryNtDiffBackground", 
							    				width: ntProp.width,
							    				height: ntProp.height
							    			}));
					    				}
						    			g3.append(svgElem('text', {
						    				"class": ntProp.diff ? "queryNtDiff" : "queryNt", 
						    				width: ntProp.width,
						    				height: ntProp.height,
						    				dx: ntProp.dx,
						    				dy: ntProp.dy
						    			}, function(text) {
						    				text.append(ntProp.text);
						    			}));
					    			}));
					    		});
			    			}));
			    			g.append(svgElem('g', {"class": "queryNtIndex", "transform":"translate(0, "+ $scope.svgParams.ntHeight + ")"}, function(g2) {
				    			g2.append(svgElem('text', {
				    				x: ntSegProp.startIndexX,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: ntSegProp.indexDx,
				    				dy: ntSegProp.indexDy
				    			}, function(text) {
				    				text.append(String(ntSegProp.startIndexText));
				    			}));
				    			g2.append(svgElem('text', {
				    				x: ntSegProp.endIndexX,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: ntSegProp.indexDx,
				    				dy: ntSegProp.indexDy
				    			}, function(text) {
				    				text.append(String(ntSegProp.endIndexText));
				    			}));
			    			}));
			    		});
		    		}));
		    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+ ($scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + $scope.svgParams.ntIndexHeight) + ")"}, 
		    				function(g) {
		    			_.each($scope.varProps, function(varProp) {
			    			g.append(svgElem('g', {}, function(g2) {
			    				var rectElem1 = svgElem('rect', {
				    				id: varProp.id,
				    				"class": "vcat_"+varProp.variationCategory,
				    				x: varProp.x,
				    				y: varProp.y,
				    				width:varProp.width,
				    				height:varProp.height,
			    					tooltip:varProp.text,
			    			        "tooltip-append-to-body":"true",
			    			        "tooltip-placement":"top",
			    			        "tooltip-animation":"false"
				    			});
			    				rectElem1.on("click", function() {
			    					$scope.displayVariationQS(varProp);
			    				});
			    				rectElem1.on("mouseenter", function() {
			    					varProp.locationRect.removeClass("display-hide");
			    				});
			    				rectElem1.on("mouseleave", function() {
			    					varProp.locationRect.addClass("display-hide");
			    				});
			    				g2.append(rectElem1);
				    			g2.append(svgElem('rect', {
				    				"class": "varBox",
				    				x: varProp.x,
				    				y: varProp.y,
				    				width:varProp.width,
				    				height:varProp.height
				    			}));
			    			}));
			    		});
		    		}));

		    		console.log("query sequence updated");
		    	}

		    	
		    	$scope.initVarProps = function() {
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.varProps = params.initVarProps($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
			    	}
		    	};
		    	
		    	$scope.displayVariationQS = function(varProp) {
					varProp.locationRect.addClass("display-hide");
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
		    link: function(scope, element, attributes){
		    	scope.elem = element;
    			scope.updateElem();
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefName: '=',
		      selectedQueryFeatAnalysis: '=',
		      selectedQueryAnalysis: '=',
		      sequenceIndex: '=',
		      variationCategories: '='
		    }
		  };
		});