package reversi

import grails.converters.JSON

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

class ReversiController {

    /**
     * Used to load the view.
     */
    def index() {}

    /**
     * We initialize the game.
     * @param gamecontroller 0, 1 or 2 = Local, Computer or Online
     * @param start If the player wants to start. (Online may change this value if both want the same)
     * @return JSON with board, model and a boolean, indicating if we have to wait
     */
    def initGame(int gamecontroller, boolean start) {
        if(session["gamecontroller"] != null) session["gamecontroller"].reset()
        session["gamecontroller"] = GameController.getGamecontrollerForInt(gamecontroller, start)

        session["board"] = new int[64] // 1-dim array for performance
        session["model"] = new BoardModel()

        session.board[3 * 8 + 3] = 1
        session.board[4 * 8 + 4] = 1
        session.board[4 * 8 + 3] = -1
        session.board[3 * 8 + 4] = -1
        session.model.nextPlayer = 1
        session.model.nextMoves = ReversiUtils.availableMoves(session.board, session.model.nextPlayer)
        session.model.lastReversed = []

        if(session.gamecontroller.isReady()) {
            session.gamecontroller.updateCanPlace(session.model)
            def respObj = [model: session.model, board: session.board, wait: false]
            render respObj as JSON
        }
        else {
            def respObj = [model: session.model, board: session.board, wait: true]
            render respObj as JSON
        }
    }

    /**
     * We wait for a player. (Only needed for online)
     * Does nothing if another gamecontroller is taken and this method is called.
     * @return JSON with model
     */
    def waitForPlayer() {
        // game has to be initialized to call this method. If model is null we return a 500 error
        if(session.model == null) {
            response.status = 500
        } else {
            session.gamecontroller.waitForPlayer()
            session.gamecontroller.updateCanPlace(session.model)
            render session.model as JSON
        }
    }

    /**
     * Updates the model for the cell that the user wants to place.
     * Does nothing if the user isn't allowed to place a stone.
     * @param cellPlaced
     * @return JSON with model
     */
    def updateBoard(int cellPlaced) {
        // game has to be initialized to call this method. If model is null we return a 500 error
        if(session.model == null) {
            response.status = 500
        } else {
            session.model.lastReversed = []
            if(session.model.canPlace)
                session.gamecontroller.updateBoard(cellPlaced, session)
            render session.model as JSON
        }
    }

    /**
     * Player waits for another player to do his move.
     * Does nothing if the can place a stone.
     * @return JSON with model
     */
    def waitForMove() {
        // game has to be initialized to call this method. If model is null we return a 500 error
        if(session.model == null) {
            response.status = 500
        } else {
            session.model.lastReversed = []
            if (session.model.canPlace == false) {
                int cellPlaced = session.gamecontroller.waitForMove(session.board)
                // XXX It should never be able to get an invalid stone (-1 from local because we already check if the player can't place.
                // XXX We keep this check for future changes. ;-)
                if (cellPlaced >= 0)
                    session.gamecontroller.updateBoard(cellPlaced, session)
            }
            render session.model as JSON
        }
    }

    /**
     * We have credits for the music ;-)
     */
    def credits() {}
}

/**
 * Model that we use to communicate with the client.
 */
class BoardModel {
    int[] nextMoves
    int[] lastReversed
    int nextPlayer
    boolean canPlace
    int winner = 2 // we cant initialize it with 0 because that would be a tie.
}

/**
 * Abstract class for the gamecontrollers.
 * <br/>
 * Has a basic implementation of the game flow.
 */
abstract class GameController {
    abstract waitForMove(board)
    abstract updateCanPlace(model)
    def isReady() { return true }
    def waitForPlayer() {}
    def reset() {}

    /**
     * Returns the gamecontroller for the given int value
     * @param gamecontroller describes the gamecontroller that should be loaded.
     * @param start describes if the player wants to start.
     * @return the gamecontroller for the given int
     */
    static getGamecontrollerForInt(int gamecontroller, boolean start) {
        switch(gamecontroller) {
            case 0: new LocalGameController(); break
            case 1: new ComputerGameController(start); break
            case 2: new OnlineGameController(start); break
        }
    }

