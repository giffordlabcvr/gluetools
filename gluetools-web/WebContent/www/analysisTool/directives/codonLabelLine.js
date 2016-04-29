analysisTool.directive('codonLabelLine', function(moduleURLs) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    	var params = $scope.svgParams;
		    	
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
		    		}
		    	}
		    	
		    },
		    replace: true,
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		    },
		    templateNamespace: 'svg',
		    templateUrl: moduleURLs.getAnalysisToolURL()+'/views/codonLabelLine.html'
		  };
		});