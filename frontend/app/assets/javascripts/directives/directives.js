define([ 'angular' ], function(angular) {

	var directives = angular.module('myApp.directives', []);
	directives.directive( 'appversion', [ 'version', function(version) {
				return function(scope, elm, attrs) {
					elm.text("1.0");
				};
			} ]);

	directives.directive('keytrap', function() {
		return function( scope, elem ) {
			elem.bind('keydown', function( event ) {
				scope.$broadcast('keydown', event.keyCode );
			});
		};
	});
	directives.directive('score', function() {
		return function(scope, elem, attrs) {
			elem.addClass('plus');
		};
	});

	return directives;
});