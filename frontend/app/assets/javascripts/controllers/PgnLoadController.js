define(function() {
	'use strict';

function PgnLoadController($scope, $modal, $log, $location, ChessService) {

		$scope.items = ['item1', 'item2', 'item3'];
		$scope.service = ChessService;


	$scope.open = function () {

		var modalInstance = $modal.open({
			templateUrl: 'partials/modal-loadpgn.html',
			controller: ModalInstanceCtrl
		});

		modalInstance.result.then(function (game) {
			$scope.loadGame (game);
		}, function () {
			$log.info('Modal dismissed at: ' + new Date());
		});
	};

		$scope.loadGame = function (game)
		{
					ChessService.setCurrentGame (game);
					$scope.$apply( $location.path("/game/pgn") );
		};
}

var ModalInstanceCtrl = function ($scope, $modalInstance, ChessService) {
		$scope.alerts = [];

	$scope.analyse = function (pgn) {
				try
				{
					var game = ChessService.createGamePGN (pgn);
					$modalInstance.close(game);
				}
				catch (err)
				{
						$scope.alerts = [
						{ type: 'danger', msg: 'Validation of PGN failed! Please insert valid PGN format.' }
					];
				}
	};


	$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
	};

	$scope.closeAlert = function(index) {
		$scope.alerts.splice(index, 1);
	};
};

PgnLoadController.$inject = [];
return PgnLoadController;
});

