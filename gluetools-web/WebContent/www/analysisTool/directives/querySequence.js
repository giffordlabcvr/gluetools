analysisTool.directive('querySequence', function(moduleURLs) {
	  return {
		    restrict: 'E',
		    replace: true,
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
		    		console.log("initProps query");
		    		$scope.y = params.sequenceY($scope.sequenceIndex);
		    		if($scope.selectedQueryFeatAnalysis && $scope.selectedFeatureAnalysis) {
				    	$scope.featureAas = params.initFeatureAas($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.aaProps = params.initAaProps($scope.featureAas, $scope.selectedFeatureAnalysis);
				    	$scope.featureNtSegs = params.initFeatureNtSegs($scope.selectedQueryFeatAnalysis, $scope.selectedFeatureAnalysis);
				    	$scope.ntSegProps = params.initNtSegProps($scope.featureNtSegs, $scope.selectedFeatureAnalysis);
			    	}
		    	};
		    	
		    	$scope.updateDiffs = function() {
		    		console.log("updateDiffs query");
			    	if($scope.selectedQueryFeatAnalysis && $scope.selectedRefName) {
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