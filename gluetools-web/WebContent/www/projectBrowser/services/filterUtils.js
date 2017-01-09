projectBrowser.service("filterUtils", ['$filter', function($filter) {
	
	this.filterOperatorsForType = { 
			"String": [
			           {operator:"like", displayName:"is like", multiDisplayName:"is like one of", hasOperand:true, multiOperand:true, cayenneOperator:"like",
			        	   preTransformOperand: function(op) { return "%"+op+"%"} }, 
			           {operator:"notlike", displayName:"is not like", multiDisplayName:"is not like any of", hasOperand:true, multiOperand:true, cayenneOperator:"like", negate:true, 
			        	   preTransformOperand: function(op) { return "%"+op+"%"}}, 
			           {operator:"matches", displayName:"matches", multiDisplayName:"matches one of", hasOperand:true, multiOperand:true, cayenneOperator:"="}, 
			           {operator:"notmatches", displayName:"does not match", multiDisplayName:"does not match any of", hasOperand:true, multiOperand:true, cayenneOperator:"=", negate:true}, 
			           {operator:"isnull", displayName:"is null", hasOperand:false, cayenneOperator:"= null"},
			           {operator:"isnotnull", displayName:"is not null", hasOperand:false, cayenneOperator:"!= null"}
			],
			"Integer" : [
				           {operator:"equals", displayName:"equals", multiDisplayName:"equals one of", hasOperand:true, multiOperand:true, cayenneOperator:"="}, 
				           {operator:"notequals", displayName:"does not equal", multiDisplayName:"does not equal any of", hasOperand:true, multiOperand:true, cayenneOperator:"=", negate:true}, 
				           {operator:"gt", displayName:">", hasOperand:true, cayenneOperator:">"}, 
				           {operator:"gte", displayName:">=", hasOperand:true, cayenneOperator:">="}, 
				           {operator:"lt", displayName:"<", hasOperand:true, cayenneOperator:"<"}, 
				           {operator:"lte", displayName:"<=", hasOperand:true, cayenneOperator:"<="}, 
				           {operator:"isnull", displayName:"is null", hasOperand:false, cayenneOperator:"= null"},
				           {operator:"isnotnull", displayName:"is not null", hasOperand:false, cayenneOperator:"!= null"}
			],
			"Double" : [
				           {operator:"equals", displayName:"equals", hasOperand:true, cayenneOperator:"="}, 
				           {operator:"notequals", displayName:"does not equal", hasOperand:true, cayenneOperator:"!="}, 
				           {operator:"gt", displayName:">", hasOperand:true, cayenneOperator:">"}, 
				           {operator:"gte", displayName:">=", hasOperand:true, cayenneOperator:">="}, 
				           {operator:"lt", displayName:"<", hasOperand:true, cayenneOperator:"<"}, 
				           {operator:"lte", displayName:"<=", hasOperand:true, cayenneOperator:"<="}, 
				           {operator:"isnull", displayName:"is null", hasOperand:false, cayenneOperator:"= null"},
				           {operator:"isnotnull", displayName:"is not null", hasOperand:false, cayenneOperator:"!= null"}
			],
			"Date" : [
				           {operator:"equals", displayName:"equals", hasOperand:true, cayenneOperator:"="}, 
				           {operator:"notequals", displayName:"does not equal", hasOperand:true, cayenneOperator:"!="}, 
				           {operator:"after", displayName:"after", hasOperand:true, cayenneOperator:">"}, 
				           {operator:"onorafter", displayName:"on or after", hasOperand:true, cayenneOperator:">="}, 
				           {operator:"before", displayName:"before", hasOperand:true, cayenneOperator:"<"}, 
				           {operator:"onorbefore", displayName:"on or before", hasOperand:true, cayenneOperator:"<="}, 
				           {operator:"isnull", displayName:"is null", hasOperand:false, cayenneOperator:"= null"},
				           {operator:"isnotnull", displayName:"is not null", hasOperand:false, cayenneOperator:"!= null"}
			]
		};
	
	this.getFilterOperatorsForType = function() {
		return this.filterOperatorsForType;
	};

	this.defaultOperandForType = function() {
		return {
			"String": "",
			"Integer" : 0,
			"Double" : 0.0,
			"Date" : "01-Jan-1970"
		};
	};

	this.transformOperand = function(type, operand) {
		switch(type) {
		case "Date":
			return "#gluedate("+$filter('date')(operand, "dd-MMM-yyyy")+")";
			break;
		case "Integer":
			return ""+operand;
			break;
		case "Double":
			return ""+operand;
			break;
		default:
			return "'"+operand.replace("'","\\'")+"'"
			break;
		}
	};

	this.filterElemToCayennePredicate = function(filterElem) {
		var type = filterElem.type;
		var filterOperator = _.find(this.filterOperatorsForType[type], function(fo) {return fo.operator == filterElem.predicate.operator});
		var cayennePredicate = "";
		var propertyOperator = filterElem.property + " " + filterOperator.cayenneOperator;
		if(filterOperator.hasOperand) {
			cayennePredicate = cayennePredicate + "(";
			for(var i = 0; i < filterElem.predicate.operand.length; i++) {
				if(i > 0) {
					cayennePredicate = cayennePredicate + " or ";
				}
				var op = filterElem.predicate.operand[i];
				if(filterOperator.preTransformOperand != null) {
					op = filterOperator.preTransformOperand(op);
				}
				cayennePredicate = cayennePredicate + "(" + propertyOperator + " " + this.transformOperand(type, op) + ")";
			}
			cayennePredicate = cayennePredicate + ")";
		} else {
			cayennePredicate = cayennePredicate + propertyOperator;
		}
		if(filterOperator.negate == true) {
			cayennePredicate = "not("+cayennePredicate+")";
		}
		return cayennePredicate;
	}
}]);