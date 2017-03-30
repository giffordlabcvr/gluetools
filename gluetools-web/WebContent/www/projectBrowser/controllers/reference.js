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

	
	glueWS.runGlueCommand("reference/"+$scope.referenceName, {
	    "show":{
	        "feature":{
	            "tree":{}
	        }
	    }
	})
	.success(function(data, status, headers, config) {
    	$scope.featureTree = data.referenceFeatureTreeResult;
		console.info('featureTree', $scope.featureTree);
	})
	.error(glueWS.raiseErrorDialog(dialogs, "showing feature tree"));
	
}]);
