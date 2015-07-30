/*
	@license Angular Treeview version 0.1.6
	ⓒ 2013 AHN JAE-HA http://github.com/eu81273/angular.treeview
	License: MIT


	[TREE attribute]
	angular-treeview: the treeview directive
	tree-id : each tree's unique id.
	tree-model : the tree model on $scope.
	node-id : each node's id
	node-label : each node's label
	node-children: each node's children

	<div
		data-angular-treeview="true"
		data-tree-id="tree"
		data-tree-model="roleList"
		data-node-id="roleId"
		data-default-selected-id="HCV"
		data-default-open-depth="1"
		data-node-label="roleName"
		data-node-children="children" >
	</div>
*/

(function ( angular ) {
	'use strict';

	angular.module( 'angularTreeview', [] ).directive( 'treeModel', ['$compile', function( $compile ) {
		return {
			restrict: 'A',
			link: function ( scope, element, attrs ) {
				//tree id
				var treeId = attrs.treeId;
			
				//tree model
				var treeModel = attrs.treeModel;

				//node id
				var nodeId = attrs.nodeId || 'id';

				//node label
				var nodeLabel = attrs.nodeLabel || 'label';

				//children
				var nodeChildren = attrs.nodeChildren || 'children';

				//children
				var defaultSelectedId = attrs.defaultSelectedId || '';

				//children
				var defaultOpenDepth = attrs.defaultOpenDepth || '-1';

				//tree template
				var template =
					'<ul data-ng-init="' + treeId + '.init(node, '+defaultOpenDepth+')">' +
						'<li data-ng-repeat="node in ' + treeModel + '">' +
							'<i class="collapsed" data-ng-show="node.' + nodeChildren + '.length && node.collapsed" data-ng-click="' + treeId + '.selectNodeHead(node)"><span class="glyphicon glyphicon-plus-sign"/></i>' +
							'<i class="expanded" data-ng-show="node.' + nodeChildren + '.length && !node.collapsed" data-ng-click="' + treeId + '.selectNodeHead(node)"><span class="glyphicon glyphicon-minus-sign"/></i>' +
							'<i class="normal" data-ng-hide="node.' + nodeChildren + '.length"><span class="glyphicon glyphicon-record"/></i>'+
							'<span data-ng-class="node.selected" data-ng-click="' + treeId + '.selectNodeLabel(node)">{{ node.' + nodeLabel + ' }}</span>' +
							'<div data-ng-hide="node.collapsed" data-default-open-depth="'+(defaultOpenDepth-1)+'" data-tree-id="' + treeId + '" data-tree-model="node.' + nodeChildren + '" data-node-id=' + nodeId + ' data-node-label=' + nodeLabel + ' data-node-children=' + nodeChildren + '></div>' +
						'</li>' +
					'</ul>';


				//check tree id, tree model
				if( treeId && treeModel ) {

					//root node
					if( attrs.angularTreeview ) {
					
						//create tree object if not exists
						scope[treeId] = scope[treeId] || {};

						//if node head clicks,
						scope[treeId].selectNodeHead = scope[treeId].selectNodeHead || function( selectedNode ){

							//Collapse or Expand
							selectedNode.collapsed = !selectedNode.collapsed;
						};
						
						//if node label clicks,
						scope[treeId].selectNodeLabel = scope[treeId].selectNodeLabel || function( selectedNode ){

							//remove highlight from previous node
							if( scope[treeId].currentNode && scope[treeId].currentNode.selected ) {
								scope[treeId].currentNode.selected = undefined;
							}

							//set highlight to selected node
							selectedNode.selected = 'selected';

							//set currentNode
							scope[treeId].currentNode = selectedNode;
						};
						
						scope[treeId].init = scope[treeId].init || function(node, defaultOpenDepth){
							if(node) {
								 if(node[nodeId] == defaultSelectedId) {
									 scope[treeId].selectNodeLabel(node);
								 }
								 if(defaultOpenDepth < 0) {
									 node.collapsed = true;
								 } else {
									 node.collapsed = false;
								 }
							}
						};
						
					}

					//Rendering template.
					element.html('').append( $compile( template )( scope ) );
				}
			}
		};
	}]);
})( angular );
