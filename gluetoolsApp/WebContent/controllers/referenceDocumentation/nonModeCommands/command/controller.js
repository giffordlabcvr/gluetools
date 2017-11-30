gluetoolsApp.controller('nonModeCommandCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {
			
			$scope.commandDocumentation = null;
			$scope.cmdWordID = $routeParams.cmdWordID;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-non-mode-command": {
						"cmdWordID": $scope.cmdWordID,
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.commandDocumentation = data.webdocsCommandDocumentation;
				console.info('$scope.commandDocumentation', $scope.commandDocumentation);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving non-mode command documentation"));

}]);