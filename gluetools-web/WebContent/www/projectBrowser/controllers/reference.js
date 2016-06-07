projectBrowser.controller('referenceCtrl', 
		[ '$scope', '$routeParams', '$controller',
		    function($scope, $routeParams, $controller) {

	$scope.refName = $routeParams.refName;
	$scope.glueObjectPath = "reference/"+$scope.refName;
	
	$controller('renderableObjectBaseCtrl', { 
		$scope: $scope, 
		glueObjectPath: $scope.glueObjectPath
	});	

}]);
