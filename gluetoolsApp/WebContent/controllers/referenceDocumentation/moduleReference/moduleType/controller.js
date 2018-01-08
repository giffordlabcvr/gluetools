gluetoolsApp.controller('moduleTypeCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', '$location', '$anchorScroll', 
		  function($scope, $route, $routeParams, glueWS, dialogs, $location, $anchorScroll) {

			$scope.moduleDocumentation = null;
			$scope.moduleTypeName = $routeParams.name;
			
			$scope.scrollTo = function(id) {
				var old = $location.hash();
				$location.hash(id);
				$anchorScroll();
				$location.hash(old);
			};

			
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
