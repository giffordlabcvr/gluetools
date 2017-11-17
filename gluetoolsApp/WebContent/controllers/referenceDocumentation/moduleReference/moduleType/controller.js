gluetoolsApp.controller('moduleTypeCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			$scope.moduleDocumentation = null;
			$scope.drugResistanceFindings = null;
			$scope.moduleTypeName = $routeParams.name;
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"document-module-type": {
						"moduleTypeName": $scope.moduleTypeName 
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.moduleDocumentation = data.webdocsModuleTypeDocumentation;
				console.info('$scope.moduleDocumentation', $scope.moduleDocumentation);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving module documentation"));

		}]);
