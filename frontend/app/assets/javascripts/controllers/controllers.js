/**
 * Base Controller
 */
		//[ 'angular', 'controllers/NavigationController', 'controllers/HomeController', 'controllers/AboutController' ],

define([ 'angular','controllers/NavigationController', 'controllers/HomeController', 'controllers/AboutController',
            'controllers/StockfishController', 'controllers/GameController', 'controllers/MovesWidgetController',
            'controllers/GlobalController', 'controllers/GamesController', 'controllers/BoardWidgetController', 'controllers/AnalyseWidgetController',
            'controllers/PgnLoadController', 'controllers/AnalyseController', 'controllers/PlayController'],

		function(angular, NavigationController, HomeController, AboutController,
                    StockfishController, GameController, MovesWidgetController, GlobalController,
                    GamesController, BoardWidgetController, AnalyseWidgetController,
                    PgnLoadController, AnalyseController, PlayController) {

			var controllers = angular.module('myApp.controllers', [ 'myApp.services' ]);

			controllers.controller('GlobalController',  ['$scope', '$rootScope', '$location', 'KeyboardService', GlobalController]);
			controllers.controller('HomeController',  ['$scope', '$route', 'KeyboardService', HomeController]);
			controllers.controller('NavigationController',  ['$scope', '$location', NavigationController]);
			controllers.controller('AboutController', ['$scope', '$route', AboutController]);
			controllers.controller('StockfishController', ['$scope', '$http','ChessService', StockfishController]);
			controllers.controller('GameController', ['$scope', '$timeout', '$routeParams', 'GamesRetriever', 'ChessService', GameController]);
			controllers.controller('GamesController', ['$scope', '$location', 'GamesRetriever', 'ChessService', GamesController]);
			controllers.controller('BoardWidgetController', ['$scope', '$timeout', 'KeyboardService', 'ChessService', BoardWidgetController]);
			controllers.controller('MovesWidgetController', ['$scope', 'ChessService', MovesWidgetController]);
			controllers.controller('AnalyseWidgetController', ['$scope', '$location','ChessService', 'AnalyseService', AnalyseWidgetController]);
			controllers.controller('PgnLoadController', ['$scope', '$modal', '$log','$location','ChessService', PgnLoadController]);
			controllers.controller('AnalyseController', ['$scope', '$location', 'ChessService', AnalyseController]);
			controllers.controller('PlayController', ['$scope', '$location','$modal', 'ChessService', 'AnalyseService', PlayController]);

			return controllers;
		});
