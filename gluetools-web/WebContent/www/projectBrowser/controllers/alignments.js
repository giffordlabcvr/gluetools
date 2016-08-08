projectBrowser.controller('alignmentsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 'glueWebToolConfig',
    function($scope, $route, $routeParams, glueWS, dialogs, glueWebToolConfig) {

	$scope.projectBrowserURL = glueWebToolConfig.getProjectBrowserURL();

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
    .error(function() {
    	glueWS.raiseErrorDialog(dialogs, "listing alignments");
    });

	
}]);
