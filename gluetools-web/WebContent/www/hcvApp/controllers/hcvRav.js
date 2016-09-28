hcvApp.controller('hcvRavCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.rav = null;
			$scope.referenceName = $routeParams.referenceName;
			$scope.featureName = $routeParams.featureName;
			$scope.variationName = $routeParams.variationName;

			glueWS.runGlueCommand("reference/"+$scope.referenceName+
					"/feature-location/"+$scope.featureName+
					"/variation/"+$scope.variationName, {
			    "render-object":{
			        "rendererModuleName":"hcvResistanceAssociatedVariantRenderer"
			    }
			})
			.success(function(data, status, headers, config) {
				$scope.rav = data.resistance_associated_variant;
				console.info('$scope.rav', $scope.rav);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "rendering RAV"));

		}]);
