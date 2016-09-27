hcvApp.controller('hcvDrugsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
		  function($scope, $route, $routeParams, glueWS, dialogs) {

			addUtilsToScope($scope);

			$scope.drugs = [];
			$scope.categoryIdToDisplayName = {};

			glueWS.runGlueCommand("", {
				"list":{
					"custom-table-row":{
						"tableName":"drug_category",
						"fieldName":[
						             "id",
						             "display_name"
						             ]
					}
				}
			})
			.success(function(data, status, headers, config) {
				var drugCategories = tableResultAsObjectList(data);
				_.each(drugCategories, function(drugCategory) {
					$scope.categoryIdToDisplayName["custom-table-row/drug_category/"+drugCategory.id] = drugCategory.display_name;
				});
				console.info('$scope.categoryIdToDisplayName', $scope.categoryIdToDisplayName);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving drug categories"));

			glueWS.runGlueCommand("", {
			    "list":{
			        "custom-table-row":{
			            "tableName":"drug",
			            "fieldName":[
						    "id",
			                "drug_category",
			                "company_code",
			            ]
			        }
			    }
			})
			.success(function(data, status, headers, config) {
				$scope.drugs = _.sortBy(tableResultAsObjectList(data), "drug_category");
				console.info('$scope.drugs', $scope.drugs);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "retrieving drug"));

		}]);
