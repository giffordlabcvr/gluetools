projectBrowser.controller('alignmentMemberCtrl', 
		[ '$scope', '$routeParams', '$controller',
		    function($scope, $routeParams, $controller) {

	$scope.alignmentName = $routeParams.alignmentName;
	$scope.sourceName = $routeParams.sourceName;
	$scope.sequenceID = $routeParams.sequenceID;
	$scope.glueObjectPath = "alignment/"+$scope.alignmentName+"/member/"+$scope.sourceName+"/"+$scope.sequenceID;

	$controller('renderableObjectBaseCtrl', { 
		$scope: $scope, 
		glueObjectPath: $scope.glueObjectPath
	});	
	
}]);
