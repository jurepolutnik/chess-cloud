require.config({
	paths : {
		domReady : '/assets/lib/require/domReady',
		angular : '/assets/lib/angular/angular.min',
		'angular.route' : '/assets/lib/angular/angular-route.min',
		'angular.resource' : '/assets/lib/angular/angular-resource.min',
		'angulartics' : '/assets/lib/angulartics/angulartics.min',
		'angulartics-google-analytics' : '/assets/lib/angulartics/angulartics-google-analytics.min',
		'ui.bootstrap' : '/assets/lib/ui-bootstrap/ui-bootstrap',
		'chessjs' : '/assets/lib/chessjs/chess',
		'chessboardjs' : '/assets/lib/chessboardjs/js/chessboard'
	},
	shim : {
		'ui.bootstrap' : ['angular'],
		'angular.route' : ['angular'],
		'angular.resource' : ['angular'],
		'angular' : {'exports' : 'angular'},
		'angulartics' : ['angular'],
		'angulartics-google-analytics' : ['angulartics']
	},
	priority : [ 'angular' ],
	urlArgs : 'v=0.1'
});

require(['app', 'bootstrap'], function(app) {

	app.run();
});
