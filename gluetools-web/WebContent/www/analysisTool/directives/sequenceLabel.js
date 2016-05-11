analysisTool.directive('sequenceLabel', function(glueWebToolConfig) {
	  return {
		    restrict: 'E',
		    controller: function($scope) {
		    	var params = $scope.svgParams;
		    	$scope.x = 0;
		    	$scope.y = params.sequenceY($scope.sequenceIndex);
		    	$scope.width = params.sequenceLabelWidth;
		    	$scope.height = params.aaHeight;
		    	$scope.dx = 0;
		    	$scope.dy = $scope.height / 2.0;
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