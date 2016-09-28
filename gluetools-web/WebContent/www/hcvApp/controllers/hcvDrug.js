hcvApp.controller('hcvDrugCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.drugRenderResult = null;
			$scope.drugId = $routeParams.id;

			glueWS.runGlueCommand("custom-table-row/drug/"+$scope.drugId, {
				"render-object": {
					"rendererModuleName":"hcvDrugRenderer"
				}
			})
			.success(function(data, status, headers, config) {
				$scope.drugRenderResult = data;
				console.info('$scope.drugRenderResult', $scope.drugRenderResult);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "rendering drug"));

		}]);
