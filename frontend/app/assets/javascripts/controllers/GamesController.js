define(function() {
	'use strict';

function GamesController($scope, $location, GamesRetriever, ChessService) {

	$scope.games = GamesRetriever.query();

	$scope.createGame = function (pgn)
	{
		try
		{
			var game = ChessService.createGamePGN (pgn);
			ChessService.setCurrentGame (game);
			$scope.$apply( $location.path("/game/pgn") );
		}
		catch (err)
		{
			$scope.alerts = [
				{ type: 'danger', msg: 'Validation of PGN failed! Please insert valid PGN format.' }
			];
		}

	};

	$scope.closeAlert = function(index) {
		$scope.alerts.splice(index, 1);
	};
}
GamesController.$inject = [];
return GamesController;
});
