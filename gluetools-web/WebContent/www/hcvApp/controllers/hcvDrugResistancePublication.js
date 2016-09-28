hcvApp.controller('hcvDrugResistancePublicationCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.drPubRenderResult = null;
			$scope.drPubId = $routeParams.id;

			glueWS.runGlueCommand("custom-table-row/drug_resistance_publication/"+$scope.drPubId, {
				"render-object": {
					"rendererModuleName":"hcvDrugPublicationRenderer"
				}
			})
			.success(function(data, status, headers, config) {
				$scope.drPubRenderResult = data;
				
				// chained sort: least significant field first
				$scope.drPubRenderResult.drugPublication.drugResistanceFinding = 
				_($scope.drPubRenderResult.drugPublication.drugResistanceFinding).chain()
				  .sortBy('variationSubstitutions')
				  .sortBy('variationFirstCodon')
				  .sortBy('variationFeatureName')
				  .value();
				console.info('$scope.drPubRenderResult', $scope.drPubRenderResult);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "rendering drug resistance publication"));

		}]);
