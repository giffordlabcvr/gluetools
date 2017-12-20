gluetoolsApp.directive('glueCommand', function() {
  return {
    template: function(elem, attr) {
      var bits = attr.url.split("/");
      var lastPart = bits[bits.length-1];
      var command = lastPart.replace("_", " ");
      if(attr.args != null) {
    	  command = command + " " + attr.args;
      }
      return '<a target="_blank" style="font-family:monospace;" href="'+
      	attr.url+'">'+
      	command+'</a>';
    }
  };
});