projectBrowser.service("pagingContext", ['dialogs', 'glueWebToolConfig', function(dialogs, glueWebToolConfig) {
	
	this.createPagingContext = function(updatePage) {
		var pagingContext = {};
		pagingContext.firstItemIndex = null;
		pagingContext.lastItemIndex = null;
		pagingContext.totalItems = null;
		pagingContext.itemsPerPage = 10;
		pagingContext.sortOrder = [];
		pagingContext.defaultSortOrder = null;
		pagingContext.updatePage = updatePage;
		pagingContext.availableItemsPerPage = [10,50,100,500];

		pagingContext.setItemsPerPage = function(newItemsPerPage) {
			pagingContext.itemsPerPage = newItemsPerPage;
			pagingContext.firstPage();
		}

		pagingContext.setTotalItems = function(totalItems) {
			pagingContext.totalItems = totalItems;
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
		
		return pagingContext;
	}
}]);