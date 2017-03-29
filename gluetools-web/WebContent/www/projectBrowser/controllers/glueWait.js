projectBrowser.controller('glueWaitCtrl',function($scope,$modalInstance,data){
	$scope.data = data;

	$scope.accept = function(){
		$modalInstance.close($scope.data);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

});