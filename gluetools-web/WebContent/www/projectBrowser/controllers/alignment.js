projectBrowser.controller('alignmentCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 'pagingContext', 
		    function($scope, glueWebToolConfig, glueWS, dialogs, pagingContext) {

			addUtilsToScope($scope);
			$scope.memberList = null;
			$scope.renderResult = null;
			$scope.memberWhereClause = null;
			$scope.memberFields = null;
			$scope.loadingSpinner = false;
			
			$scope.downloadAlignment = function(referenceName, fastaAlignmentExporter, fastaProteinAlignmentExporter) {
				console.log("Download sequence alignment, referenceName: "+referenceName);
				glueWS.runGlueCommand("reference/"+referenceName, {
				    "show":{
				        "feature":{
				            "tree":{}
				        }
				    }
				})
				.success(function(data, status, headers, config) {
					console.info('featureTree', data.referenceFeatureTreeResult);
					var dlg = dialogs.create(
							glueWebToolConfig.getProjectBrowserURL()+'/dialogs/configureAlignment.html','configureAlignmentCtrl',
							{ featureTree:data.referenceFeatureTreeResult }, {});
					dlg.result.then(function(data){
						console.info('data', data);
					});
				})
				.error(glueWS.raiseErrorDialog(dialogs, "retrieving reference feature tree"));

				
				
			}

			$scope.updateCount = function(pContext) {
				$scope.memberList = null;
				$scope.loadingSpinner = true;
				
				var cmdParams = {
		    	        "recursive":"true",
		    			"whereClause": $scope.memberWhereClause
		    	};
				pContext.extendCountCmdParams(cmdParams);

				
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"count": { 
			    		"member": cmdParams, 
			    	} 
				})
			    .success(function(data, status, headers, config) {
					console.info('count almt-member raw result', data);
					$scope.pagingContext.setTotalItems(data.countResult.count);
					$scope.pagingContext.firstPage();
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "counting alignment members");
			    	fn(data, status, headers, config);
			    });
			}
			
			$scope.updatePage = function(pContext) {
				console.log("updatePage", pContext);
				var cmdParams = {
		    	        "recursive":"true",
			            "fieldName":$scope.memberFields,
					    "whereClause": $scope.memberWhereClause
				};
				pContext.extendListCmdParams(cmdParams);
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"list": { "member": cmdParams } 
				})
			    .success(function(data, status, headers, config) {
					  $scope.loadingSpinner = false;
					  console.info('list almt-member raw result', data);
					  $scope.memberList = tableResultAsObjectList(data);
					  console.info('list almt-member result as object list', $scope.memberList);
			    })
			    .error(function(data, status, headers, config) {
					$scope.loadingSpinner = false;
			    	var fn = glueWS.raiseErrorDialog(dialogs, "listing alignment members");
			    	fn(data, status, headers, config);
			    });
			}

			$scope.init = function(almtName, almtRendererModuleName, memberWhereClause, memberFields) {
				var renderCmdParams = {};
				$scope.memberFields = memberFields;
				$scope.almtName = almtName;
				$scope.memberWhereClause = memberWhereClause;
				if(almtRendererModuleName) {
					renderCmdParams.rendererModuleName = almtRendererModuleName;
				}
				glueWS.runGlueCommand("alignment/"+almtName, {
				    "render-object":renderCmdParams
				})
				.success(function(data, status, headers, config) {
					$scope.renderResult = data;
					console.info('$scope.renderResult', $scope.renderResult);
					$scope.pagingContext.countChanged();
				})
				.error(glueWS.raiseErrorDialog(dialogs, "rendering alignment"));
			}

			$scope.pagingContext = pagingContext.createPagingContext($scope.updateCount, $scope.updatePage);
			
}]);