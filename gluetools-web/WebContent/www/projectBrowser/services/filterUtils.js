projectBrowser.service("filterUtils", ['$filter', function($filter) {
	
	this.filterOperatorsForType = { 
			"String": [
			           {operator:"contains", displayName:"contains", hasOperand:true, cayenneOperator:"like",
			        	   preTransformOperand: function(op) { return "%"+op+"%"} }, 
			           {operator:"notcontains", displayName:"does not contain", hasOperand:true, cayenneOperator:"like", negate:true, 
			        	   preTransformOperand: function(op) { return "%"+op+"%"}}, 
			           {operator:"equals", displayName:"matches", hasOperand:true, cayenneOperator:"="}, 
			           {operator:"notequals", displayName:"does not match", hasOperand:true, cayenneOperator:"!="}, 
			           {operator:"like", displayName:"is like", hasOperand:true, cayenneOperator:"like"}, 
			           {operator:"notlike", displayName:"is not like", hasOperand:true, cayenneOperator:"like", negate:true}, 
			           {operator:"isnull", displayName:"is null", hasOperand:false, cayenneOperator:"= null"},
			           {operator:"isnotnull", displayName:"is not null", hasOperand:false, cayenneOperator:"!= null"}
			],
			"Integer" : [
				           {operator:"equals", displayName:"equals", hasOperand:true, cayenneOperator:"="}, 
				           {operator:"notequals", displayName:"does not equal", hasOperand:true, cayenneOperator:"!="}, 
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
		var cayennePredicate = filterElem.property + " " + filterOperator.cayenneOperator;
		if(filterOperator.hasOperand) {
			var op = filterElem.predicate.operand;
			if(filterOperator.preTransformOperand != null) {
				op = filterOperator.preTransformOperand(op);
			}
			cayennePredicate = cayennePredicate + " " + this.transformOperand(type, op);
		}
		if(filterOperator.negate == true) {
			cayennePredicate = "not("+cayennePredicate+")";
		}
		return cayennePredicate;
	}
}]);