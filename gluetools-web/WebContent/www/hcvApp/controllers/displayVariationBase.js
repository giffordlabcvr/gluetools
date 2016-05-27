// http://jasonwatmore.com/post/2014/03/25/AngularJS-A-better-way-to-implement-a-base-controller.aspx

analysisTool.controller('displayVariationBase', ['$scope', '$modalInstance', 
                                                 'variationCategory', 'variation', 'ancestorAlmtNames',
    function($scope, $modalInstance, variationCategory, variation, ancestorAlmtNames ) {
		$scope.variationCategory = variationCategory;
		$scope.variation = variation;
		
		if($scope.variation.alignmentNote) {
			$scope.publicSeqFreqs = [];
			for(var i = 0; i < ancestorAlmtNames.length; i++) {
				var freq = _.find($scope.variation.alignmentNote, function(almtNote) { 
					return ancestorAlmtNames[i] == almtNote.alignmentName;
				});
				if(freq) {
					$scope.publicSeqFreqs.push(freq);
				}
			}
		}
		
		
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