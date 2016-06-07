projectBrowser.controller('alignmentCtrl', 
		[ '$scope', '$routeParams', '$controller',
    function($scope, $routeParams, $controller) {

	$scope.alignmentName = $routeParams.alignmentName;
	$scope.glueObjectPath = "alignment/"+$scope.alignmentName;

	$controller('renderableObjectBaseCtrl', { 
		$scope: $scope, 
		glueObjectPath: $scope.glueObjectPath
	});	
}]);
