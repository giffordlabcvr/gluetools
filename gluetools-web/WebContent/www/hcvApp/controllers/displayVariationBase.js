// http://jasonwatmore.com/post/2014/03/25/AngularJS-A-better-way-to-implement-a-base-controller.aspx

analysisTool.controller('displayVariationBase', ['$scope', '$modalInstance', 
                                                 'variationCategory', 'variation', 'ancestorAlmtNames',
    function($scope, $modalInstance, variationCategory, variation, ancestorAlmtNames ) {
		$scope.variationCategory = variationCategory;
		$scope.variation = variation;
		
		$scope.data = {
				freqView: "querySequenceClades",
				queryCladeFreqs: [],
				allCladeFreqs: []
		};
		
		if($scope.variation.alignmentNote) {
			$scope.data.allCladeFreqs = $scope.variation.alignmentNote;
			for(var i = 0; i < ancestorAlmtNames.length; i++) {
				var freq = _.find($scope.variation.alignmentNote, function(almtNote) { 
					return ancestorAlmtNames[i] == almtNote.alignmentName;
				});
				if(freq) {
					$scope.data.queryCladeFreqs.push(freq);
				}
			}
		}
		
		$scope.data.cladeFreqs = $scope.data.queryCladeFreqs;
		
		$scope.data.updateCladeFreqs = function() {
			if($scope.data.freqView == "querySequenceClades") {
				$scope.data.cladeFreqs = $scope.data.queryCladeFreqs;
			} else {
				$scope.data.cladeFreqs = $scope.data.allCladeFreqs;
			}
		}
		
		$scope.$watch( 'data.freqView', function(newObj, oldObj) {
			$scope.data.updateCladeFreqs();
		}, false);

		addUtilsToScope($scope);
		
		$scope.close = function(){
			$modalInstance.close();
		}; 
	
		$scope.handleNull = function(text){
			if(text == null) {
				return "-";
			}
			return text;
		}; 
	}]);