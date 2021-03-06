projectBrowser.controller('selectFilterCtrl',
		[ '$scope', '$modalInstance', 'data', 'filterUtils',
	function($scope, $modalInstance, data, filterUtils){
	$scope.data = data;
	
	addUtilsToScope($scope);
	
	$scope.addFilterElem = function() {
		var filterElem = {};
		$scope.data.newFilterElems.push(filterElem);
	}
	
	$scope.availableOperatorsForType = filterUtils.getFilterOperatorsForType();
	
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
	
	$scope.typeHasOperator = function(type) {
		return filterUtils.typeHasOperator(type);
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