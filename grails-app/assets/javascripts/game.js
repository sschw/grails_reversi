/**
 * Created by sschw on 16.05.2017.
 */

function pad(s) {
    return (s < 10) ? ("0" + s) : s;
}

function initStartGameDialog() {
    // Register radiobutton handler to activate checkbox
    // You can't select if you want to start if its a local game
    if($("#rb0").prop("checked") == true)
        $("#cb0").prop("disabled", true);
    else
        $("#cb0").prop("disabled", false);
    $("input:radio[name=\"gamecontroller\"]").change(function(evt) {
        if($(this).val() == 0)
            $("#cb0").prop("disabled", true);
        else
            $("#cb0").prop("disabled", false);
    });

    // Register submit handler
    // Start the game by calling initGame on Server and initGame in this JS script when the server returns its data
    $("#newGame form").submit(function(evt) {
        evt.preventDefault();
        $.ajax("./initGame", { data: $(this).serialize(), method: "POST"}).done(function(data) {
            initGame(data);
            $("#newGame").fadeOut();
            $(".overlay").fadeOut();
            $("#open")[0].pause();
            $("#open")[0].currentTime = 0;
            $("#open")[0].play();
        });
    });
}

function initWinningScreen() {
    $("#winningScreen button").click(function() {
        $("#open")[0].pause();
        $("#open")[0].currentTime = 0;
        $("#open")[0].play();
        $("#winningScreen").fadeOut(400, showStartGameDialog);
    });
}

function initTieScreen() {
    $("#tieScreen button").click(function() {
        $("#open")[0].pause();
        $("#open")[0].currentTime = 0;
        $("#open")[0].play();
        $("#tieScreen").fadeOut(400, showStartGameDialog());
    });
}

function initLoosingScreen() {
    $("#loosingScreen button").click(function() {
        $("#open")[0].pause();
        $("#open")[0].currentTime = 0;
        $("#open")[0].play();
        $("#loosingScreen").fadeOut(400, showStartGameDialog());
    });
}

function showStartGameDialog() {
    $(".overlay").fadeIn();
    $("#newGame").fadeIn();
}

function showWinningGameDialog(winner) {
    $("#win")[0].pause();
    $("#win")[0].currentTime = 0;
    $("#win")[0].play();
    $("#winningScreen .winner").text(winner == -1 ? 2 : 1);
    $(".overlay").fadeIn();
    $("#winningScreen").fadeIn();
}

function showTieGameDialog() {
    $("#win")[0].pause();
    $("#win")[0].currentTime = 0;
    $("#win")[0].play();
    $(".overlay").fadeIn();
    $("#tieScreen").fadeIn();
}

function showLoosingGameDialog(winner) {
    $("#loose")[0].pause();
    $("#loose")[0].currentTime = 0;
    $("#loose")[0].play();
    $("#loosingScreen .winner").text(winner == -1 ? 2 : 1);
    $(".overlay").fadeIn();
    $("#loosingScreen").fadeIn();
}

function showWait(waitingFor) {
    $(".wait .waitingFor").text(waitingFor);
    $(".wait").fadeIn();
}

function hideWait(waitingFor) {
    $(".wait").fadeOut();
}

function initGame(data) {
    // Draw the board.
    for(var i = 0; i < data.board.length; i++) {
        $("#cell_" + i + " .stone").removeClass("stone_p0 stone_p1 stone_p-1").addClass("stone_p" + data.board[i]);
    }
    // Draw next moves if the player begins
    if(data.model.canPlace) {
        for (var i = 0; i < data.model.nextMoves.length; i++) {
            var stone = $("#cell_" + data.model.nextMoves[i] + " .stone");
            stone.addClass("stone_next_p1");
        }
    }

    startGame(data.model.nextPlayer, data.wait, data.model.canPlace);
}

