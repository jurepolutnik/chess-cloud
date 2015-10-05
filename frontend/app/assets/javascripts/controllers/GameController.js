define(['chessjs'], function() {
	'use strict';

function GameController($scope, $timeout, $routeParams, GamesRetriever, ChessService) {
	$scope.gameId = $routeParams.gameId;

  $scope.comments = null;
  $scope.alternatives = null;

  $scope.board = ChessService.createBoard();
  ChessService.setCurrentBoard($scope.board);


  $scope.board.cfg.onChange = function ()
  {
      $scope.fen = ChessService.getCurrentGame().fen();
  };



    $scope.init = function ()
    {
        if ($scope.gameId === 'pgn')
        {
              $scope.game = ChessService.getCurrentGame();
              ChessService.setCurrentGame ($scope.game);
              $scope.info = $scope.game.header();
              ChessService.moveToPath ($scope.game, -1);
        }
        else
        {
          var gameData = GamesRetriever.get({gameId:$scope.gameId}, function () {
              $scope.game = ChessService.createGamePGN (gameData.pgn);
              ChessService.setCurrentGame ($scope.game);
              $scope.info = $scope.game.header();
              ChessService.moveToPath ($scope.game, -1);
          });
        }
    };

    $scope.init();
}

GameController.$inject = [];
return GameController;
});
