
define(['angular', 'services/services', 'directives/directives',
		'providers/providers', 'filters/filters', 'controllers/controllers' ],
		function(angular) {
			'use strict';

			return angular.module(
					'myApp',
					[ 'ui.bootstrap','angulartics', 'angulartics.google.analytics','ngRoute','ngResource',
						'myApp.filters', 'myApp.services', 'myApp.directives','myApp.controllers'])
						.config([ '$routeProvider', function($routeProvider) {
						$routeProvider.when('/home', {
							templateUrl : 'partials/home.html',
							controller : 'HomeController'
						});
						$routeProvider.when('/analyse', {
							templateUrl : 'partials/analyseview.html',
							controller : 'AnalyseController'
						});
						$routeProvider.when('/games', {
							templateUrl : 'partials/games.html',
							controller : 'GamesController'
						});
						$routeProvider.when('/game/:gameId', {
							templateUrl : 'partials/gameview.html',
							controller : 'GameController'
						});
						$routeProvider.when('/play', {
							templateUrl : 'partials/playview.html',
							controller : 'PlayController'
						});

						$routeProvider.otherwise({
							redirectTo : '/home'
						});
					} ]);

		});
