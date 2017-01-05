projectBrowser.directive('integerInput', function() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attr, ngModel) {
 			function fromUser(text) {
 				console.log("converting from string to int", text)
                return parseInt(text, 10);
            }
            function toUser(userInt) {
 				console.log("converting from int to string", userInt)
                return ''+userInt;
            }
            ngModel.$parsers.push(fromUser);
            ngModel.$formatters.push(toUser);        }
    };
});