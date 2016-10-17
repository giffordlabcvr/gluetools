projectBrowser.controller('renderableObjectBaseCtrl', 
		[ '$scope', 'glueObjectPath', 'glueWS', 'glueWebToolConfig', 'dialogs', 'rendererModule',
        function($scope, glueObjectPath, glueWS, glueWebToolConfig, dialogs, rendererModule) {
	
	$scope.projectBrowserURL = glueWebToolConfig.getProjectBrowserURL();

	console.log("renderableObjectBaseCtrl");
	$scope.renderResult = null;
	var cmdParams = {};
	if(rendererModule != null) {
		cmdParams.rendererModuleName = rendererModule
	}
	glueWS.runGlueCommand($scope.glueObjectPath, {
    	"render-object": cmdParams
	})
    .success(function(data, status, headers, config) {
		  console.info('render result', data);
		  $scope.renderResult = data;
    })
    .error(glueWS.raiseErrorDialog(dialogs, "rendering object"));

	
}]);