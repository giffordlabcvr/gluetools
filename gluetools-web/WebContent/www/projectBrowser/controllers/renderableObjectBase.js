projectBrowser.controller('renderableObjectBaseCtrl', [ '$scope', 'glueObjectPath', 'glueWS', 'glueWebToolConfig', 'dialogs',
        function($scope, glueObjectPath, glueWS, glueWebToolConfig, dialogs) {
	
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
    .error(glueWS.raiseErrorDialog(dialogs, "rendering object"));

	
}]);