    /**
     * Updates the board for the cell that the player wants to place
     * @param cellPlaced cell that should be placed
     * @param session the session with contains the board and {@link BoardModel}
     * @return void (changes are done in the session object)
     */
    def updateBoard(int cellPlaced, session) {
        if (cellPlaced in session.model.nextMoves) {
            session.board[cellPlaced] = session.model.nextPlayer
            def stonesToReverse = ReversiUtils.getValidReverses(session.board, cellPlaced, session.model.nextPlayer)
            ReversiUtils.reverseStones(session.board, stonesToReverse as int[])
            // Use reversed stones to save last changed stones
            stonesToReverse << cellPlaced
            session.model.lastReversed = stonesToReverse as int[]
            // Switch player
            session.model.nextPlayer = -session.model.nextPlayer

            // Check if player has moves
            session.model.nextMoves = ReversiUtils.availableMoves(session.board, session.model.nextPlayer)
            // Player has no move so switch player
            if (session.model.nextMoves.length == 0) {
                session.model.nextPlayer = -session.model.nextPlayer
                session.model.nextMoves = ReversiUtils.availableMoves(session.board, session.model.nextPlayer)
                // Player has no move so game is finished
                if(session.model.nextMoves.length == 0) {
                    session.model.winner = ReversiUtils.calcWinner(session.board);
                }
            }
            // Update the canPlace value
            updateCanPlace(session.model)
        }
    }
}

/**
 * This gamecontroller handles the game for local games (both players on the same computer)
 */
class LocalGameController extends GameController {

    def waitForMove(board) {
        // Its local - the user does not have to wait for anything.
        return -1
    }

    /**
     * Player can always place
     * @param model
     * @return
     */
    def updateCanPlace(model) {
        model.canPlace = true
    }
}
/**
 * This gamecontroller handles the game for games against the computer.
 * <br/>
 * As the calculation isn't that efficient, we use an {@link ExecutorService} to run {@link Callable} parallel in order
 * to use all processors of the computer. To split the work, we calculate the first possible moves sequential and split
 * the work then to the underling Callable.
 * <br/>
 * As the computer should change its strategy at the end of the game, we have a Callable taking 2 different boardValues,
 * 1 for early and midgame and 1 for endgame calculations.
 * @see reversi.ComputerGameController.CalcTask
 */
class ComputerGameController extends GameController {
    private final static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final static int[] EARLY_MID_GAME_VALUES = [
            9999,   5, 500, 200, 200, 500,   5, 9999,
            5,   1, 50,  150, 150,  50,   1,    5,
            500,  50, 250, 100, 100, 250,  50,  500,
            200, 150, 100,  50,  50, 100, 150,  200,
            200, 150, 100,  50,  50, 100, 150,  200,
            500,  50, 250, 100, 100, 250,  50,  500,
            5,   1, 50,  150, 150,  50,   1,    5,
            9999,   5, 500, 200, 200, 500,   5, 9999
    ]
    private final static int[] END_GAME_VALUES = [
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1
    ]

    private int player // Defines what player the client is.

    /**
     * @param start ComputerPlayer replaces a local player so canPlace has to be updated according to the starting
     * player
     */
    ComputerGameController(boolean start) {
        this.player = start ? 1 : -1
    }

    /**
     * Calculates the next move using negamax alogrithm in a parallel way.
     * @param board the board for calculation
     * @return the calculated move
     */
    def waitForMove(board) {
        // Calc computer move with alpha beta pruning
        int[] rev = ReversiUtils.availableMoves(board, -player)

        int roundsPlayed = ReversiUtils.roundsPlayed(board)

        int depth = 6
        if(roundsPlayed > 54)
            depth = 64-roundsPlayed
        else if(roundsPlayed > 44)
            depth = 8

        int[] boardValue = EARLY_MID_GAME_VALUES
        if(depth+roundsPlayed >= 64)
            boardValue = END_GAME_VALUES

        List<Future<Integer[]>> list = new ArrayList<>()

        for(int r : rev) {
            list.add(executorService.submit(new CalcTask(board, r, -player, depth-1, boardValue)))
        }
        int move = list.stream().map{ f -> f.get() }.max(Comparator.comparingInt{ f -> f[0] }).map{ f -> f[1] }.get()
        return move
    }

