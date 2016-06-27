analysisTool.directive('referenceSequence', function(glueWebToolConfig) {
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
		    	
		    	
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
			    	$scope.propsDirty = true;
		    		$scope.updateElem();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedRefFeatAnalysis', function(newObj, oldObj) {
			    	$scope.propsDirty = true;
		    		$scope.updateElem();
		    	}, false);
		    	
		    	$scope.$watch( 'analysisView', function(newObj, oldObj) {
		    		$scope.updateElem();
		    	}, false);

		    	$scope.initProps = function() {
		    		$scope.y = params.sequenceY($scope.sequenceIndex);
		    		if($scope.selectedRefFeatAnalysis && $scope.selectedFeatureAnalysis) {
			    		console.log("initProps reference start");
				    	$scope.featureAas = params.initFeatureAas($scope.selectedRefFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.aaProps = params.initAaProps($scope.featureAas, $scope.selectedFeatureAnalysis);
				    	$scope.featureNtSegs = params.initFeatureNtSegs($scope.selectedRefFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.ntSegProps = params.initNtSegProps($scope.featureNtSegs, $scope.selectedFeatureAnalysis);
			    		console.log("initProps reference finish");
			    	}
		    	};

		    	$scope.updateElem = function() {
		    		if($scope.analysisView == 'genomeDetail' && $scope.selectedRefFeatAnalysis && $scope.selectedFeatureAnalysis) {
		    			if($scope.propsDirty) {
		    				$scope.initProps();
		    				
				    		console.log("updating reference sequence");
				    		$scope.elem.empty();
				    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+$scope.y+")"}, function(g) {
				    			_.each($scope.aaProps, function(aaProp) {
					    			g.append(svgElem('g', {"transform":"translate("+aaProp.x+", 0)"}, function(g2) {
						    			g2.append(svgElem('rect', {
						    				"class": "referenceAaBackground", 
						    				width: aaProp.width,
						    				height: aaProp.height
						    			}));
						    			g2.append(svgElem('text', {
						    				"class": "referenceAa", 
						    				width: aaProp.width,
						    				height: aaProp.height,
						    				dx: aaProp.dx,
						    				dy: aaProp.dy
						    			}, function(text) {
						    				text.append(aaProp.text);
						    			}));
					    			}));
					    		});
				    		})
				    		);
				    		$scope.elem.append(svgElem('g', {"transform":"translate(0, "+ ($scope.y + $scope.svgParams.aaHeight) + ")"}, 
				    				function(g) {
				    			_.each($scope.ntSegProps, function(ntSegProp) {
					    			g.append(svgElem('g', {}, function(g2) {
						    			_.each(ntSegProp.ntProps, function(ntProp) {
							    			g2.append(svgElem('g', {"transform":"translate("+ ntProp.x + ", 0)"}, function(g3) {
								    			g3.append(svgElem('text', {
								    				"class": "referenceNt", 
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
					    			g.append(svgElem('g', {"class": "referenceNtIndex", "transform":"translate(0, "+ $scope.svgParams.ntHeight + ")"}, function(g2) {
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
				    		})
				    		);
				    		console.log("reference sequence updated");
		    			}
	    				$scope.propsDirty = false;
		    		}
		    	}
		    	
		    	
		    },
		    link: function(scope, element, attributes){
		    	scope.elem = element;
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefFeatAnalysis: '=',
		      sequenceIndex: '=',
		      analysisView: '='
		    }
		  };
		});