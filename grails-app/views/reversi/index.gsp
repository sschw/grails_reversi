<html>
<head>
    <meta name="layout" content="layout"/>
    <title>Reversi - Game</title>
</head>
<body>
    <audio id="cancel">
        <source src="/static/audio/close-sound.wav" type="audio/wav" />
    </audio>
    <audio id="loose">
        <source src="/static/audio/loose-sound.wav" type="audio/wav" />
    </audio>
    <audio id="open">
        <source src="/static/audio/open-sound.wav" type="audio/wav" />
    </audio>
    <audio id="success">
        <source src="/static/audio/success-sound.wav" type="audio/wav" />
    </audio>
    <audio id="win">
        <source src="/static/audio/win-sound.wav" type="audio/wav" />
    </audio>
    <section id="game">
        <h2>Reversi</h2>

        <div class="board">
            <g:each in="${(0..7).toList()}" var="row">
                <div class="board_row">
                    <g:each in="${(0..7).toList()}" var="col">
                    %{-- First row/col have also a border on top/left | cell_r1 and cell_c1 is added to them --}%
                        <div class="board_cell${row == 0? " cell_r1":""}${col == 0? " cell_c1":""}" id="cell_${row*8+col}">
                            <div class="stone"></div>
                        </div>
                    </g:each>
                </div>
            </g:each>
        </div>
    </section>
    <div class="overlay">
        <div class="dialog" id="newGame">
            <h3>Start a new game:</h3>
            <g:form action="./initGame" method="POST">
                <div>
                    <span>Please select your preferred gamemode:</span>
                    <ul>
                        <li><input name="gamecontroller" id="rb0" value="0" type="radio" /><label for="rb0">Local</label></li>
                        <li><input name="gamecontroller" id="rb1" value="1" type="radio" /><label for="rb1">Computer</label></li>
                        <li><input name="gamecontroller" id="rb2" value="2" type="radio" /><label for="rb2">Online</label></li>
                    </ul>
                </div>
                <div>
                    <label for="cb0">Do you want to begin? </label><input name="start" id="cb0" type="checkbox" value="true"/>
                </div>
                <input type="submit" value="Start" />
                <div style="clear: both;"></div>
            </g:form>
        </div>
        <div class="dialog" id="winningScreen">
            <h1>Congratulation!</h1>
            <p>You (Player <span class="winner"></span>) have won.</p>
            <p>Do you want to play again?</p>
            <button>Yes</button>
            <div style="clear: both;"></div>
        </div>
        <div class="dialog" id="tieScreen">
            <h1>Well played!</h1>
            <p>You haven't won but at least it's a tie.</p>
            <p>Do you want to play again?</p>
            <button>Yes</button>
            <div style="clear: both;"></div>
        </div>
        <div class="dialog" id="loosingScreen">
            <h1>That was close!</h1>
            <p>The other player (Player <span class="winner"></span>) has won.</p>
            <p>Do you want to play again?</p>
            <button>Yes</button>
            <div style="clear: both;"></div>
        </div>
    </div>
    <aside class="infopanel">
        <h3>Stats:</h3>
        <ul>
            <li>Player 1: <span id="noP1">02</span></li>
            <li>Player 2: <span id="noP2">02</span></li>
        </ul>
    </aside>
    <div class="wait">
        <div>
            <div class="spinner"></div>
            <div class="waitingFor"></div>
        </div>
    </div>

    <asset:javascript src="game.js" />
    <script type="application/javascript">
        $(document).ready(function() {
            initStartGameDialog();
            initWinningScreen();
            initTieScreen();
            initLoosingScreen();
            showStartGameDialog();
        });
    </script>
</body>
</html>