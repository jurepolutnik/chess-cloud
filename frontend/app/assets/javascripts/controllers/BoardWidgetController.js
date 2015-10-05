define(['chessboardjs', 'chessjs'], function() {
	'use strict';

function BoardWidgetController($scope, $timeout, KeyboardService, ChessService) {


	// Pass chess service to view
	$scope.chessService = ChessService;
	$scope.controlsVisible = false;

	KeyboardService.bind('left', function() {
		ChessService.goBack();
	});
	KeyboardService.bind('right', function() {
		ChessService.goNext();
	});

	$scope.$on('$destroy', function() {
		KeyboardService.unbind ('left');
		KeyboardService.unbind ('right');
	});

    // MovesList  and Board resizing ...
    var resize = function ()
    {
        var h = $("#sectionBoard").height();
        $("#sectionMoves").height(h);
        $("#MovesTable").height(h-40);
    };

    $(window).resize(ChessService.getCurrentBoard().resize);
    $(window).resize (resize);
    $timeout (resize, 100, false);
}


BoardWidgetController.$inject = [];
return BoardWidgetController;
});
