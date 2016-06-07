projectBrowser.controller('renderableObjectBaseCtrl', [ '$scope', 'glueObjectPath', 'glueWS', 'glueWebToolConfig',
        function($scope, glueObjectPath, glueWS, glueWebToolConfig) {
	
	$scope.projectBrowserURL = glueWebToolConfig.getProjectBrowserURL();

	console.log("renderableObjectBaseCtrl");
	$scope.renderResult = null;
	glueWS.runGlueCommand($scope.glueObjectPath, {
    	"render-object": {} 
	})
    .success(function(data, status, headers, config) {
		  console.info('render result', data);
		  $scope.renderResult = data;
    })
    .error(function() {
    	glueWS.raiseErrorDialog(dialogs, "rendering object");
    });

	
}]);