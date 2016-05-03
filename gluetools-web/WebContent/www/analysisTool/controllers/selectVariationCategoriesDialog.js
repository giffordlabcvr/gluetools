analysisTool.controller('selectVariationCategoriesCtrl',function($scope,$modalInstance,data){
	$scope.data = data;
	
	$scope.sortByDisplayName = function(vCatList) {
		return _.sortBy(vCatList, "displayName");
	}

	$scope.data.available = [];

	_.each($scope.data.variationCategories, function(vCat1) {
		if(!_.find($scope.data.included, function(vCat2) {return vCat2.name == vCat1.name} )) {
			$scope.data.available.push(vCat1);
		}
	});
	
	$scope.data.included = $scope.sortByDisplayName($scope.data.included);
	$scope.data.available = $scope.sortByDisplayName($scope.data.available);

	$scope.addToIncluded = function(vCat) {
		$scope.data.available = _.without($scope.data.available, vCat);
		$scope.data.included.push(vCat);
		$scope.data.included = $scope.sortByDisplayName($scope.data.included);
	}

	$scope.removeFromIncluded = function(vCat) {
		$scope.data.included = _.without($scope.data.included, vCat);
		$scope.data.available.push(vCat);
		$scope.data.available = $scope.sortByDisplayName($scope.data.available);
	}

	$scope.accept = function(){
		$modalInstance.close($scope.data);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

});