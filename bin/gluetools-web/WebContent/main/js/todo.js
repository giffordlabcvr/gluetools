var todoApp = angular.module('todoApp', []);

todoApp.controller('todoCtrl', [ '$scope', function($scope) {
	$scope.todos = [ {
		text : 'Learn Angular',
		done : false
	}, {
		text : 'build an app',
		done : false
	} ];
	
	$scope.getTotalTodos = function() {
		return $scope.todos.length;
	}
	
	$scope.clear = function() {
		$scope.todos = _.filter($scope.todos, function(todo) {
			return !todo.done;
		});
	}
	
	$scope.addTodo = function() {
		$scope.todos.push({text:$scope.formTodoText, done:false});
		$scope.formTodoText = "";
	}
} ]);