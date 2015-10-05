define([ 'require', 'angular', 'angular.route','angular.resource', 'ui.bootstrap', 'app' ], function(require, angular) {

	'use strict';

	return require([ 'domReady!' ], function(document) {
		return angular.bootstrap(document, [ 'myApp' ]);
	});
});