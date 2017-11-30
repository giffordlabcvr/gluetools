gluetoolsApp.controller('commandCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			$scope.commandDocumentation = null;
			$scope.absoluteModePathID = $routeParams.absoluteModePathID;
			$scope.modeDescription = null;
			$scope.cmdWordID = $routeParams.cmdWordID;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-mode-command": {
						"absoluteModePathID": $scope.absoluteModePathID,
						"cmdWordID": $scope.cmdWordID,
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.commandDocumentation = data.webdocsModeCommandDocumentation.commandDocumentation;
				$scope.modeDescription = data.webdocsModeCommandDocumentation.modeDescription;
				console.info('$scope.commandDocumentation', $scope.commandDocumentation);
				console.info('$scope.modeDescription', $scope.modeDescription);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving mode command documentation"));

		}]);
