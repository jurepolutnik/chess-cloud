/**
 * HomeController
 */

define(function() {
	'use strict';

	function HomeController($scope, $route, KeyboardService) {
		$scope.$route = $route;

	}
	
	HomeController.$inject = [];

	return HomeController;
});