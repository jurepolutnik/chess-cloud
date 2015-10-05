define(function() {
	'use strict';

function GlobalController($scope, $rootScope, $location, KeyboardService) {

		console.log("GlobalController called");

		KeyboardService.bind('ctrl+h', function() {
			console.log("Paste");
			$rootScope.$apply(function() {

					$location.path("/home");
					console.log($location.path());
			});
		});
}


GlobalController.$inject = [];
return GlobalController;

});
