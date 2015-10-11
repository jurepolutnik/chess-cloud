define([ 'require', 'angular', 'angular.route','angular.resource','angulartics','angulartics-google-analytics', 'ui.bootstrap', 'app' ], function(require, angular) {

	'use strict';

	return require([ 'domReady!' ], function(document) {
		return angular.bootstrap(document, [ 'myApp' ]);
	});
});