    /**
     * non computer player can place if he is next player
     * @param model
     * @return
     */
    def updateCanPlace(model) {
        model.canPlace = player == model.nextPlayer
    }

    /**
     * Calculates the best possible move recursive for the players and calculates the rate the situation at the end.
     * <br/>
     * Uses an implementation of alpha-beta pruning.
     */
    private class CalcTask implements Callable<Integer[]> {
        private int[] localBoard
        private int localPlayer
        private int localDepth
        private int localPlaceField
        private int[] localBoardValue

        CalcTask(int[] board, int placeField, int player, int depth, int[] boardValue) {
            localBoard = Arrays.copyOf(board, board.length)
            localBoard[placeField] = player
            localPlaceField = placeField
            ReversiUtils.reverseStones(localBoard, ReversiUtils.getValidReverses(localBoard, placeField, player) as int[])
            localPlayer = -player
            localDepth = depth
            localBoardValue = boardValue
        }

        Integer[] call() throws Exception {
            return [ -calcMove(localPlayer, localDepth, Integer.MIN_VALUE, Integer.MAX_VALUE), localPlaceField ]
        }

        private calcMove(int p, int d, int alpha, int beta) {
            if(d == 0)
                return calcSituation(p)
            int[] nextMoves = ReversiUtils.availableMoves(localBoard, p)
            if(nextMoves.length == 0) {
                if (ReversiUtils.availableMoves(localBoard, -p).size() == 0)
                    return calcSituation(p)
                return -calcMove(-p, d, -beta, -alpha)
            }
            int maxValue = alpha
            for(int i = 0; i < nextMoves.length; i++) {
                int currentCell = nextMoves[i]
                localBoard[currentCell] = p
                int[] revs = ReversiUtils.getValidReverses(localBoard, currentCell, p)
                ReversiUtils.reverseStones(localBoard, revs)
                int val = -calcMove(-p, d-1, -beta, -maxValue)
                ReversiUtils.reverseStones(localBoard, revs)
                localBoard[currentCell] = 0
                if(val > maxValue) {
                    maxValue = val
                    if(maxValue >= beta)
                        break
                }
            }
            return maxValue
        }

        private calcSituation(int player) {
            int gameValue = 0
            for(int i = 0; i < 64; i++) {
                gameValue += localBoardValue[i] * localBoard[i]
            }
            return gameValue * player
        }
    }
}

/**
 * This gamecontroller handles the game for online games.
 * <br/>
 * We use a {@link BlockingQueue} to connect 2 players. (first player will be blocked, second player will find player 1
 * in {@link BlockingQueue} and notify him that he was found. (non-blocking load)<br/>
 * The 2 players will define a communicator ({@link BlockingQueue}) and will communicate over it by writing its moves
 * into it and read out the other move out of the {@link BlockingQueue}. (blocking the request until the move is
 * available)
 */
class OnlineGameController extends GameController {
    private final static BlockingQueue<OnlineGameController> BQ_WAITING_PLAYERS = new LinkedBlockingQueue<>()
    private BlockingQueue<Integer> communicator
    private boolean start

    OnlineGameController(boolean s) {
        start = s
    }

    /**
     * We block the request until we get something into the blocking queue.
     * @param board
     * @return
     */
    def waitForMove(board) {
        return communicator.take();
    }

    /**
     * If player starts another gametype, reset his values.
     * @return
     */
    def reset() {
        // If the client was waiting
        BQ_WAITING_PLAYERS.remove(this)
        // If client was communicating (remove to help GC)
        communicator = null
    }

    /**
     * After update the board, send the cell that we changed to the other player (write into Queue) if we have changes
     * (lastReversed has changed)
     * @param cellPlaced
     * @param session
     * @return
     */
    def updateBoard(int cellPlaced, session) {
        int[] s = session.model.lastReversed
        boolean isThisPlayer = session.model.nextPlayer == (start ? 1 : -1)
        super.updateBoard(cellPlaced, session)
        if(s != session.model.lastReversed && isThisPlayer)
            communicator.add(cellPlaced)
    }

