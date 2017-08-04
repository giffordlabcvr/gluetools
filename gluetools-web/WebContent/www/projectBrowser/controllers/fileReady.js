projectBrowser.controller('fileReadyCtrl',function($scope,$modalInstance,data){
	$scope.data = data;

	$scope.accept = function(){
		$modalInstance.close($scope.data);
	}; 

});