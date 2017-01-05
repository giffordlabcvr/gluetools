projectBrowser.controller('selectFilterCtrl',
		[ '$scope', '$modalInstance', 'data', 'filterUtils',
	function($scope, $modalInstance, data, filterUtils){
	$scope.data = data;
	
	
	$scope.addFilterElem = function() {
		var filterElem = {};
		$scope.data.newFilterElems.push(filterElem);
	}
	
	$scope.availableOperatorsForType = filterUtils.filterOperatorsForType();
	
	$scope.defaultOperandForType = filterUtils.defaultOperandForType();

	$scope.availableOperatorsForFilterHints = function(filterHints) {
		var forType = $scope.availableOperatorsForType[filterHints.type];
		if(forType != null) {
			return forType;
		}
		return [
		           {operator:"equals", displayName:"equals", operand:""}
		];
	}

	$scope.defaultOperandForFilterHints = function(filterHints) {
		var forType = $scope.defaultOperandForType[filterHints.type];
		if(forType != null) {
			return forType;
		}
		return "";
	}

	
	
	$scope.removeFilterElem = function(filterElem) {
		$scope.data.newFilterElems = _.without($scope.data.newFilterElems, filterElem);
	}

	
	$scope.resetToDefault = function() {
		$scope.data.newFilterElems = _($scope.data.defaultFilterElems).clone()
	}
	
	$scope.accept = function(){
		$modalInstance.close($scope.data);
	}; 

	$scope.dismiss = function(){
		$modalInstance.dismiss('Dismissed');
	}; 

}]);