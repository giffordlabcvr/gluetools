projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs',
    function($scope, $route, $routeParams, glueWS, dialogs) {

			$scope.listSequenceResult = null;
			addUtilsToScope($scope);

			$scope.init = function(whereClause, fieldNames)  {
				glueWS.runGlueCommand("", {
			    	"list": { "sequence": {
						"whereClause":whereClause,
			            "fieldName":fieldNames
			    	} } 
				})
			    .success(function(data, status, headers, config) {
					  console.info('list sequence raw result', data);
					  $scope.listSequenceResult = tableResultAsObjectList(data);
					  console.info('list sequence result as object list', $scope.listSequenceResult);
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "listing sequences"));
			}
}]);
