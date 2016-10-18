projectBrowser.service("pagingContext", function() {
	
	this.createPagingContext = function(totalItems, itemsPerPage, updatePage) {
		var pagingContext = {};
		pagingContext.firstItemIndex = null;
		pagingContext.lastItemIndex = null;
		pagingContext.totalItems = totalItems;
		pagingContext.itemsPerPage = itemsPerPage;
		pagingContext.updatePage = updatePage;
		pagingContext.availableItemsPerPage = [10,50,100,500];

		pagingContext.setItemsPerPage = function(newItemsPerPage) {
			pagingContext.itemsPerPage = newItemsPerPage;
			pagingContext.firstPage();
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
	
		pagingContext.firstPage();
		
		return pagingContext;
	}
});