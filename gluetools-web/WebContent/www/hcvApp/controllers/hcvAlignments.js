hcvApp.controller('hcvAlignmentsCtrl', 
		[ '$scope', '$route', '$routeParams', 'glueWS', 'dialogs', 
    function($scope, $route, $routeParams, glueWS, dialogs) {

			$scope.treeOptions = {
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
				}
			
	$scope.descendentTree = null;
	$scope.expandedNodes = [];
	
	addUtilsToScope($scope);
	
	glueWS.runGlueCommand("alignment/AL_MASTER", {
    	"descendent-tree": {} 
	})
    .success(function(data, status, headers, config) {
    	var rootNode = data.alignmentDescendentTreeResult;
    	$scope.expandedNodes = [rootNode];
		$scope.descendentTree = { childAlignment: [rootNode] };
		console.info('descendent-tree', $scope.descendentTree);
    })
    .error(glueWS.raiseErrorDialog(dialogs, "retrieving descendent-tree"));

	
}]);
