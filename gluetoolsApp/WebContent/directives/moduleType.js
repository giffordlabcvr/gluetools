gluetoolsApp.directive('moduleType', function() {
	  return {
		    template: function(elem, attr) {
		      var url = "#/moduleReference/moduleType/" + attr.name;
		      return '<a target="_blank" href="'+
		      	url+'">'+
		      	attr.name+'</a>';
		    }
		  };
	});
