gluetoolsApp.controller('moduleCommandCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			$scope.commandDocumentation = null;
			$scope.moduleTypeName = $routeParams.name;
			$scope.cmdWordID = $routeParams.cmdWordID;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-module-command": {
						"moduleTypeName": $scope.moduleTypeName,
						"cmdWordID": $scope.cmdWordID,
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.commandDocumentation = data.webdocsCommandDocumentation;
				console.info('$scope.commandDocumentation', $scope.commandDocumentation);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving module command documentation"));

		}]);
