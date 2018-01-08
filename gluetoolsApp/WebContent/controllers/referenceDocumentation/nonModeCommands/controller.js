gluetoolsApp.controller('nonModeCommandsCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', '$location', '$anchorScroll', 
		    function($scope, glueWebToolConfig, glueWS, dialogs, $location, $anchorScroll) {
			
			$scope.commandCategories = null;
			
			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};
			
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