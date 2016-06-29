analysisTool.directive('sequenceLabel', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    	var params = $scope.svgParams;
		    	$scope.width = params.sequenceLabelWidth;
		    	$scope.height = params.aaHeight;
		    	$scope.x = 0;
		    	$scope.y = params.sequenceY($scope.sequenceIndex) + $scope.height / 2.0;
		    	$scope.dx = 0;
		    	$scope.dy = svgDyValue(userAgent);
		    },
		    replace: true,
		    scope: {
		      sequenceLabel: '=',
		      sequenceIndex: '=',
		      svgParams: '=',
		    },
		    templateNamespace: 'svg',
		    templateUrl: glueWebToolConfig.getAnalysisToolURL()+'/views/sequenceLabel.html'
		  };
		});