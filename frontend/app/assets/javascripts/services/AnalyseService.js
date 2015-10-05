define(['angular', 'chessboardjs', 'chessjs'],function() {
	'use strict';


    var service = null;
    function AnalyseService($rootScope, ChessService){

        // Singleton
        if (service !== null)
            return service;

        service = {};

        var websocket;

        var game, fen, skill, callback;

        service.requestAnalyse = function(_game, _skill,  _callback)
        {
            game = _game;
            fen = _game.fen();
            skill = _skill;
            callback = _callback;


            if (false) // WS not used atm
            {
                var request = JSON.stringify({type:"request_analyse",message:{fen:fen}});
                console.log ("[WebSocket] Sending: "+ request);
                if (websocket === undefined ) initSocket();

                websocket.send (request);
            }
            else
            {
                requestAnalyseComet (fen, skill);
            }
        };
        
        service.update = function (data)
        {
            console.log("Received : "+data);
            var json = JSON.parse(data);

            switch (json.type)
            {
                case "response_analyse":
                    processResult (json.message);
                    break;
                case "response_bestmove":
                    processBestmove(json.message);
                    break;
                case "response_done":
                    processDone();
                    break;
                case "response_ok" : break;
                case "response_error":
                    processError (json.message);
                    break;
            }

            $rootScope.$apply();
        };

        var processBestmove = function (bestmove)
        {
            var from = bestmove.move.substring(0,2);
            var to = bestmove.move.substring(2,4);
            if (callback.onBestmove !== undefined)
                callback.onBestmove ({from:from, to:to});
        };

        var processResult = function (res)
        {
            var paths = [];

            for (var i = 0; i < res.evaluations.length; ++i) {
                var evaluation = res.evaluations[i];
                var moves = evaluation.moves.split(' ');
                var path = ChessService.createGameUCI (fen, moves);

                var score = "";
                var scoreType = "";
                switch (evaluation.scoreType)
                {
                    case "cp":
                        score += evaluation.scoreValue < 0 ? '-' : '+';
                        scoreType = evaluation.scoreValue < 0 ? "minus" : "plus";
                        score += Math.abs(evaluation.scoreValue / 100);
                        break;
                    case "mate":
                        score = '#' + evaluation.scoreValue;
                        scoreType = "mate";
                        break;
                }
                path.score = score;
                path.parent = game;
                paths.push(path);
            }

            if (callback.onResult !== undefined)
                callback.onResult(res, paths);
        };

        var processDone = function ()
        {
            if (callback.onDone !== undefined)
                callback.onDone();
        };


        var processError = function (err)
        {
            console.log ("ERROR: "+err.errorCode);

            if (callback.onError !== undefined)
                callback.onError(err);
        };


        //////
        // Comet implementation
        var requestAnalyseComet = function (fen, skill)
        {
            //Reload iframe - send GET request
            var iframe = document.getElementById('comet-analyse-frame');
            var query = 'fen='+fen+'&skill='+skill;

            console.log ("[Comet] "+query);
            iframe.src = '/comet?'+query;
        };

        //////
        // WebSocket implementation
        var initSocket = function ()
        {
            if (websocket === undefined )
            {
                var wsurl = "ws://"+window.location.host+"/ws";
                websocket = new WebSocket (wsurl);
                websocket.onopen = function ()
                {
                    console.log ("WebSocket opened.");
                };
                websocket.onmessage = function (msg)
                {
                    service.update(msg.data);
                };
                socket.onerror = function (err)
                {
                    console.log (err);
                    alert ("SOCKET ERROR: "+err);

                };
            }

        };

        return service;
    }

	return AnalyseService;
});
