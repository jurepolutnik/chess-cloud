define(['angular', 'chessboardjs', 'chessjs'],function() {
	'use strict';

function ChessService(){

  var service = {};

  var currentGame;
  var currentBoard;

  //////////////////////
  // Geters and setters
  ///
  service.setCurrentGame = function(game)
  {
    currentGame = game;
  };

  service.getCurrentGame = function ()
  {
    return currentGame;
  };

  service.setCurrentBoard = function (board)
  {
    currentBoard = board;
  };

  service.getCurrentBoard = function ()
  {
    return currentBoard;
  };

  ////////////////////////
  // ChessBoard functions
  ////
  service.createBoard = function ()
  {
    var cfg = {
      pieceTheme: 'assets/lib/chessboardjs/img/chesspieces/wikipedia/{piece}.png',
      position: 'start',
      moveSpeed: 100,
      showNotation: false
    };

    return new ChessBoard2('board', cfg);
  };


  ////////////////////
  // ChessJS functions
  ////
  service.createGame = function ()
  {
    var game = new Chess();
    service.initGameData(game);
    return game;
  };

  service.createGamePGN = function (pgn)
  {
    var game = new Chess();
    game.load_pgn (pgn);
    service.initGameData(game);
    return game;
  };

  service.createGameUCI = function (fen, moves)
  {
    var game = new Chess (fen);
    game.startPosition = fen;

    for (var index = 0; index < moves.length; ++index) {
        var move = moves[index];
        var from = move.substring(0,2);
        var to = move.substring(2,4);
        game.move({from:from, to:to});
    }

    service.initGameData(game);

    return game;
  };
  
  service.initGameData = function (game)
  {
    game.history_all = game.history({verbose: true});

    game.currentHalfMove = -1;
    game.halfMoves = game.history();
    game.fullMoves = [];

    for (var i=0; i<game.halfMoves.length; i+=2)
    {
      game.fullMoves.push ({white: game.halfMoves[i], black: game.halfMoves[i+1]});
    }
  };

  ////////////////////////////////////
  // ChessBoard - ChessJS integration
  ////
  service.goStart = function ()
  {
    currentGame.reset();
    currentGame.currentHalfMove = -1;
    currentBoard.position(currentGame.fen());
  };

  //buttons
  service.goBack = function() {
    if (currentGame.currentHalfMove >= 0) {
      currentGame.undo();
      currentGame.currentHalfMove--;
      currentBoard.position(currentGame.fen());
    }
    else
    {
      if (currentGame.parent !== null)
      {
        service.moveToPath (currentGame.parent, currentGame.parent.currentHalfMove);
      }
    }
  };

  service.goNext = function() {
    if (currentGame.currentHalfMove < currentGame.history_all.length - 1) {
      currentGame.currentHalfMove++;
      currentGame.move(currentGame.history_all[currentGame.currentHalfMove].san);
      currentBoard.position(currentGame.fen());
    }
  };

  service.goEnd = function() {
    while (currentGame.currentHalfMove < currentGame.history_all.length - 1) {
      currentGame.currentHalfMove++;
      currentGame.move(currentGame.history_all[currentGame.currentHalfMove].san);
    }
    currentBoard.position(currentGame.fen());
  };

  service.moveToPath = function (path, ply)
  {

    //if (ply > path.history_all.length - 1) ply = gameHistory.length - 1;
    if (typeof path.startPosition === 'undefined')
      path.reset();
    else
      path.load(path.startPosition);

    for (var i = 0; i <= ply-1; i++) {
      path.move(path.history_all[i].san);
    }

    currentBoard.position(path.fen(), false);
    if (ply >= 0)
        path.move(path.history_all[ply].san);
    currentBoard.cfg.moveSpeed = 250;
    currentBoard.position(path.fen());
    currentBoard.cfg.moveSpeed = 100;

    //currentGame = path;
    //currentGame.currentHalfMove = ply;
    //service.setCurrentGame(currentGame);

    path.currentHalfMove = ply;
    service.setCurrentGame(path);
  };

  return service;
  }




	//ChessService.$inject = [];

	return ChessService;
});
