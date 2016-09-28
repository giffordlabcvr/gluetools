hcvApp.controller('hcvDrugResistancePublicationsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.drugResistancePublications = [];

			glueWS.runGlueCommand("", {
			    "list":{
			        "custom-table-row":{
			            "tableName":"drug_resistance_publication",
			            "fieldName":[
			                "id",
			                "display_name",
			                "publication",
			                "pub_location",
			                "authors",
			                "title",
			                "year",
			                "web_link"
			            ]
			        }
			    }
			})
			.success(function(data, status, headers, config) {
				$scope.drugResistancePublications = tableResultAsObjectList(data);
				console.info('$scope.drugResistancePublications', $scope.drugResistancePublications);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving drug categories"));

		}]);
