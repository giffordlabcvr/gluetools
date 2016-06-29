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
				    		var docFrag = angular.element(document.createDocumentFragment());
				    		
			    			_.each($scope.aaProps, function(aaProp) {
			    				docFrag.append(svgElem('rect', {
				    				"class": "referenceAaBackground",
				    				x: aaProp.x,
				    				y: $scope.y,
				    				width: aaProp.width,
				    				height: aaProp.height
				    			}));
			    				docFrag.append(svgElem('text', {
				    				"class": "referenceAa", 
				    				x: aaProp.x + aaProp.dx,
				    				y: $scope.y + aaProp.dy,
				    				dy: svgDyValue(userAgent)
				    			}, function(text) {
				    				text.append(aaProp.text);
				    			}));
				    		});
			    			
			    			_.each($scope.ntSegProps, function(ntSegProp) {
				    			_.each(ntSegProp.ntProps, function(ntProp) {
					    			docFrag.append(svgElem('text', {
					    				"class": "referenceNt", 
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
				    				"class": "referenceNtIndex", 
				    				x: ntSegProp.startIndexX + ntSegProp.indexDx,
				    				y: $scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + ntSegProp.indexDy,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: svgDxValue(userAgent)
				    			}, function(text) {
				    				text.append(String(ntSegProp.startIndexText));
				    			}));
			    				docFrag.append(svgElem('text', {
				    				"class": "referenceNtIndex", 
				    				x: ntSegProp.endIndexX + ntSegProp.indexDx,
				    				y: $scope.y + $scope.svgParams.aaHeight + $scope.svgParams.ntHeight + ntSegProp.indexDy,
				    				width: $scope.svgParams.ntWidth,
				    				height: $scope.svgParams.ntIndexWidth,
				    				dx: svgDxValue(userAgent)
				    			}, function(text) {
				    				text.append(String(ntSegProp.endIndexText));
				    			}));
				    		});
				    		console.log("reference sequence updated");
		    				$scope.propsDirty = false;
				    		$scope.elem.empty();
				    		$scope.elem.append(docFrag);
		    			}
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