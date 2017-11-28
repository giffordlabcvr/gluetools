gluetoolsApp.controller('commandReferenceCtrl', 
		[ '$scope', 'glueWebToolConfig', 'glueWS', 'dialogs', 
		    function($scope, glueWebToolConfig, glueWS, dialogs) {
			
			$scope.treeOptions = {
			    nodeChildren: "childCommandModes",
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
			$scope.expandedNodes = [];

			$scope.expandAll = function(node) {
    			$scope.expandedNodes.push(node);
    			_.each(node.childCommandModes, function(n) {$scope.expandAll(n)} );
    		}
			
			glueWS.runGlueCommand("", {
				"webdocs": {
					"list-command-modes":{}
				}
			})
			.success(function(data, status, headers, config) {
		    	$scope.modeTree = { childCommandModes: [data.webdocsCommandModeTree] };
		    	$scope.expandAll($scope.modeTree);
				console.info('command mode tree', $scope.modeTree);
		    })
		    .error(glueWS.raiseErrorDialog(dialogs, "listing command modes"));

}]);