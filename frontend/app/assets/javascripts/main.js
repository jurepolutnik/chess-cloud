require.config({
	paths : {
		domReady : '/assets/lib/require/domReady',
		angular : '/assets/lib/angular/angular.min',
		'angular.route' : '/assets/lib/angular/angular-route.min',
		'angular.resource' : '/assets/lib/angular/angular-resource.min',
		'ui.bootstrap' : '/assets/lib/ui-bootstrap/ui-bootstrap',
		'chessjs' : '/assets/lib/chessjs/chess',
		'chessboardjs' : '/assets/lib/chessboardjs/js/chessboard'
	},
	shim : {
		'ui.bootstrap' : ['angular'],
		'angular.route' : ['angular'],
		'angular.resource' : ['angular'],
		'angular' : {'exports' : 'angular'}
	},
	priority : [ 'angular' ],
	urlArgs : 'v=0.1'
});

require(['app', 'services/services',
		'directives/directives', 'providers/providers', 'filters/filters',
		'controllers/controllers', 'bootstrap'], function(app) {

	app.run();
});