function startGame(player, wait, canPlace) {
    var previousPlayer;
    var nextPlayer = player;
    var noStones = [2, 2];
    var stones = $(".stone");
    $("#noP1").text(pad(noStones[0]));
    $("#noP2").text(pad(noStones[1]));

    // Define function which updates the board.
    function updateBoard(elem) {
        // Read out the id of the cell
        var cellid = $($(elem).parent()).attr('id');
        // Calculate the index of the cell
        var stone = parseInt(cellid.substr(5));
        // Reset all click actions on the stone div
        stones.off("click");
        stones.off("touchstart");
        // Remove classes that are reallocated
        stones.removeClass("stone_new stone_next_p1 stone_next_p-1");

        // Send the index to the server | use POST (because data do change and one time action) | write Accept: text/json into the header
        $.ajax("./updateBoard", { method: "POST", data: {cellPlaced: stone}, headers: {Accept: "text/json"}})
            .done(drawBoard);
    }

    function drawBoard(data) {
        // If we were waiting before drawing, we hide the wait.
        hideWait();

        // Define the player that just placed his stone.
        previousPlayer = nextPlayer;
        nextPlayer = data.nextPlayer;
        // BoardModel received as json

        // Update game stats
        if(data.lastReversed.length > 0) {
            noStones[-(previousPlayer - 1) / 2] += data.lastReversed.length;
            noStones[(previousPlayer + 1) / 2] -= (data.lastReversed.length - 1);
            $("#noP1").text(pad(noStones[0]));
            $("#noP2").text(pad(noStones[1]));
        }

        // Loop through last reversed stones board in the BoardModel
        for(var i = 0; i < data.lastReversed.length; i++) {
            var stone = $("#cell_" + data.lastReversed[i] + " .stone");
            if(stone.hasClass("stone_p0")) {
                stone.removeClass("stone_p0");
                stone.addClass("stone_p" + previousPlayer);
            } else if(stone.hasClass("stone_p1")) {
                stone.removeClass("stone_p1");
                stone.addClass("stone_p-1");
            } else {
                stone.removeClass("stone_p-1");
                stone.addClass("stone_p1");
            }
            stone.addClass("stone_new");
            // Fade in new stones.
            stone.stop();
            stone.css("display", "none");
            stone.fadeIn();
        }

        // If winner != 2 we have a winner or a tie.
        if(data.winner != 2) {
            // We dont save locally if the player is 1 or -1 because he could be both. (Local game)
            if(data.winner == 0)
                showTieGameDialog();
            else if (data.winner == data.nextPlayer && data.canPlace || data.winner != data.nextPlayer && !data.canPlace)
                showWinningGameDialog(data.winner);
            else
                showLoosingGameDialog(data.winner);
        } else {
            // Show indicators for next move and register the action if it is a valid stone to place.
            if (data.canPlace) {
                for (var i = 0; i < data.nextMoves.length; i++) {
                    var stone = $("#cell_" + data.nextMoves[i] + " .stone");
                    stone.addClass("stone_next_p" + nextPlayer);
                    stone.click(function (evt) {
                        $("#open")[0].pause();
                        $("#open")[0].currentTime = 0;
                        $("#open")[0].play();
                        updateBoard(this);
                    });
                    stone.on('touchstart', function(evt) {
                        $("#open")[0].pause();
                        $("#open")[0].currentTime = 0;
                        $("#open")[0].play();
                        updateBoard(this);
                    });
                }
            } else {
                showWait("Wait for enemy to make his move");
                // Player can't place the next stone so we just wait for the move from the server.
                $.ajax("./waitForMove", {method: "POST", headers: {Accept: "text/json"}})
                    .done(function (data) {
                        $("#cancel")[0].pause();
                        $("#cancel")[0].currentTime = 0;
                        $("#cancel")[0].play();
                        drawBoard(data);
                    });
            }
        }
    }

    // Register click action on all stone div's which are available for p1 or if he isn't p1 wait for the move.
    // If the other player isn't available (online mode) - we wait for the player.
    if(wait) {
        showWait("Wait for enemy connecting");
        $.ajax("./waitForPlayer", {method: "POST", headers: {Accept: "text/json"}})
            .done(function(data) {
                $("#success")[0].pause();
                $("#success")[0].currentTime = 0;
                $("#success")[0].play();
                drawBoard(data);
            });
    }
    else if(canPlace) {
        $(".stone_next_p1").click(function (evt) {
            $("#open")[0].pause();
            $("#open")[0].currentTime = 0;
            $("#open")[0].play();
            updateBoard(this);
        });
        $(".stone_next_p1").on('touchstart', function (evt) {
            $("#open")[0].pause();
            $("#open")[0].currentTime = 0;
            $("#open")[0].play();
            updateBoard(this);
        });
    }
    else {
        showWait("Wait for enemy to make his move");
        $.ajax("./waitForMove", {method: "POST", headers: {Accept: "text/json"}})
            .done(drawBoard);
    }
}