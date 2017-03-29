projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 'glueWebToolConfig', 'pagingContext', 'FileSaver',
    function($scope, $route, $routeParams, glueWS, dialogs, glueWebToolConfig, pagingContext, FileSaver) {

			$scope.listSequenceResult = null;
			$scope.pagingContext = null;
			$scope.whereClause = null;
			$scope.fieldNames = null;

			addUtilsToScope($scope);
			
			$scope.updateCount = function(pContext) {
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
			    .error(glueWS.raiseErrorDialog(dialogs, "counting sequences"));
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
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "listing sequences"));
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
					cmdParams.allSequences = false;
				} else {
					cmdParams.allSequences = true;
				}
				$scope.pagingContext.extendCmdParamsWhereClause(cmdParams);

				/*
				var dlg = dialogs.create(
						glueWebToolConfig.getProjectBrowserURL()+'/dialogs/glueWait.html','glueWaitCtrl',
						{ message: "FASTA sequence download in progress" }, {});

				console.log("dlg", dlg);
				*/
				glueWS.runGlueCommandLong("module/"+moduleName, {
			    	"web-export": cmdParams	
				},
				"Download of FASTA sequences in progress")
			    .success(function(data, status, headers, config) {
			    	console.info('web export raw result', data);
			    	var blob = $scope.b64ToBlob(data.fastaExportResult.base64, "text/plain", 512);
/*			    	dlg.close();*/
				    FileSaver.saveAs(blob, "sequences.fasta");
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "downloading sequences"));
/*			    .error(function(data, status, headers, config) {
			    	dlg.close();
			    	var fn = glueWS.raiseErrorDialog(dialogs, "downloading sequences");
			    	fn(data, status, headers, config);
			    }); */
			}

}]);
