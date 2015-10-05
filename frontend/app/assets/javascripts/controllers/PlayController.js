define(['chessjs', 'chessboardjs'], function() {
	'use strict';

function PlayController($scope, $location, $modal, ChessService, AnalyseService) {


  $scope.game = new Chess();
  $scope.game.currentHalfMove = -1;
  $scope.game.fullMoves = [];
  $scope.playerColor = 'w';
  ChessService.setCurrentGame($scope.game);

  $scope.bestmove = null;
  $scope.inProgress = false;

  //////////////////////////////////////////
  // Analysis Callback
  ////
  var callback = {};

  callback.onBestmove = function(move)
  {
    playmove (move);

		// Find played path
		for (var i = 0; i < $scope.paths.length; i++) {
			var path = $scope.paths[i];

			if ($scope.game.halfMoves[$scope.game.halfMoves.length -1] === path.halfMoves[0])
			{
				$scope.path = path;
			}
		}
  };

  callback.onResult = function(results, paths)
  {
    $scope.results = results;
    $scope.paths = paths;
  };

  callback.onError = function(error)
  {
      alert ("An error occurred. Sorry...");
      $scope.inProgress = false;
  };


  var playmove = function (move)
  {
    $scope.game.move(move);
    $scope.board.position($scope.game.fen());
    update();
  };

  var analyse = function() {
			if($scope.game.in_checkmate() || $scope.game.in_draw())
			{
				return;
			}

      var uciStrength = Math.round(20*($scope.strength-1)/7);
      AnalyseService.requestAnalyse($scope.game, uciStrength, callback);
      $scope.inProgress = true;
  };

  // do not pick up pieces if the game is over
  // only pick up pieces for the side to move
  var onDragStart = function(source, piece, position, orientation) {
    if ($scope.game.game_over() === true || $scope.game.turn() !== $scope.playerColor ||
        ($scope.game.turn() === 'w' && piece.search(/^b/) !== -1) ||
        ($scope.game.turn() === 'b' && piece.search(/^w/) !== -1))
    {
      return false;
    }
  };

  var onDrop = function(source, target) {
    // see if the move is legal
    var move = $scope.game.move({
      from: source,
      to: target,
      promotion: 'q' // NOTE: always promote to a queen for example simplicity
    });

    // illegal move
    if (move === null) return 'snapback';
    update();

    setTimeout(analyse, 100);
  };

  // update the board position after the piece snap
  // for castling, en passant, pawn promotion
  var onSnapEnd = function() {
    $scope.board.position($scope.game.fen());
  };

  var update = function ()
  {
    $scope.game.currentHalfMove++;
    $scope.game.history_all = $scope.game.history({verbose: true});
    $scope.game.halfMoves = $scope.game.history();
    $scope.game.fullMoves.length = 0;

    for (var i=0; i<$scope.game.halfMoves.length; i+=2)
    {
      $scope.game.fullMoves.push ({white: $scope.game.halfMoves[i], black: $scope.game.halfMoves[i+1]});
    }
    updateStatus();
    $scope.$apply();
  };

  var updateStatus = function() {
    $scope.status = '';

    var moveColor = 'White';
    var opositeColor = 'Black';
    if ($scope.game.turn() === 'b') {
			var temp = opositeColor;
      opositeColor = moveColor;
			moveColor = temp;
    }

    // checkmate?
    if ($scope.game.in_checkmate() === true) {
      $scope.status = opositeColor + ' wins!  ' + moveColor + ' is in checkmate.';
      setTimeout($scope.endgame, 1000);
    }

    // draw?
    else if ($scope.game.in_draw() === true) {
      $scope.status = 'Game is drawn.';
      setTimeout($scope.endgame, 1000);
    }

    // game still on
    else {
      $scope.status = moveColor + ' to move';

      // check?
      if ($scope.game.in_check() === true) {
        $scope.status += ' (check)';
      }
    }
  };

  var cfg = {
    draggable: true,
    moveSpeed: 100,
    position: 'start',
    pieceTheme: 'assets/lib/chessboardjs/img/chesspieces/wikipedia/{piece}.png',
    onDragStart: onDragStart,
    onDrop: onDrop,
    onSnapEnd: onSnapEnd
  };

  $scope.board = new ChessBoard2('board', cfg);
  ChessService.setCurrentBoard($scope.board);


  $scope.createNewGame = function (color, strength, fen )
  {
		if (fen)
		{
			$scope.game = new Chess(fen);
			$scope.game.startPosition = fen;
		}
		else{
			$scope.game = new Chess();
		}

    $scope.game.currentHalfMove = -1;
    $scope.game.fullMoves = [];

    $scope.playerColor = color;
    $scope.strength = strength;


    ChessService.setCurrentGame($scope.game);

    $scope.board.position($scope.game.fen());
		updateStatus();

    if ($scope.playerColor == "b")
    {
      $scope.board.orientation('black');
    }
    else
    {
      $scope.board.orientation('white');
    }

		if ($scope.game.turn() !== $scope.playerColor)
		{
      setTimeout(analyse, 100);
		}
  };
  //////////////////////////////////////////////////////////////
  // Popup
  ////

	$scope.endgame = function() {
    var modalInstance = $modal.open({
      templateUrl: 'assets/html/modals/modal-endgame.html',
			scope: $scope,
			controller: function($scope, $modalInstance){
				$scope.close = function(){
					$modalInstance.dismiss();
				};
			}
    });
	};

  $scope.newgame = function () {

		var fen = $location.search().fen;
    var modalInstance = $modal.open({
      templateUrl: 'assets/html/modals/modal-newgame.html',
      controller: ModalInstanceCtrl,
			resolve: {
				fen: function(){return fen;}
			}
    });


    modalInstance.result.then(function (res) {

      console.log('Play: ' + res.color + ' at '+res.strength);
      $scope.createNewGame(res.color, res.strength, res.fen);
    }, function () {
      console.log('Modal dismissed at: ' + new Date());
    });
  };

  $scope.newgame();
}

var ModalInstanceCtrl = function ($scope, $modalInstance, ChessService, fen) {
  $scope.computer = {
    color: 'w',
    strength: '4',
		fen: fen
  };
  $scope.getStrenghts = function ()
  {
    return new Array(10);
  };

  $scope.play = function () {
      $modalInstance.close($scope.computer);
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
};


PlayController.$inject = [];
return PlayController;
});
