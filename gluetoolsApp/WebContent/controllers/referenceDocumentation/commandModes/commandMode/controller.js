gluetoolsApp.controller('commandModeCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', '$location', '$anchorScroll', 
		  function($scope, $route, $routeParams, glueWS, dialogs, $location, $anchorScroll) {
			
			
			$scope.commandModeDocumentation = null;
			$scope.absoluteModePathID = $routeParams.absoluteModePathID;
			
			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
				};
				
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
