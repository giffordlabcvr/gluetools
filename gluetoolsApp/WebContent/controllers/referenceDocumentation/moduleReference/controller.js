gluetoolsApp.controller('moduleReferenceCtrl', 
		[ '$scope', '$http', 'glueWS', 'dialogs',
		function($scope, $http, glueWS, dialogs) {
			
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"list-module-types":{}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.moduleTypeCategories = data.webdocsModuleTypeList.moduleTypeCategories;
				console.info('$scope.moduleTypeCategories', $scope.moduleTypeCategories);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "listing module types"));
			
		} ]);
