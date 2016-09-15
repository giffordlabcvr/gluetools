projectBrowser.controller('alignmentsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
    function($scope, $route, $routeParams, glueWS, dialogs) {

	$scope.listAlignmentResult = null;
	
	addUtilsToScope($scope);
	
	glueWS.runGlueCommand("", {
    	"list": { 
    		"alignment": {
    			"whereClause":"refSequence.name != null",
    			"fieldName":[
    			                "name",
    			                "displayName",
    			                "parent.name",
    			                "parent.displayName",
    			                "description",
    			                "refSequence.name"
    			            ]
    		} } 
	})
    .success(function(data, status, headers, config) {
		  console.info('list alignment raw result', data);
		  $scope.listAlignmentResult = tableResultAsObjectList(data);
		  console.info('list alignment result as object list', $scope.listAlignmentResult);
    })
    .error(glueWS.raiseErrorDialog(dialogs, "listing alignments"));

	
}]);