    /**
     * We try to get a waiting player from the blocking queue. <br/>
     * If we don't have a player, we write ourself into the list.<br/>
     * If we have a player, we create a communicator, register it for me and for the enemy and notify the enemy by
     * calling {@link #registerEnemy(java.lang.Object, boolean)}
     * @return true if we could establish a connection
     */
    def isReady() {
        OnlineGameController enemy = BQ_WAITING_PLAYERS.poll()
        if(enemy != null) {
            communicator = new ArrayBlockingQueue<>(1)

            if(enemy.start && start || !enemy.start && !start)
                start = Math.round(Math.random()) == 1
            enemy.registerEnemy(communicator, !start)
            return true
        }
        BQ_WAITING_PLAYERS.put(this)
        return false
    }

    /**
     * We were found by another player so we register the communicator and notify our other blocked thread.
     * @param communicator
     * @param start
     * @return
     */
    private registerEnemy(communicator, boolean start) {
        this.communicator = communicator
        this.start = start
        // We notify the other client that he is initialized if he is already in the waitForPlayer method
        synchronized (this) {
            notifyAll()
        }
    }

    /**
     * We block ourself until we have a communicator registered
     */
    synchronized waitForPlayer() {
        // We wait for another client reading out the blocking queue
        while(communicator == null) {
            try {
                wait()
            } catch(Exception ex) {}
        }
    }

    /**
     * We can place if we are the player that should be able to place. (sounds strange but that's what we do here)
     * @param model
     * @return
     */
    def updateCanPlace(model) {
        model.canPlace = start && model.nextPlayer == 1 || !start && model.nextPlayer == -1
    }
}

class ReversiUtils {
    private ReversiUtils() {}

    /**
     * Checks for available moves and returns them
     * @param board the game board to check on
     * @param player the player that should be checked for
     * @return list with all available moves
     */
    static availableMoves(int[] board, int player) {
        def availableMoves = []
        for(int i = 0; i < 64; i++) {
            if(board[i] == 0)
                if(getValidReverses(board, i, player).size() > 0)
                    availableMoves << i
        }
        return availableMoves
    }

    /**
     * Checks for a cell what reverses could be done.
     * @param board the game board to check on
     * @param cellToCheck the cell to check for valid reverses
     * @param player the player that should be checked for
     * @return list with all valid reverses if the player places the stone on this cell
     */
    static getValidReverses(int[] board, int cellToCheck, int player) {
        def validReverses = []
        int otherPlayer = -player
        int cellRow = cellToCheck/8
        int cellCol = cellToCheck%8
        for(int dirX = -1; dirX < 2; dirX++) {
            for(int dirY = -1; dirY < 2; dirY++) {
                // If there is no direction (dirX = 0, dirY = 0) - dont try to check this direction.
                if(dirX != 0 || dirY != 0) {
                    int nextRow = cellRow+dirY
                    int nextCol = cellCol+dirX
                    int nextCell = (nextRow * 8) + nextCol
                    def evValidReverses = []
                    while (nextRow >= 0 && nextRow < 8 && nextCol >= 0 && nextCol < 8 && board[nextCell] == otherPlayer) {
                        evValidReverses << nextCell
                        nextRow += dirY
                        nextCol += dirX
                        nextCell = (nextRow * 8) + nextCol
                    }
                    if (nextRow >= 0 && nextRow < 8 && nextCol >= 0 && nextCol < 8 && board[nextCell] == player)
                        validReverses += evValidReverses
                }
            }
        }
        return validReverses
    }

    /**
     * Reverse the given stones on the given board
     * @param board the board to reverse on
     * @param reverseStones the stones that should be reversed
     * @return void
     */
    static reverseStones(int[] board, int[] reverseStones) {
        for(def stone : reverseStones) {
            board[stone] = -board[stone];
        }
        return board;
    }

    /**
     * Counts the stones on the field to say how many rounds already have been played
     * @param board the board to check on
     * @return int with number of stones on the board. (value - 4 = actually rounds as 4 stones already have been placed at the start)
     */
    static roundsPlayed(int[] board) {
        int i = 0
        for(int j = 0; j < 64; j++) {
            if(board[j] != 0)
                i++
        }
        return i
    }

    /**
     * Calcs the winner
     * @param board the board to check on
     * @return -1, 0 or 1 indicating the winner.
     */
    static calcWinner(int[] board) {
        int i = 0
        for(int j = 0; j < 64; j++) {
            i += board[j];
        }
        return Math.signum(i)
    }
}