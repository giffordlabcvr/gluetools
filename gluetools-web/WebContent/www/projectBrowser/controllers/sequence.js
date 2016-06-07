projectBrowser.controller('sequenceCtrl', 
		[ '$scope', '$routeParams', '$controller',
		    function($scope, $routeParams, $controller) {

	$scope.sourceName = $routeParams.sourceName;
	$scope.sequenceID = $routeParams.sequenceID;
	$scope.glueObjectPath = "sequence/"+$scope.sourceName+"/"+$scope.sequenceID;

	$controller('renderableObjectBaseCtrl', { 
		$scope: $scope, 
		glueObjectPath: $scope.glueObjectPath
	});	

}]);
