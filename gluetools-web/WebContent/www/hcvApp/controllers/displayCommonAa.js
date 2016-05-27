analysisTool.controller('displayCommonAaCtrl', ['$scope', '$modalInstance', '$controller', 'data',
    function($scope, $modalInstance, $controller, data) {
	$controller('displayVariationBase', { $scope: $scope, 
		$modalInstance: $modalInstance,
		variationCategory: data.variationCategory, 
		variation: data.renderedVariation.common_aa_polymorphism, 
		ancestorAlmtNames: data.ancestorAlmtNames});
}]);

