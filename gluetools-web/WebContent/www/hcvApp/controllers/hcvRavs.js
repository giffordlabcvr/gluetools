hcvApp.controller('hcvRavsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.ravs = [];

			glueWS.runGlueCommand("", {
			    "list":{
			        "variation":{
			            "whereClause":"is_resistance_associated_variant = 'true'",
			            "fieldName":[
			                "featureLoc.feature.name",
			                "rav_substitutions",
			                "rav_first_codon",
			                "featureLoc.referenceSequence.name",
			                "name"
			            ]
			        }
			    }
			})
			.success(function(data, status, headers, config) {
				$scope.ravs = tableResultAsObjectList(data);
				// chained sort: least significant field first
				$scope.ravs = _($scope.ravs).chain()
				  .sortBy('rav_substitutions')
				  .sortBy('rav_first_codon')
				  .sortBy('featureLoc.feature.name')
				  .value();
				console.info('$scope.ravs', $scope.ravs);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving resistance associated variants"));

		}]);
