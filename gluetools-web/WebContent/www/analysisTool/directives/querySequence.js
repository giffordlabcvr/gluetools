
// This is a hack.
// When closing a dialog which was launched from an SVG element, on Internet Explorer, 
// there is an attempt to invoke focus() on the main svg element, which doesn't exist.
// this hack defines it.
if (typeof SVGElement.prototype.focus == 'undefined') {
    SVGElement.prototype.focus = function() {};
}


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
		    	$scope.propsDirty = true;
		    	
		    	// not sure if this first watch is necessary.
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
			    	$scope.propsDirty = true;
		    		$scope.updateElem();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedQueryFeatAnalysis', function(newObj, oldObj) {
			    	$scope.propsDirty = true;
		    		$scope.updateElem();
		    	}, false);

		    	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
			    	$scope.propsDirty = true;
		    		$scope.updateElem();
		    	}, false);

		    	$scope.$watch( 'analysisView', function(newObj, oldObj) {
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
		    		if($scope.analysisView == 'genomeDetail' && $scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis && $scope.selectedRefName) {
		    			if($scope.propsDirty) {
				    		$scope.initProps();
				    		$scope.initVarProps();
				    		$scope.updateDiffs();

			    			console.log("updating query sequence");
			    			var docFrag = angular.element(document.createDocumentFragment());
			    			
				    		docFrag.append(svgElem('g', {"transform":"translate(0, "+$scope.y+")"}, 
				    				function(g) {
					    		_.each($scope.varProps, function(varProp) {
					    			g.append(svgElem('g', {}, function(g2) {
						    			varProp.highlightRects = [];
							    		_.each(varProp.propLocations, function(propLocation) {
							    				var highlightRect = svgElem('rect', {
								    				"class": "varHighlight", 
								    				x: propLocation.x,
								    				width: propLocation.width,
								    				height: varProp.highlightHeight
								    			});
							    				highlightRect.addClass("display-hide");
							    				varProp.highlightRects.push(highlightRect);
								    			g2.append(highlightRect);
						    			});
					    			}));
					    		});
				    		}));
				    		_.each($scope.aaProps, function(aaProp) {
			    				docFrag.append(svgElem('rect', {
				    				"class": aaProp.diff ? "queryAaDiffBackground" : "queryAaBackground", 
				    				x: aaProp.x,
				    				y: $scope.y,
				    				width: aaProp.width,
				    				height: aaProp.height
				    			}));
			    				docFrag.append(svgElem('text', {
				    				"class": aaProp.diff ? "queryAaDiff" : "queryAa", 
				    				x: aaProp.x + aaProp.dx,
				    				y: $scope.y + aaProp.dy,
				    				width: aaProp.width,
				    				height: aaProp.height,
				    				dy: svgDyValue(userAgent)
				    			}, function(text) {
				    				text.append(aaProp.text);
				    			}));
				    		});
				    		
			    			_.each($scope.ntSegProps, function(ntSegProp) {
				    			_.each(ntSegProp.ntProps, function(ntProp) {
				    				if(ntProp.diff) {
				    					docFrag.append(svgElem('rect', {
						    				"class": "queryNtDiffBackground", 
						    				x: ntProp.x,
						    				y: $scope.y + $scope.svgParams.aaHeight,
						    				width: ntProp.width,
						    				height: ntProp.height
						    			}));
				    				}
				    				docFrag.append(svgElem('text', {
					    				"class": ntProp.diff ? "queryNtDiff" : "queryNt", 
					    				x: ntProp.x + ntProp.dx,
					    				y: $scope.y + $scope.svgParams.aaHeight + ntProp.dy,
					    				width: ntProp.width,
					    				height: ntProp.height,
					    				dy: svgDyValue(userAgent)
					    			}, function(text) {
					    				text.append(ntProp.text);
					    			}));
					    		});
				    		});
	
				    		
				    		
			    			_.each($scope.ntSegProps, function(ntSegProp) {
			    				docFrag.append(svgElem('text', {
				    				"class": "queryNtIndex", 
				    				x: ntSegProp.startIndexX + ntSegProp.indexDx,
				    				y: $scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + ntSegProp.indexDy,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: svgDxValue(userAgent)
				    			}, function(text) {
				    				text.append(String(ntSegProp.startIndexText));
				    			}));
			    				docFrag.append(svgElem('text', {
				    				"class": "queryNtIndex", 
				    				x: ntSegProp.endIndexX + ntSegProp.indexDx,
				    				y: $scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + ntSegProp.indexDy,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: svgDxValue(userAgent)
				    			}, function(text) {
				    				text.append(String(ntSegProp.endIndexText));
				    			}));
				    		});
				    		docFrag.append(svgElem('g', {"transform":"translate(0, "+ ($scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + $scope.svgParams.ntIndexHeight) + ")"}, 
				    				function(g) {
				    			_.each($scope.varProps, function(varProp) {
					    			g.append(svgElem('g', {}, function(g2) {
					    				var rectElems = [];
					    				if(varProp.pLocConnector) {
						    				var connectorElem = svgElem('rect', {
							    				"class": "vcat_"+varProp.variationCategory,
							    				x: varProp.pLocConnector.x,
							    				y: varProp.pLocConnector.y,
							    				width:varProp.pLocConnector.width,
							    				height:varProp.pLocConnector.height
							    			});
						    				g2.append(connectorElem);
							    			g2.append(svgElem('rect', {
							    				"class": "varBox",
							    				x: varProp.pLocConnector.x,
							    				y: varProp.pLocConnector.y,
							    				width:varProp.pLocConnector.width,
							    				height:varProp.pLocConnector.height
							    			}));
							    			rectElems.push(connectorElem);
					    				}
						    			_.each(varProp.propLocations, function(propLocation) {
						    				var rectElem1 = svgElem('rect', {
							    				id: varProp.id,
							    				"class": "vcat_"+varProp.variationCategory,
							    				x: propLocation.x,
							    				y: propLocation.y,
							    				width:propLocation.width,
							    				height:propLocation.height
							    			});
						    				g2.append(rectElem1);
						    				rectElems.push(rectElem1);
							    			g2.append(svgElem('rect', {
							    				"class": "varBox",
							    				x: propLocation.x,
							    				y: propLocation.y,
							    				width:propLocation.width,
							    				height:propLocation.height
							    			}));
						    			});
						    			_.each(rectElems, function(rectElem) {
						    				rectElem.on("click", function() {
						    					$scope.displayVariationQS(varProp);
						    				});
						    				// IE event issues require Angular 1.4.9
						    				// https://github.com/angular/angular.js/issues/10259
						    				rectElem.on("mouseenter", function() {
						    					_.each(varProp.highlightRects, function(highlightRect) {
							    					highlightRect.removeClass("display-hide");
						    					});
						    				});
						    				rectElem.on("mouseleave", function() {
						    					_.each(varProp.highlightRects, function(highlightRect) {
							    					highlightRect.addClass("display-hide");
						    					});
						    				});
						    			});
					    				g2.append(svgElem('title', {}, function(title) {
					    					title.append(varProp.text);
					    				}));
					    				
					    			}));
					    		});
				    		}));

				    		console.log("query sequence updated");
				    		$scope.elem.empty();
				    		$scope.elem.append(docFrag);
				    		$scope.propsDirty = false;
		    			}
		    		}
		    	}

		    	
		    	$scope.initVarProps = function() {
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.varProps = params.initVarProps($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
			    	}
		    	};
		    	
		    	$scope.displayVariationQS = function(varProp) {
					_.each(varProp.highlightRects, function(highlightRect) {
    					highlightRect.addClass("display-hide");
					});
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
				    				  ancestorAlmtNames: _.uniq($scope.selectedQueryAnalysis.ancestorAlmtName),
				    				  pLocMatches: varProp.pLocMatches
				    				}, {});
				    		dlg.result.then(function() {
				    			// completion handler
				    		}, function() {
				    		    // Error handler
				    		}).finally(function() {
				    			varProp.text = tooltip;
				    		});
				    })
				    .error(function(data, status, headers, config) {
				    	var standardErrFunction = glueWS.raiseErrorDialog(dialogs, "rendering variation "+varProp.variationName);
				    	console.log("error!")
		    			varProp.text = tooltip;
				    	standardErrFunction(data, status, headers, config);
				    });
		    	}
		    	
		    },
		    link: function(scope, element, attributes){
		    	scope.elem = element;
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefName: '=',
		      selectedQueryFeatAnalysis: '=',
		      selectedQueryAnalysis: '=',
		      sequenceIndex: '=',
		      variationCategories: '=',
			  analysisView: '='
		    }
		  };
		});