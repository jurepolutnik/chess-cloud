/**
 * AboutController
 */

define(function() {
	'use strict';

	function AboutController($scope, $route) {
		$scope.$route = $route;
		console.log("AboutController called");
		$scope.tra = "trata";
	}

	AboutController.$inject = [];

	return AboutController;
});