gluetoolsApp.controller('nonModeCommandsCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 
		    function($scope, glueWebToolConfig, glueWS, dialogs) {
			
			$scope.commandCategories = null;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-non-mode-commands": {}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.commandCategories = data.webdocsNonModeCommandsSummary.commandCategories;
				console.info('$scope.commandCategories', $scope.commandCategories);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving non mode command documentation"));
			
}]);