define([ 'angular', 'services/ChessService', 'services/KeyboardService', 'services/AnalyseService' ],
    function(angular, ChessService, KeyboardService, AnalyseService) {
        var services = angular.module('myApp.services', ['ngResource']).value( 'version', '0.1');


        services.service('ChessService', ChessService);
        services.service('AnalyseService', ['$rootScope', 'ChessService', AnalyseService]);
        services.service('KeyboardService', ['$window', '$timeout', KeyboardService]);
        services.factory ('GamesRetriever', function($resource){ return $resource('/games/:gameId'); });
        return services;
});