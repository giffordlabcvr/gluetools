projectBrowser.controller('alignmentCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 
		    function($scope, glueWebToolConfig, glueWS, dialogs) {

			addUtilsToScope($scope);
			$scope.memberList = null;
			$scope.renderResult = null;
			
			$scope.init = function(almtName, almtRendererModuleName, memberWhereClause, memberFields) {
				var renderCmdParams = {};
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
				
				glueWS.runGlueCommand("alignment/"+almtName, {
					"list":{
						"member":{
							"whereClause":memberWhereClause,
				            "fieldName":memberFields
						}
					}
				})
				.success(function(data, status, headers, config) {
					$scope.memberList = tableResultAsObjectList(data);
					console.info('$scope.memberList', $scope.memberList);
				})
				.error(glueWS.raiseErrorDialog(dialogs, "listing alignment members"));
			}
}]);