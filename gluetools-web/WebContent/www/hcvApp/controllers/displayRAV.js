analysisTool.controller('displayRAVCtrl', ['$scope', '$modalInstance', '$controller', 'data',
    function($scope, $modalInstance, $controller, data) {
	$controller('displayVariationBase', { $scope: $scope, 
		$modalInstance: $modalInstance,
		variationCategory: data.variationCategory, 
		variation: data.renderedVariation.resistance_associated_variant, 
		ancestorAlmtNames: data.ancestorAlmtNames});
	
}]);
