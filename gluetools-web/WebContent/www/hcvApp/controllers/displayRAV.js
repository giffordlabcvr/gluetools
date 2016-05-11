analysisTool.controller('displayRAVCtrl',function($scope,$modalInstance,data){
	$scope.renderedVariation = data.renderedVariation;
	$scope.variationCategory = data.variationCategory;
	
	$scope.close = function(){
		$modalInstance.close();
	}; 

	$scope.handleNull = function(text){
		if(text == null) {
			return "-";
		}
		return text;
	}; 

	
});