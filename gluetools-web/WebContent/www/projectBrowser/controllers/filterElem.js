projectBrowser.controller('filterElemCtrl',function($scope){
	
	$scope.operator_isopen = false;
	$scope.property_isopen = false;

	$scope.setFilterElemProperty = function(filterProperty) {
		$scope.filterElemProperty = filterProperty;
		$scope.filterElem.property = $scope.filterElemProperty.property;
		$scope.filterElem.type = $scope.filterElemProperty.filterHints.type;
		var availableOperators = $scope.availableOperatorsForFilterHints($scope.filterElemProperty.filterHints);
		$scope.filterElem.predicate = null;
		$scope.setFilterElemOperator(availableOperators[0]);
	}

	$scope.dateOptions = {
		    formatYear: 'yy',
		    startingDay: 1,
		    isopen:false
	};
	
	$scope.openDatePicker = function($event) {
		    $event.preventDefault();
		    $event.stopPropagation();

		    $scope.dateOptions.isopen = true;
	};
	
	$scope.setFilterElemOperator = function(availableOperator) {
		$scope.filterElemOperator = availableOperator;
		if($scope.filterElem.predicate == null) {
			$scope.filterElem.predicate = {};
		}
		$scope.filterElem.predicate.operator = availableOperator.operator;
		console.log("$scope.filterElem.predicate",$scope.filterElem.predicate);
		if(availableOperator.hasOperand && $scope.filterElem.predicate.operand == null) {
			$scope.filterElem.predicate.operand = $scope.defaultOperandForFilterHints($scope.filterElemProperty.filterHints);
		}
		if(!availableOperator.hasOperand) {
			$scope.filterElem.predicate.operand = null;
		}
	}

	console.log("$scope.filterElem",$scope.filterElem);
	if($scope.filterElem.property == null) {
		$scope.setFilterElemProperty($scope.data.filterProperties[0]);
	} else {
		$scope.filterElemProperty = _.find($scope.data.filterProperties, 
				function(fp) { return fp.property == $scope.filterElem.property; });
		var availableOperators = $scope.availableOperatorsForFilterHints($scope.filterElemProperty.filterHints);
		$scope.filterElemOperator = _.find(availableOperators, 
				function(aop) { return aop.operator == $scope.filterElem.predicate.operator; });
	}

	console.log("$scope.filterElemProperty",$scope.filterElemProperty);

	console.log("$scope.filterElemOperator",$scope.filterElemOperator);


	
	
});