
define([], function() {
	'use strict';

function AnalyseWidgetController($scope, $location, ChessService, AnalyseService) {

    // Pass chess service to view
    $scope.chessService = ChessService;

    $scope.liveAnalyse = false;
    $scope.inProgress = false;
    $scope.lastHalfMove = -1;

    //////////////////////////////////////////////////
    // AnalyseService Callback definition
    ////
    var callback = {};
    callback.onResult = function(results, paths)
    {
        $scope.paths = paths;
        $scope.results = results;
    };

    callback.onDone = function()
    {
        $scope.inProgress = false;
        if ($scope.liveAnalyse && $scope.game.currentHalfMove != $scope.lastHalfMove)
        {
            $scope.analyse();
        }
    };

    callback.onError = function(error)
    {
        $scope.liveAnalyse = false;
        $scope.inProgress = false;
        alert ("An error occurred. Try again...");
    };

    /////////////////////////////////////////////////////////
    // Methods
    ////

    $scope.analyse = function ()
    {
        AnalyseService.requestAnalyse($scope.game, "20", callback);
        $scope.inProgress = true;
        $scope.lastHalfMove = $scope.game.currentHalfMove;
    };

		$scope.playPosition = function ()
		{
			$location.path('/play').search('fen='+$scope.fen);
		};

    $scope.toggleLiveAnalyse = function()
    {
        $scope.liveAnalyse = !$scope.liveAnalyse;
        if ($scope.liveAnalyse && !$scope.inProgress)
        {
            $scope.analyse();
        }
    };

    /////////////////////////////////////////////////
    // Auto repeat analyse handle
    ////
    var handle = null;
    // Do this before it runs, and it'll never run

    $scope.$watch('game', function() {
        console.log ("Game changed : Request analyse");
        if (!$scope.inProgress && $scope.liveAnalyse)
        {
            $scope.analyse();
        }
    }, true);
}

AnalyseWidgetController.$inject = [];
return AnalyseWidgetController;
});
