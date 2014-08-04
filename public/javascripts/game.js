$(document).ready(function() {
    "use strict";

    // Hide board elements initially.
    $.each(["playerId", "opponentId", "nextPlayerId", "board"], function(_, selector) {
        $("#" + selector).hide();
    });

    var updateStatus = function(message) {
        $("#status span").text(message);
        console.log(message);
    };

    // Set initial status.
    updateStatus("Connecting...");

    var playerId, opponentId, nextPlayerId;

    var handleWaitingForOpponent = function(data) {
        playerId = data.playerId;
        $("#playerId span").text(playerId);
        $("#playerId").show();
        updateStatus("Connected. Waiting for opponent...");
    };

    var updateBoard = function(board, setCallbacks) {
        $.each(board, function(pid, pits) {
            var selector = "#" + (pid == playerId ? "player" : "opponent") + "-pit";
            $.each(pits, function(pos, count) {
                $(selector + pos + " .count").text(count);
            });
        });

        $.each(board[playerId], function(pos, count) {
            var selector = "#player-pit" + pos + " .count";
            if (playerId == nextPlayerId && count > 0) $(selector).removeAttr("disabled");
            else $(selector).attr("disabled", true);
            if (setCallbacks) $(selector).click(function() {
                ws.send(JSON.stringify(pos));
                updateStatus("Pit " + pos + " is clicked.");
            });
        });
    };

    var setBoardColors = function(colors) {
        for (var i = 0; i < 7; i++)
            $.each(["player", "opponent"], function(pos, key) {
                $("#" + key + "-pit" + i).css("background-color", colors[key]);
            });
    };

    var handleReadyToStart = function(data) {
        opponentId = data.opponentId;
        nextPlayerId = data.nextPlayerId;
        $("#opponentId span").text(opponentId);
        $("#opponentId").show();
        $("#nextPlayerId span").text(nextPlayerId);
        $("#nextPlayerId").show();

        setBoardColors(
            (nextPlayerId == playerId)
            ? {player: "green", opponent: "gray"}
            : {player: "gray", opponent: "green"});

        $("#board").show();
        var board = {};
        board[playerId] = board[opponentId] = [6, 6, 6, 6, 6, 6, 0];
        updateBoard(board, true);

        updateStatus("Paired. Waiting for move...");
    };

    var handleIllegalMove = function(data) {
        updateStatus("Illegal move: " + data.reason);
    }

    var handleBoardState = function(data) {
        nextPlayerId = data.nextPlayerId;
        $("#nextPlayerId span").text(nextPlayerId);
        updateBoard(data.board);
        updateStatus("Received board state.");
    };

    var disableButtons = function() {
        for (var i = 0; i < 6; i++)
            $("#player-pit" + i + " .count").attr("disabled", true);
    };

    var handleGameOver = function(data) {
        alert("You " + (data.winnerId == playerId ? "win" : "lost") + "!");
        disableButtons();
    };

    ws.onmessage = function(event) {
        var data = JSON.parse(event.data);
        if ("type" in data)
            switch (data.type) {
                case "WaitingForOpponent": return handleWaitingForOpponent(data);
                case "ReadyToStart": return handleReadyToStart(data);
                case "IllegalMove": return handleIllegalMove(data);
                case "BoardState": return handleBoardState(data);
                case "GameOver": return handleGameOver(data);
            }
        updateStatus("Invalid WS event: " + JSON.stringify(data));
    };

    var onalert = function(event) {
        updateStatus(
            ("type" in event && event.type == "close")
            ? "Connection lost!"
            : ("Alert received: " + JSON.stringify(event)));
        ws.close();
        disableButtons();
    };

    ws.onerror = onalert;
    ws.onclose = onalert;

});
