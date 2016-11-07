projectBrowser.controller('selectSortOrderCtrl',function($scope,$modalInstance,data){
	$scope.data = data;
	$scope.updating = false;
	
	$scope.sortByDisplayName = function(propList) {
		return _.sortBy(propList, "displayName");
	}

	$scope.initAvailable = function() {
		$scope.data.available = [];
		_.each($scope.data.sortableProperties, function(prop) {
			if(!_.find($scope.data.newSortOrder, function(sortOrderProp) {return sortOrderProp.property == prop.property} )) {
				$scope.data.available.push(prop);
			}
		});
		$scope.data.available = $scope.sortByDisplayName($scope.data.available);
	}

	$scope.initAvailable();
	
	$scope.addToSortOrder = function(availableProp) {
		$scope.data.available = _.without($scope.data.available, availableProp);
		var sortOrderProp = {
				property: availableProp.property,
				displayName: availableProp.displayName,
				order: "+"
		};
		$scope.data.newSortOrder.push(sortOrderProp);
	}
	
	$scope.removeFromSortOrder = function(sortOrderProp) {
		$scope.data.newSortOrder = _.without($scope.data.newSortOrder, sortOrderProp);
		var prop = _.find($scope.data.sortableProperties, 
				function(prop) {return prop.property == sortOrderProp.property} );
		$scope.data.available.push(prop);
		$scope.data.available = $scope.sortByDisplayName($scope.data.available);
	}

	$scope.resetToDefault = function() {
		$scope.data.newSortOrder = _($scope.data.defaultSortOrder).clone()
		$scope.initAvailable();
	}
	
	$scope.accept = function(){
		$modalInstance.close($scope.data);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

});