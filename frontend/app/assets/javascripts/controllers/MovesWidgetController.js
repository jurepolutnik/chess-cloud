define(function() {
	'use strict';

function MovesWidgetController($scope, ChessService) {

	$scope.chessService = ChessService;
}

MovesWidgetController.$inject = [];
return MovesWidgetController;
});

