<!doctype html>
<html>
<head>
    <title>Welcome to my Reversi</title>
    <meta name="layout" content="layout">
</head>
<body>
<section>
    <h1>Welcome to my Reversi</h1>
    <p>
        This Project is done by just using JQuery as Javascript library. The creator of the sound can be found in the <g:link controller="reversi" action="credits">credits</g:link>.
        Program is tested with firefox but should also work in most other browsers.
    </p>
    <p>
        If you don't want to read how things work in this project: <g:link controller="reversi" action="index">Here you can find the Reversi</g:link>. Have fun!
    </p>
    <h2>What is this project about</h2>
    <p>Reversi is a board game for 2 players with a 8x8 board. <a href="https://en.wikipedia.org/wiki/Reversi">Learn more about Reversi</a></p>
    <p>In this project you can play this game. You can play locally versus a friend or versus the computer or you can try the online mode.</p>
    <h2>Details how things are done</h2>
    <h3>Communication with Server</h3>
    <p>
        The client has to call initGame on the server to select a gametype. If the gametype is local or computer, we return the board, the model and wait=false.
        This makes it possible for a client to create the same board as the server has. Later communication from the server to the client will only include the model.
        Then we will look if canPlace in the model is true. This indicates if the client can place a stone.
        If he can, we draw the help circles and register click handlers onto them.
        By clicking on a stone, we send the id number of the stone to the server and receive an updated model.
        If he can't place a stone we call waitForMove on the server which blocks the server until we get an updated model back.
        After receiving the updated model, we always draw the updated board onto the screen and check again for the canPlace variable.
    </p>
    <p>
        If the updated model contains a valid value in the winner variable we don't call waitForMove or register the handlers but show the winning or loosing dialog.
    </p>
    <p>
        You might ask now yourself: Wait this is for computer and local...
    </p>
    <h3>What's with the online mode?</h3>
    <p>
        The online mode can work like the computer mode but when starting the game by calling initGame, we might not have an opponent.
        If we don't have an opponent, we call waitForPlayer. This will block the request until an opponent could be found.
        After that, the game can proceed like a computer or local game.
    </p>
    <p>Please note that you can't play against yourself within the same browser as the online mode saves your data in the session variable.</p>
    <h2>How does the Computer work</h2>
    <p>
        The computer uses alpha-beta pruning to find the most optimal move for himself. He tries to think at least 6 moves ahead.
        Later in the game he will calculate more moves ahead.
        How he rate the field won't be said here. We don't want that the computer has to loose.
    </p>
    <h2>How does the Online Mode communicate with another player?</h2>
    <p>
        If a new player starts an online game, he will try to find another player in a list of players. If the list is empty, he writes himself into the list and waits.
        Another player will then find him in the list and will register a communicator between them. After that he notifies the player that the communicator is registered.
    </p>
    <h2>How could this product be improved</h2>
    <ul>
        <li>There are some design flaw.</li>
        <li>As I don't use jQuery Mobile, the application doesn't have a perfect mobile support (vclick events aren't available)</li>
        <li>Computer could still be improved by calculating the valid reverses once</li>
        <li>There might be some improvements possible in the ReversiUtil class</li>
        <li>Sound doesn't work perfectly (Is also a problem with HTML5 player)</li>
    </ul>
</section>
</body>
</html>
