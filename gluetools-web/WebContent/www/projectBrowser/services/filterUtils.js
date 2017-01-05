projectBrowser.service("filterUtils", ['$filter', function($filter) {
	
	this.filterOperatorsForType = function() {
		return { 
			"String": [
			           {operator:"equals", displayName:"exactly matches", hasOperand:true, cayenneOperator:"="}, 
			           {operator:"notequals", displayName:"does not match", hasOperand:true, cayenneOperator:"!="}, 
			           {operator:"like", displayName:"is like", hasOperand:true, cayenneOperator:"like"}, 
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
	};

	this.defaultOperandForType = function() {
		return {
			"String": "",
			"Integer" : 0,
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
		default:
			return "'"+operand.replace("'","\\'")+"'"
			break;
		}
	};

	
}]);