analysisTool.directive('querySequence', function(moduleURLs) {
	  return {
		    restrict: 'E',
		    replace: true,
		    link: function(scope, element, attrs) {
		    	console.log("link running");
		    },
		    controller: function($scope) {
		    	var params = $scope.svgParams;

		    	$scope.$watch( 'selectedFeatureAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.updateDiffs();
		    	}, false);
		    	
		    	$scope.$watch( 'selectedQueryFeatAnalysis', function(newObj, oldObj) {
		    		$scope.initProps();
		    		$scope.updateDiffs();
		    	}, false);

		    	$scope.$watch( 'selectedRefName', function(newObj, oldObj) {
		    		$scope.updateDiffs();
		    	}, false);

		    	$scope.initProps = function() {
		    		$scope.y = params.sequenceY($scope.sequenceIndex);
		    		if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.aaProps = params.initAaProps($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.ntSegProps = params.initNtSegProps($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
			    	}
		    	};
		    	
		    	$scope.updateDiffs = function() {
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedRefName) {
			    		for(var i = 0; i < $scope.selectedQueryFeatAnalysis.aas.length; i++) {
			    			var queryAa = $scope.selectedQueryFeatAnalysis.aas[i];
			    			$scope.aaProps[i].diff = queryAa.referenceDiffs != null && queryAa.referenceDiffs.indexOf($scope.selectedRefName) != -1;
			    		}
			    		for(var i = 0; i < $scope.selectedQueryFeatAnalysis.nts.length; i++) {
			    			var ntSeg = $scope.selectedQueryFeatAnalysis.nts[i];
			    			var ntSegProp = $scope.ntSegProps[i];
			    			var referenceDiff = _.find(ntSeg.referenceDiffs, function(rDiff) { return rDiff.refName == $scope.selectedRefName; });
				    		for(var j = 0; j < ntSeg.nts.length; j++) {
				    			ntSegProp.ntProps[j].diff = referenceDiff.mask[j] == 'X';
				    		}
			    		}
			    	}
		    	};
		    },
		    scope: {
		      svgParams: '=',
		      selectedFeatureAnalysis: '=',
		      selectedRefName: '=',
		      selectedQueryFeatAnalysis: '=',
		      sequenceIndex: '='
		    },
		    templateNamespace: 'svg',
		    templateUrl: moduleURLs.getAnalysisToolURL()+'/views/querySequence.html'
		  };
		});