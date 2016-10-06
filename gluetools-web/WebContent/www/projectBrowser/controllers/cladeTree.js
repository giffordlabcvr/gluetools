projectBrowser.controller('cladeTreeCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 
		    function($scope, glueWebToolConfig, glueWS, dialogs) {

			$scope.almtNameToTreeOptions = {};
			$scope.almtNameToExpandedNodes = {};
			$scope.almtNameToDescendentTree = {};

			addUtilsToScope($scope);
			
			$scope.initFromRootNodes = function(rootNodes) {
				_.each(rootNodes, function(rootNode) {
					
					$scope.almtNameToTreeOptions[rootNode.almtName] = {
					    nodeChildren: "childAlignment",
					    dirSelectable: true,
					    injectClasses: {
					        ul: "a1",
					        li: "a2",
					        liSelected: "a7",
					        iExpanded: "a3",
					        iCollapsed: "a4",
					        iLeaf: "a5",
					        label: "a6",
					        labelSelected: "a8"
					    }
					};
					$scope.almtNameToExpandedNodes[rootNode.almtName] = [];
					$scope.almtNameToDescendentTree[rootNode.almtName] = null;

					glueWS.runGlueCommand("alignment/"+rootNode.almtName, {
				    	"descendent-tree": {} 
					})
				    .success(function(data, status, headers, config) {
				    	var topNode = data.alignmentDescendentTreeResult;
				    	if(rootNode.initiallyExpanded) {
							$scope.almtNameToExpandedNodes[rootNode.almtName] = [topNode];
				    	}
						$scope.almtNameToDescendentTree[rootNode.almtName] = { childAlignment: [topNode] };
						console.info('descendent-tree', $scope.almtNameToDescendentTree[rootNode.almtName]);
				    })
				    .error(glueWS.raiseErrorDialog(dialogs, "retrieving descendent-tree "+rootNode.almtName));
				});
			}

}]);