gluetoolsApp.directive('modeCommand', function() {
  return {
    template: function(elem, attr) {
      var url = "#/commandModes/commandMode/" + attr.mode + "/command/" + attr.command;
      var commandWords = attr.command.replace("_", " ");
      if(attr.args != null) {
    	  commandWords = commandWords + " " + attr.args;
      }
      return '<a target="_blank" style="font-family:monospace;" href="'+
      	url+'">'+
      	commandWords+'</a>';
    }
  };
});

gluetoolsApp.directive('nonModeCommand', function() {
	  return {
	    template: function(elem, attr) {
	      var url = "#/nonModeCommands/command/" + attr.command;
	      var commandWords = attr.command.replace("_", " ");
	      if(attr.args != null) {
	    	  commandWords = commandWords + " " + attr.args;
	      }
	      return '<a target="_blank" style="font-family:monospace;" href="'+
	      	url+'">'+
	      	commandWords+'</a>';
	    }
	  };
	});


gluetoolsApp.directive('moduleCommand', function() {
	  return {
		    template: function(elem, attr) {
		      var url = "#/moduleReference/moduleType/" + attr.module + "/command/" + attr.command;
		      var commandWords = attr.command.replace("_", " ");
		      if(attr.args != null) {
		    	  commandWords = commandWords + " " + attr.args;
		      }
		      return '<a target="_blank" style="font-family:monospace;" href="'+
		      	url+'">'+
		      	commandWords+'</a>';
		    }
		  };
	});
