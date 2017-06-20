package reversi

import grails.async.Promises
import grails.converters.JSON
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ReversiController)
class ReversiControllerSpec extends Specification {
    ReversiController controller

    def setup() {
        controller = new ReversiController()
    }

    def cleanup() {
    }

    @Unroll
    void "test gamecontroller local"() {
        when:
            controller.initGame(0, true)
        then:"board and correct model should be returned"
            response.json.model.nextPlayer == 1
            response.json.model.canPlace == true
            response.json.model.lastReversed == []
            response.json.board == [0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 1, -1, 0, 0, 0
                                    , 0, 0, 0, -1, 1, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0]
        when:
            response.reset()
            controller.updateBoard(0)
        then: "invalid move returns same model"
            response.json.nextPlayer == 1
            response.json.canPlace == true
            response.json.lastReversed == []

        when:
            response.reset()
            controller.waitForMove()
        then: "if we wait for a move we get the same model back"
            response.json.nextPlayer == 1
            response.json.canPlace == true
            response.json.lastReversed == []

        when:
            response.reset()
            controller.updateBoard(20)
        then: "valid move returns model with new data"
            response.json.lastReversed == [28, 20]
            response.json.nextPlayer == -1 // player switched

        when:
            response.reset()
            controller.initGame(0, false)
        then: "calling controller again resets the game (also start value should be ignored)"
            response.json.model.nextPlayer == 1
            response.json.model.canPlace == true
            response.json.model.lastReversed == []
            response.json.board == [0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 1, -1, 0, 0, 0
                                    , 0, 0, 0, -1, 1, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0]
    }

    @Unroll
    void "test gamecontroller computer"() {
        when:
            controller.initGame(1, true)
        then:"board and correct model should be returned"
            response.json.model.nextPlayer == 1
            response.json.model.canPlace == true
            response.json.model.lastReversed == []
            response.json.board == [0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 1, -1, 0, 0, 0
                                    , 0, 0, 0, -1, 1, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0]
        when:
            response.reset()
            controller.updateBoard(0)
        then: "invalid move returns same model"
            response.json.nextPlayer == 1
            response.json.canPlace == true
            response.json.lastReversed == []


        when:
            response.reset()
            controller.waitForMove()
        then: "if we wait for a move we get the same model back"
            response.json.nextPlayer == 1
            response.json.canPlace == true
            response.json.lastReversed == []


        when:
            response.reset()
            controller.updateBoard(20)
        then: "valid move returns model with new data"
            response.json.lastReversed == [28, 20]
            response.json.nextPlayer == -1 // player switched

        when:
            response.reset()
            controller.updateBoard(19)
        then: "valid move does not change model if computer has to play"
            response.json.lastReversed == []
            response.json.nextPlayer == -1 // player still the same

        when:
            response.reset()
            controller.waitForMove()
        then: "computer did a move"
            response.json.lastReversed != [28, 20]
            response.json.nextPlayer == 1 // player still the same
    }

    // This test might look a bit hacky. We clear the session and reset it to be able to have 2 different clients tested
    @Unroll
    void "test gamecontroller online"() {
        when:
            controller.initGame(2, true)
        then:"board and correct model should be returned - wait should be true"
            response.json.model.nextPlayer == 1
            response.json.model.canPlace == false
            response.json.model.lastReversed == []
            response.json.wait == true
            response.json.board == [0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 1, -1, 0, 0, 0
                                    , 0, 0, 0, -1, 1, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0]

        when:
            def gC1 = session.gamecontroller
            def m1 = session.model
            def b1 = session.board
            session.clearAttributes() // You should never do this.
            response.reset();
            controller.initGame(2, false)
        then:"board and correct model should be returned - wait should be false"
            response.json.model.nextPlayer == 1
            response.json.model.canPlace == false
            response.json.model.lastReversed == []
            response.json.wait == false
            response.json.board == [0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 1, -1, 0, 0, 0
                                    , 0, 0, 0, -1, 1, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0
                                    , 0, 0, 0, 0, 0, 0, 0, 0]

        when:
            response.reset()
            controller.updateBoard(20)
        then:"Player cant update board because other player begins"
            response.json.nextPlayer == 1
            response.json.canPlace == false
            response.json.lastReversed == []

        when:
            def gC2 = session.gamecontroller
            def m2 = session.model
            def b2 = session.board
            session.gamecontroller = gC1
            session.model = m1
            session.board = b1
            response.reset()
            controller.waitForPlayer()
        then:"Beginning player finds his enemy if he tries to find him."
            response.json.nextPlayer == 1
            response.json.canPlace == true
            response.json.lastReversed == []

        when:
            response.reset()
            controller.updateBoard(20)
        then:"Beginning player can change the board"
            response.json.lastReversed == [28, 20]
            response.json.nextPlayer == -1  // player switched
            response.json.canPlace == false // player switched

        when:
            session.gamecontroller = gC2
            session.model = m2
            session.board = b2
            response.reset()
            controller.waitForMove()
        then:"Other player founds the move if he tries to find it"
            response.json.lastReversed == [28, 20]
            response.json.nextPlayer == -1  // player switched
            response.json.canPlace == true // player switched

    }
}
