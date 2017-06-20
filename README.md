# Welcome to my Reversi

This Project is done by just using JQuery as Javascript library. The creator of the sound can be found [here](http://www.freesound.org/people/Mativve/packs/22004/).
Program is tested with firefox but should also work in most other browsers.

If you don't want to read how things work in this project: You can start the project with `./grailsw run-app`. Then you can open the game [in your browser](http://localhost:8080/reversi/index). Have fun!

## What is this project about
Reversi is a board game for 2 players with a 8x8 board. [Learn more about Reversi!](https://en.wikipedia.org/wiki/Reversi)
In this project you can play this game. You can play locally versus a friend or versus the computer or you can try the online mode.

## Details how things are done

### Communication with Server
The client has to call initGame on the server to select a gametype. If the gametype is local or computer, we return the board, the model and wait=false.
This makes it possible for a client to create the same board as the server has. Later communication from the server to the client will only include the model.
Then we will look if canPlace in the model is true. This indicates if the client can place a stone.
If he can, we draw the help circles and register click handlers onto them.
By clicking on a stone, we send the id number of the stone to the server and receive an updated model.
If he can't place a stone we call waitForMove on the server which blocks the server until we get an updated model back.
After receiving the updated model, we always draw the updated board onto the screen and check again for the canPlace variable.

If the updated model contains a valid value in the winner variable we don't call waitForMove or register the handlers but show the winning or loosing dialog.

You might ask now yourself: Wait this is for computer and local...

### What's with the online mode?
The online mode can work like the computer mode but when starting the game by calling initGame, we might not have an opponent.
If we don't have an opponent, we call waitForPlayer. This will block the request until an opponent could be found.
After that, the game can proceed like a computer or local game.

*Please note that you can't play against yourself within the same browser as the online mode saves your data in the session variable.*

### How does the Computer work
The computer uses alpha-beta pruning to find the most optimal move for himself. He tries to think at least 6 moves ahead.
Later in the game he will calculate more moves ahead.
How he rate the field won't be said here. We don't want that the computer has to loose.

### How does the Online Mode communicate with another player?
If a new player starts an online game, he will try to find another player in a list of players. If the list is empty, he writes himself into the list and waits.
Another player will then find him in the list and will register a communicator between them. After that he notifies the player that the communicator is registered.

## How could this product be improved
- [ ] There are some design flaw.
- [ ] As I don't use jQuery Mobile, the application doesn't have a perfect mobile support (vclick events aren't available)
- [ ] Computer could still be improved by calculating the valid reverses once
- [ ] There might be some improvements possible in the ReversiUtil class
- [ ] Sound doesn't work perfectly (Is also a problem with HTML5 player)
