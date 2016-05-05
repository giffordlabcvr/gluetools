analysisTool.directive('referenceSequence', function(moduleURLs) {
	  return {
		    restrict: 'E',
		    replace: true,
		    controller: function($scope) {
		    	var params = $scope.svgParams;
		    	
		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedRefFeatAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    	}, false);

		    	$scope.initProps = function() {
		    		console.log("initProps reference");
		    		$scope.y = params.sequenceY($scope.sequenceIndex);
		    		if($scope.selectedRefFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.featureAas = params.initFeatureAas($scope.selectedRefFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.aaProps = params.initAaProps($scope.featureAas, $scope.selectedFeatureAnalysis);
				    	$scope.featureNtSegs = params.initFeatureNtSegs($scope.selectedRefFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.ntSegProps = params.initNtSegProps($scope.featureNtSegs, $scope.selectedFeatureAnalysis);
			    	}
		    	};

		    	
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefFeatAnalysis: '=',
		      sequenceIndex: '='
		    },
		    templateNamespace: 'svg',
		    templateUrl: moduleURLs.getAnalysisToolURL()+'/views/referenceSequence.html'
		  };
		});