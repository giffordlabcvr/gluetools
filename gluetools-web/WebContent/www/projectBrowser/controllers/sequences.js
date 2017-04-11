projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 'glueWebToolConfig', 'pagingContext', 'FileSaver', 'saveFile',
    function($scope, $route, $routeParams, glueWS, dialogs, glueWebToolConfig, pagingContext, FileSaver, saveFile) {

			$scope.listSequenceResult = null;
			$scope.pagingContext = null;
			$scope.whereClause = null;
			$scope.fieldNames = null;
			$scope.loadingSpinner = false;

			addUtilsToScope($scope);
			
			$scope.updateCount = function(pContext) {
				$scope.listSequenceResult = null;
				$scope.loadingSpinner = true;

				var cmdParams = {};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				$scope.pagingContext.extendCountCmdParams(cmdParams);
				glueWS.runGlueCommand("", {
			    	"count": { "sequence": cmdParams	 } 
				})
			    .success(function(data, status, headers, config) {
					console.info('count sequence raw result', data);
					$scope.pagingContext.setTotalItems(data.countResult.count);
					$scope.pagingContext.firstPage();
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "counting sequences");
			    	fn(data, status, headers, config);
			    });
			}
				
			$scope.updatePage = function(pContext) {
				console.log("updatePage", pContext);
				var cmdParams = {
			            "fieldName":$scope.fieldNames
				};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				pContext.extendListCmdParams(cmdParams);
				glueWS.runGlueCommand("", {
			    	"list": { "sequence": cmdParams } 
				})
			    .success(function(data, status, headers, config) {
					  console.info('list sequence raw result', data);
					  $scope.listSequenceResult = tableResultAsObjectList(data);
					  console.info('list sequence result as object list', $scope.listSequenceResult);
					  $scope.loadingSpinner = false;
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "listing sequences");
			    	fn(data, status, headers, config);
			    });
			}

			$scope.init = function(whereClause, fieldNames)  {
				$scope.whereClause = whereClause;
				$scope.fieldNames = fieldNames;
				$scope.pagingContext = pagingContext.createPagingContext($scope.updateCount, $scope.updatePage);
				$scope.pagingContext.countChanged();
			}
			
			$scope.downloadSequences = function(moduleName) {
				console.log("Downloading sequences, using module '"+moduleName+"'");
				var cmdParams = {};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);
				if(cmdParams.whereClause != null) {
					cmdParams.allSequences = false;
				} else {
					cmdParams.allSequences = true;
				}

				glueWS.runGlueCommandLong("module/"+moduleName, {
			    	"web-export": cmdParams	
				},
				"FASTA sequence download in progress")
			    .success(function(data, status, headers, config) {
			    	var blob = $scope.b64ToBlob(data.fastaExportResult.base64, "text/plain", 512);
				    saveFile.saveFile(blob, "FASTA sequence file", "sequences.fasta");
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "downloading sequences"));
			}
			
			
			$scope.downloadSequenceMetadata = function() {
				console.log("Downloading sequence metadata");
				var cmdParams = {
		            "fieldName": $scope.fieldNames
			    };
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
				$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);
				$scope.pagingContext.extendCmdParamsSortOrder(cmdParams);

				var glueHeaders = {
						"glue-binary-table-result" : true,
						"glue-binary-table-result-format" : "TAB"
				};
				
				glueWS.runGlueCommandLong("", {
			    	"list": {
			    		"sequence" : cmdParams
			    	},
				},
				"Sequence metadata download in progress",
		    	glueHeaders)
			    .success(function(data, status, headers, config) {
			    	var blob = $scope.b64ToBlob(data.binaryTableResult.base64, "text/plain", 512);
				    saveFile.saveFile(blob, "sequence metadata file", "sequence_metadata.txt");
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "downloading sequence metadata"));
			}

}]);
