gluetoolsApp.controller('commandModeCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {
			
			
			$scope.commandModeDocumentation = null;
			$scope.absoluteModePathID = $routeParams.absoluteModePathID;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-command-mode": {
						"absoluteModePathID": $scope.absoluteModePathID 
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.commandModeDocumentation = data.webdocsCommandModeDocumentation;
				console.info('$scope.commandModeDocumentation', $scope.commandModeDocumentation);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving command mode documentation"));

			
		}]);