projectBrowser.controller('referencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs',
    function($scope, $route, $routeParams, glueWS, dialogs) {

	$scope.listReferenceResult = null;
	
	glueWS.runGlueCommand("", {
    	"list": { "reference": {} } 
	})
    .success(function(data, status, headers, config) {
		  console.info('list reference raw result', data);
		  $scope.listReferenceResult = tableResultAsObjectList(data);
		  console.info('list reference result as object list', $scope.listReferenceResult);
    })
    .error(glueWS.raiseErrorDialog(dialogs, "listing references"));

	
}]);
