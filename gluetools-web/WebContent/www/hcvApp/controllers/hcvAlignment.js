hcvApp.controller('hcvAlignmentCtrl', 
		[ '$scope', '$routeParams', '$controller', 'glueWS', 'dialogs',
		  function($scope, $routeParams, $controller, glueWS, dialogs) {
			$scope.alignmentName = $routeParams.alignmentName;

			addUtilsToScope($scope);

			$scope.memberList = null;
			$scope.renderResult = null;

			glueWS.runGlueCommand("alignment/"+$scope.alignmentName, {
			    "render-object":{
			        "rendererModuleName":"hcvAlignmentRenderer"
			    }
			})
			.success(function(data, status, headers, config) {
				$scope.renderResult = data;
				console.info('$scope.renderResult', $scope.renderResult);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "rendering alignment"));

			
			glueWS.runGlueCommand("alignment/"+$scope.alignmentName, {
				"list":{
					"member":{
						"whereClause":"sequence.source.name = 'ncbi-curated'",
			            "fieldName":[
			                         "sequence.sequenceID",
			                         "sequence.gb_country_official",
			                         "sequence.gb_collection_year",
			                         "sequence.gb_length",
			                         "sequence.gb_create_date",
			                         "sequence.gb_pubmed_id",
			                         "sequence.gb_isolate"
			                     ]
					}
				}
			})
			.success(function(data, status, headers, config) {
				$scope.memberList = tableResultAsObjectList(data);
				console.info('$scope.memberList', $scope.memberList);
			})
			.error(glueWS.raiseErrorDialog(dialogs, "listing alignment members"));
		}]);
