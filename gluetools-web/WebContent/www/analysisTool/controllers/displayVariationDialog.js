analysisTool.controller('displayVariationCtrl',function($scope,$modalInstance,data){
	$scope.data = data;
	
	$scope.close = function(){
		$modalInstance.close($scope.data);
	}; 

});