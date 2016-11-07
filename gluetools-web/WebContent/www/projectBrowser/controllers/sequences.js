projectBrowser.controller('sequencesCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 'pagingContext',
    function($scope, $route, $routeParams, glueWS, dialogs, pagingContext) {

			$scope.listSequenceResult = null;
			$scope.pagingContext = null;
			$scope.whereClause = null;
			$scope.fieldNames = null;

			addUtilsToScope($scope);
			
			$scope.init = function(whereClause, fieldNames)  {
				$scope.whereClause = whereClause;
				$scope.fieldNames = fieldNames;

				var cmdParams = {};
				if($scope.whereClause) {
					cmdParams.whereClause = $scope.whereClause;
				}
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
				cmdParams.pageSize = pContext.itemsPerPage;
				cmdParams.fetchLimit = pContext.itemsPerPage;
				cmdParams.fetchOffset = pContext.firstItemIndex - 1;
				var glueSortOrder = pContext.getGlueSortOrder();
				if(glueSortOrder != null) {
					cmdParams.sortProperties = glueSortOrder;
				}
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
			
			$scope.pagingContext = pagingContext.createPagingContext($scope.updatePage);
}]);
