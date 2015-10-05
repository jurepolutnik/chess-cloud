/**
 * NavigationController
 */

define(function() {
	'use strict';

	function StockfishController($scope, $http, ChessService) {
		console.log("StockfishController called");

		$scope.name = "Stockfish 4.0.1";
		$scope.result = "";
		$scope.fen = "asdf";

		var game = ChessService.parseFEN("rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2");


		$scope.analyse = function() {
			$scope.result = "";
			return $http.get('/analyse?fen='+$scope.fen).success(function(data) {
				$scope.result = data;
				return data;
			});
		};

	}

	StockfishController.$inject = [];

	return StockfishController;
});