define([ 'angular' ], function(angular) {

	var filters = angular.module('myApp.filters', []).filter('interpolate',
			[ 'version', function(version) {
				return function(text) {
					return String(text).replace(/\%VERSION\%/mg, version);
				};
			} ]);

	return filters;
});