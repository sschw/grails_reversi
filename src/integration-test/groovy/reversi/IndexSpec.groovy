package reversi

import geb.module.RadioButtons
import geb.spock.GebSpec
import grails.test.mixin.integration.Integration

/**
 * Created by sschw on 26.05.2017.
 */
@Integration
class IndexSpec extends GebSpec {

    /*
    We can't test it because jQuery doesn't work with that... Can't wait for fadeIn of dialog with radio buttons
    void "Play a whole game"() {
        when:
            go '/reversi/index'

            waitFor { $("#rb0").displayed }

            $("#rb0").module(RadioButtons).checked = "0"
            $("#newGame input", type: "submit").click()

            while($("#winningScreen").attr("display") == "none") {
                waitFor { $(".stone_next_p1, .stone_next_p-1").size() > 0 }
                $(".stone_next_p1, .stone_next_p-1")[0].click()
            }
        then: "We played a whole game and now we are on the winning screen"
            $("#winningScreen").attr("display") != "none"
    }*/
}
