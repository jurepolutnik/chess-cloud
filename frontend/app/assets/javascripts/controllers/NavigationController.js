/**
 * NavigationController
 */

define(function() {
	'use strict';

	function NavigationController($scope, $location) {
		console.log("NavigationController called");

		$scope.navigationItems = [
		{
			'route' : '/home',
			'text' : 'Home',
			'icon' : 'fa-home'
		},
		{
			'route' : '/play',
			'text' : 'Play',
			'icon' : 'fa-laptop'
		},
		{
			'route' : '/analyse',
			'text' : 'Analyse',
			'icon' : 'fa-cogs'
		},
		{
			'route' : '/games',
			'text' : 'Games',
			'icon' : 'fa-table'
		}
		];

		$scope.isActiveRoute = function(route) {
			return route === $location.path();
		};
	}

	NavigationController.$inject = [];

	return NavigationController;
});
