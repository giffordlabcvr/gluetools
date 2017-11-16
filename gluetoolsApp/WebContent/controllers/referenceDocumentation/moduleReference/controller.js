gluetoolsApp.controller('moduleReferenceCtrl', 
		[ '$scope', '$http', 'glueWS', 'dialogs',
		function($scope, $http, glueWS, dialogs) {
			
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"list-module-types":{}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.moduleTypes = tableResultAsObjectList(data);
				console.info('$scope.moduleTypes', $scope.moduleTypes);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "listing module types"));
			
		} ]);
