projectBrowser.service("pagingContext", ['dialogs', 'glueWebToolConfig', 'filterUtils', function(dialogs, glueWebToolConfig, filterUtils) {
	
	this.createPagingContext = function(updateCount, updatePage) {
		var pagingContext = {};
		pagingContext.firstItemIndex = null;
		pagingContext.lastItemIndex = null;
		pagingContext.totalItems = null;
		pagingContext.itemsPerPage = 10;
		pagingContext.sortOrder = [];
		pagingContext.defaultSortOrder = null;
		pagingContext.updatePage = updatePage;
		pagingContext.updateCount = updateCount;
		pagingContext.availableItemsPerPage = [10,25,50,100,250,500];

		pagingContext.setItemsPerPage = function(newItemsPerPage) {
			pagingContext.itemsPerPage = newItemsPerPage;
			pagingContext.firstPage();
		}

		pagingContext.setTotalItems = function(totalItems) {
			pagingContext.totalItems = totalItems;
		}

		pagingContext.countChanged = function() {
			pagingContext.updateCount(pagingContext);
		}
		
		pagingContext.firstPage = function() {
			pagingContext.firstItemIndex = 1;
			pagingContext.lastItemIndex = Math.min(pagingContext.totalItems, pagingContext.itemsPerPage);
			pagingContext.updatePage(pagingContext);
		}
	
		pagingContext.prevPage = function() {
			pagingContext.firstItemIndex = Math.max(pagingContext.firstItemIndex - pagingContext.itemsPerPage, 1);
			pagingContext.lastItemIndex = Math.min(pagingContext.totalItems, pagingContext.firstItemIndex + (pagingContext.itemsPerPage-1));
			pagingContext.updatePage(pagingContext);
		}
	
		pagingContext.nextPage = function() {
			pagingContext.firstItemIndex = Math.min(pagingContext.firstItemIndex + pagingContext.itemsPerPage, pagingContext.totalItems);
			pagingContext.lastItemIndex = Math.min(pagingContext.totalItems, pagingContext.firstItemIndex + (pagingContext.itemsPerPage-1));
			pagingContext.updatePage(pagingContext);
		}
	
		pagingContext.lastPage = function() {
			pagingContext.firstItemIndex = 1 + ( Math.floor( (pagingContext.totalItems-1) / pagingContext.itemsPerPage) * pagingContext.itemsPerPage );  
			pagingContext.lastItemIndex = pagingContext.totalItems;
			pagingContext.updatePage(pagingContext);
		}

		/*
		 * Example:
		 * pagingContext.setSortableProperties([
		 * 		{ property: "id", displayName: "Sequence ID" },
		 * 		{ property: "collection_date", displayName: "Collection Date" }
		 * ])
		 */
		pagingContext.setSortableProperties = function(sortableProperties) {
			pagingContext.sortableProperties = sortableProperties;
		}

		pagingContext.getSortableProperties = function() {
			return pagingContext.sortableProperties;
		}

		/*
		 * Example:
		 * pagingContext.setFilterProperties([
		 * 		{ property: "id", displayName: "Sequence ID", filterHints: {type: "String", inputType="text"} },
		 * 		{ property: "gb_length", displayName: "Sequence Length", filterHints: {type: "Integer", inputType="number"} },
		 * 		{ property: "gb_create_date", displayName: "Creation Date", filterHints: {type: "Date", inputType="date"} }
		 * ])
		 */
		pagingContext.setFilterProperties = function(filterProperties) {
			pagingContext.filterProperties = filterProperties;
		}

		pagingContext.getFilterProperties = function() {
			return pagingContext.filterProperties;
		}

		
		
		/* Example:
		 * pagingContxt.setSortOrder([
		 * 		{ property: "id", displayName: "Sequence ID", order: "+" },
		 * 		{ property: "collection_date", displayName: "Collection Date", order: "-"}
		 * ])
		 * 
		 */
		pagingContext.setSortOrder = function(sortOrder) {
			pagingContext.sortOrder = sortOrder;
		} 

		pagingContext.setDefaultSortOrder = function(defaultSortOrder) {
			pagingContext.defaultSortOrder = defaultSortOrder;
			pagingContext.sortOrder = defaultSortOrder;
		} 

		pagingContext.getSortOrder = function() {
			return pagingContext.sortOrder;
		}

		pagingContext.getDefaultSortOrder = function() {
			return pagingContext.defaultSortOrder;
		}

		
		/* Example:
		 * pagingContxt.setFilterElems([
		 * 		{ property: "id", type: "String", predicate: { operator: "like", operand: "KJ%"} },
		 * 		{ property: "gb_length", type: "Integer", predicate: { operator: "gte", operand: "1000"} }
		 * ]);
		 * 
		 */
		pagingContext.setFilterElems = function(filterElems) {
			pagingContext.filterElems = filterElems;
		} 

		pagingContext.setDefaultFilterElems = function(defaultFilterElems) {
			pagingContext.defaultFilterElems = defaultFilterElems;
			pagingContext.filterElems = defaultFilterElems;
		} 

		pagingContext.getFilterElems = function() {
			return pagingContext.filterElems;
		}

		pagingContext.getDefaultFilterElems = function() {
			return pagingContext.defaultFilterElems;
		}
		
		
		pagingContext.sortOrderDialog = function() {
			var newSortOrder = _(pagingContext.sortOrder).clone();
			var dlg = dialogs.create(
					glueWebToolConfig.getProjectBrowserURL()+'/dialogs/selectSortOrder.html','selectSortOrderCtrl',
					{ newSortOrder: newSortOrder,
					  defaultSortOrder: pagingContext.defaultSortOrder,
				      sortableProperties: pagingContext.sortableProperties }, {});
			dlg.result.then(function(data){
				if(pagingContext.getGlueSortOrderAux(pagingContext.sortOrder) != pagingContext.getGlueSortOrderAux(data.newSortOrder)) {
					pagingContext.sortOrder = data.newSortOrder;
					console.log("pagingContext.sortOrder updated", pagingContext.sortOrder);
					pagingContext.firstPage();
				}
			});
		}

		
		pagingContext.filterDialog = function() {
			var newFilterElems = _(pagingContext.filterElems).clone();
			var dlg = dialogs.create(
					glueWebToolConfig.getProjectBrowserURL()+'/dialogs/selectFilter.html','selectFilterCtrl',
					{ newFilterElems: newFilterElems,
					  defaultFilterElems: pagingContext.defaultFilterElems,
				      filterProperties: pagingContext.filterProperties }, {});
			dlg.result.then(function(data){
				var updatedWhereClause = pagingContext.getGlueWhereClauseAux(data.newFilterElems);
				if(pagingContext.getGlueWhereClauseAux(pagingContext.filterElems) != updatedWhereClause) {
					pagingContext.filterElems = data.newFilterElems;
					console.log("pagingContext.filterElems updated", pagingContext.filterElems);
					console.log("updatedWhereClause", updatedWhereClause);
					pagingContext.countChanged();
				}
			});
		}

		
		pagingContext.extendListCmdParams = function(cmdParams) {
			cmdParams.pageSize = pagingContext.itemsPerPage;
			cmdParams.fetchLimit = pagingContext.itemsPerPage;
			cmdParams.fetchOffset = pagingContext.firstItemIndex - 1;
			var glueSortOrder = pagingContext.getGlueSortOrder();
			if(glueSortOrder != null) {
				cmdParams.sortProperties = glueSortOrder;
			}
			pagingContext.extendCmdParamsWhereClause(cmdParams);
		}

		pagingContext.extendCountCmdParams = function(cmdParams) {
			pagingContext.extendCmdParamsWhereClause(cmdParams);
		}
		
		pagingContext.extendCmdParamsWhereClause = function(cmdParams) {
			var glueWhereClause = pagingContext.getGlueWhereClause();
			if(glueWhereClause != null) {
				if(cmdParams.whereClause != null) {
					cmdParams.whereClause = cmdParams.whereClause + " and " + glueWhereClause;
				} else {
					cmdParams.whereClause = glueWhereClause;
				}
			}
		}

		pagingContext.getGlueSortOrder = function() {
			return pagingContext.getGlueSortOrderAux(pagingContext.sortOrder);
		}
		
		pagingContext.getGlueSortOrderAux = function(sortOrder) {
			if(sortOrder.length == 0) {
				return null;
			}
			var glueSortOrder = "";
			for(var i = 0; i < sortOrder.length; i++) {
				if(i > 0) {
					glueSortOrder = glueSortOrder+",";
				}
				glueSortOrder = glueSortOrder + sortOrder[i].order + sortOrder[i].property;
			}
			return glueSortOrder;
		}

		
		pagingContext.getGlueWhereClause = function() {
			return pagingContext.getGlueWhereClauseAux(pagingContext.filterElems);
		}
		
		pagingContext.getGlueWhereClauseAux = function(filterElems) {
			if(filterElems == null || filterElems.length == 0) {
				return null;
			}
			var typeToFilterOperators = filterUtils.filterOperatorsForType();
			var whereClause = "";
			for(var i = 0; i < filterElems.length; i++) {
				if(i > 0) {
					whereClause = whereClause + " and"
				}
				var filterElem = filterElems[i];
				var type = filterElem.type;
				var filterOperator = _.find(typeToFilterOperators[type], function(fo) {return fo.operator == filterElem.predicate.operator});
				whereClause = whereClause + " "+ filterElem.property + " " + filterOperator.cayenneOperator;
				if(filterOperator.hasOperand) {
					whereClause = whereClause + " " + filterUtils.transformOperand(type, filterElem.predicate.operand);
				}
				
			}

			return whereClause;
		}

		return pagingContext;
	};
}]);