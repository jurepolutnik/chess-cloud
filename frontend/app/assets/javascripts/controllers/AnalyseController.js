define(function() {
	'use strict';

function AnalyseController($scope, $location, ChessService) {

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
AnalyseController.$inject = [];
return AnalyseController;
});
