projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs',
    function($scope, $route, $routeParams, glueWS, dialogs) {

	$scope.listSequenceResult = null;
	
	glueWS.runGlueCommand("", {
    	"list": { "sequence": {} } 
	})
    .success(function(data, status, headers, config) {
		  console.info('list sequence raw result', data);
		  $scope.listSequenceResult = tableResultAsObjectList(data);
		  console.info('list sequence result as object list', $scope.listSequenceResult);
    })
    .error(glueWS.raiseErrorDialog(dialogs, "listing sequences"));

	
}]);
