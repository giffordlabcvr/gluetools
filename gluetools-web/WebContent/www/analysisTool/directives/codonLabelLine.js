analysisTool.directive('codonLabelLine', function(glueWebToolConfig) {
	  return {
		    restrict: 'A',
		    controller: function($scope) {
		    	var params = $scope.svgParams;
		    	$scope.y = 0;
		    	$scope.cProps = [];
		    	
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    	}, false);

		    	$scope.initProps = function() {
		    		$scope.y = params.codonLabelLineY();
		    		if($scope.selectedFeatureAnalysis) {
		    			$scope.cProps = _.map($scope.selectedFeatureAnalysis.codonLabel, function(codonLabel) {
				    		var nts = (codonLabel.endUIndex - codonLabel.startUIndex) + 1;
					    	var height = params.codonLabelHeight;
					    	var width = (nts * params.ntWidth) + ( (nts-1) * params.ntGap );
		    				return {
		    		    		x: (codonLabel.startUIndex - $scope.selectedFeatureAnalysis.startUIndex) * (params.ntWidth + params.ntGap),
						    	width: width,
		    					height: height,
		    					dx: width / 2.0,
		    					dy: height / 2.0,
		    					text: codonLabel.label
		    				};
		    			});
		    			$scope.updateElem();
		    		}
		    	}
		    	
		    	$scope.updateElem = function() {
		    		console.log("updating codon label line");
		    		$scope.elem.empty();
		    		$scope.elem.attr("transform", "translate(0, "+$scope.y+")");
		    		_.each($scope.cProps, function(cProp) {
		    			$scope.elem.append(svgElem('text', {
		    				"class": "codonLabel", 
		    				x: cProp.x,
		    				width: cProp.width,
		    				height: cProp.height,
		    				dx: cProp.dx,
		    				dy: cProp.dy
		    			}, function(text) {
		    				text.append(cProp.text);
		    			}));
		    		});
		    		console.log("codon label line updated");
		    	}
		    	
		    },
		    link: function(scope, element, attributes){
		    	scope.elem = element;
    			scope.updateElem();
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		    }
		  };
		});