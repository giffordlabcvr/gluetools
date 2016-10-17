projectBrowser.controller('referenceCtrl', 
		[ '$scope', '$routeParams', '$controller', 'glueWS', 'dialogs', 'rendererModule',
		    function($scope, $routeParams, $controller, glueWS, dialogs, rendererModule) {

	$scope.referenceName = $routeParams.referenceName;
	$scope.glueObjectPath = "reference/"+$scope.referenceName;
	
	$controller('renderableObjectBaseCtrl', { 
		$scope: $scope, 
		glueObjectPath: $scope.glueObjectPath,
		rendererModule: rendererModule
	});	

	$scope.featureTree = null;

	$scope.treeOptions = {
	    nodeChildren: "features",
	    dirSelectable: true,
	    allowDeselect: false,
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
	
	$scope.expandedNodes = []; 
	
	$scope.selectedNode = null;
	
	$scope.expandAll = function(node) {
		$scope.expandedNodes.push(node);
		_.each(node.features, function(n) {$scope.expandAll(n)} );
	}
	
	glueWS.runGlueCommand("reference/"+$scope.referenceName, {
	    "show":{
	        "feature":{
	            "tree":{}
	        }
	    }
	})
	.success(function(data, status, headers, config) {
    	$scope.featureTree = data.referenceFeatureTreeResult;
    	$scope.expandAll($scope.featureTree);
    	if($scope.featureTree.features.length > 0) {
    		$scope.selectedNode = $scope.featureTree.features[0];
    	}
		console.info('featureTree', $scope.featureTree);
	})
	.error(glueWS.raiseErrorDialog(dialogs, "showing feature tree"));
	
}]);
