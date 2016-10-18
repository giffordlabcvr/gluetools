projectBrowser.controller('alignmentCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 'pagingContext', 
		    function($scope, glueWebToolConfig, glueWS, dialogs, pagingContext) {

			addUtilsToScope($scope);
			$scope.memberList = null;
			$scope.renderResult = null;
			$scope.memberWhereClause = null;
			$scope.memberFields = null;
			
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
				})
				.error(glueWS.raiseErrorDialog(dialogs, "rendering alignment"));
				
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"count": { 
			    		"member": {
			    			whereClause: $scope.memberWhereClause
			    		}, 
			    	} 
				})
			    .success(function(data, status, headers, config) {
					console.info('count almt-member raw result', data);
					$scope.pagingContext = pagingContext.createPagingContext(data.countResult.count, 10, $scope.updatePage);
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "counting alignment members"));
			}
			
			
			$scope.updatePage = function(pContext) {
				console.log("updatePage", pContext);
				var cmdParams = {
			            "fieldName":$scope.memberFields,
					    "whereClause": $scope.memberWhereClause
				};
				cmdParams.pageSize = pContext.itemsPerPage;
				cmdParams.fetchLimit = pContext.itemsPerPage;
				cmdParams.fetchOffset = pContext.firstItemIndex - 1;
				glueWS.runGlueCommand("alignment/"+$scope.almtName, {
			    	"list": { "member": cmdParams } 
				})
			    .success(function(data, status, headers, config) {
					  console.info('list almt-member raw result', data);
					  $scope.memberList = tableResultAsObjectList(data);
					  console.info('list almt-member result as object list', $scope.memberList);
			    })
			    .error(glueWS.raiseErrorDialog(dialogs, "listing alignment members"));
			}
			
}]);