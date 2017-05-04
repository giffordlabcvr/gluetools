projectBrowser.controller('filterElemCtrl',function($scope){
	
	$scope.operator_isopen = false;
	$scope.property_isopen = false;
	$scope.month_isopen = false;
	$scope.feature_isopen = false;

	$scope.months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

	$scope.readDate = function() {
		if($scope.filterElemProperty.filterHints.type == "Date" && $scope.filterElemOperator.hasOperand) {
			$scope.date = {
					day:parseInt($scope.filterElem.predicate.operand[0].substring(0,2)),
					month:$scope.filterElem.predicate.operand[0].substring(3,6),
					year:parseInt($scope.filterElem.predicate.operand[0].substring(7,11))
			};
			console.log("$scope.date",$scope.date);
		}
	}
	
	$scope.setFilterElemProperty = function(filterProperty) {
		$scope.filterElemProperty = filterProperty;
		if($scope.filterElemProperty.filterHints.generateCustomDefault != null) {
			$scope.filterElem.custom = $scope.filterElemProperty.filterHints.generateCustomDefault();
		}
		$scope.filterElem.property = $scope.filterElemProperty.property;
		$scope.filterElem.altProperties = $scope.filterElemProperty.altProperties;
		$scope.filterElem.type = $scope.filterElemProperty.filterHints.type;
		$scope.filterElem.predicate = null;
		if($scope.typeHasOperator($scope.filterElem.type)) {
			var availableOperators = $scope.availableOperatorsForFilterHints($scope.filterElemProperty.filterHints);
			$scope.setFilterElemOperator(availableOperators[0]);
		}
		$scope.readDate();
	}

	$scope.setDateMonth = function(dateMonth) {
		$scope.date.month = dateMonth;
	}
	
	$scope.setCustomProperty = function(customProperty, customValue) {
		$scope.filterElem.custom[customProperty] = customValue;
	}
	
	
	$scope.inputTypeForProperty = function() {
		switch($scope.filterElemProperty.filterHints.type) {
		case "Double":
			return "number";
		case "Integer":
			return "number";
		default:
			return "text"
		}
	}
	
	$scope.addOperand = function() {
		$scope.filterElem.predicate.operand.push($scope.defaultOperandForFilterHints($scope.filterElemProperty.filterHints));
	}

	$scope.removeOperand = function(index) {
		$scope.filterElem.predicate.operand.splice(index, 1);
	}

	$scope.setFilterElemOperator = function(availableOperator) {
		$scope.filterElemOperator = availableOperator;
		if($scope.filterElem.predicate == null) {
			$scope.filterElem.predicate = {};
		}
		$scope.filterElem.predicate.operator = availableOperator.operator;
		if(availableOperator.hasOperand) {
			if($scope.filterElem.predicate.operand == null) {
				$scope.filterElem.predicate.operand = [];
				$scope.addOperand();
			} else {
				if(!availableOperator.multiOperand) {
					$scope.filterElem.predicate.operand = [$scope.filterElem.predicate.operand[0]];
				}
			}
			console.log("$scope.filterElem.predicate",$scope.filterElem.predicate);
		} else {
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
		$scope.readDate();
	}

	
	console.log("$scope.filterElemProperty",$scope.filterElemProperty);

	console.log("$scope.filterElemOperator",$scope.filterElemOperator);


	
	
	$scope.$watch("date.day", function(newval,oldval) {
		$scope.updateDateOperand();
	});
	$scope.$watch("date.month", function(newval,oldval) {
		$scope.updateDateOperand();
	});
	$scope.$watch("date.year", function(newval,oldval) {
		$scope.updateDateOperand();
	});
	
	$scope.updateDateOperand = function() {
		if($scope.date == null) {
			return;
		}
		var day = ""+$scope.date.day;
		while(day.length < 2) {
			day = "0"+day;
		}
		var year = ""+$scope.date.year;
		while(year.length < 4) {
			year = "0"+year;
		}
		$scope.filterElem.predicate.operand[0] = day+"-"+$scope.date.month+"-"+year;
	}
	
	
});