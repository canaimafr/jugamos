package com.canaima.frjugamos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreTrackerViewModelTest {
    @Test
    fun gameRequiresTwoPlayersAndTrimsNames() {
        val viewModel = ScoreTrackerViewModel()
        assertEquals(AppScreen.HOME, viewModel.state.value.screen)
        viewModel.openSetup()

        viewModel.addPlayer("  Alice  ")
        viewModel.addPlayer(" ")
        viewModel.startGame()

        assertEquals(AppScreen.SETUP, viewModel.state.value.screen)
        assertEquals(listOf("Alice"), viewModel.state.value.players.map { it.name })

        viewModel.addPlayer("Bob")
        viewModel.startGame()

        assertEquals(AppScreen.GAME, viewModel.state.value.screen)
    }

    @Test
    fun scoresAreRankedAndReplayResetsThem() {
        val viewModel = ScoreTrackerViewModel()
        viewModel.addPlayer("Alice")
        viewModel.addPlayer("Bob")
        viewModel.startGame()
        val aliceId = viewModel.state.value.players[0].id
        val bobId = viewModel.state.value.players[1].id

        viewModel.changeScore(aliceId, 2)
        viewModel.changeScore(bobId, -1)
        viewModel.finishGame()

        assertEquals(AppScreen.RESULTS, viewModel.state.value.screen)
        assertEquals(
            listOf("Alice", "Bob"),
            viewModel.state.value.players.sortedByDescending { it.score }.map { it.name },
        )

        viewModel.replay()

        assertEquals(AppScreen.GAME, viewModel.state.value.screen)
        assertTrue(viewModel.state.value.players.all { it.score == 0 })
        assertEquals(VictoryMode.MOST_POINTS, viewModel.state.value.victoryMode)
    }

    @Test
    fun startingANewGameReturnsToSetupWithPlayersAndResetScores() {
        val viewModel = ScoreTrackerViewModel()
        viewModel.addPlayer("Alice")
        viewModel.addPlayer("Bob")
        viewModel.startGame()
        val aliceId = viewModel.state.value.players[0].id
        viewModel.changeScore(aliceId, 3)
        viewModel.finishGame()
        viewModel.setVictoryMode(VictoryMode.FEWEST_POINTS)

        viewModel.newGame()

        assertEquals(AppScreen.HOME, viewModel.state.value.screen)
        assertEquals(listOf("Alice", "Bob"), viewModel.state.value.players.map { it.name })
        assertTrue(viewModel.state.value.players.all { it.score == 0 })
        assertEquals(VictoryMode.MOST_POINTS, viewModel.state.value.victoryMode)

        viewModel.openSetup()
        assertEquals(AppScreen.SETUP, viewModel.state.value.screen)
    }

    @Test
    fun clearingAllPlayersLeavesSetupReadyForANewPlayerList() {
        val viewModel = ScoreTrackerViewModel()
        viewModel.openSetup()
        viewModel.addPlayer("Alice")
        viewModel.addPlayer("Bob")

        viewModel.removeAllPlayers()

        assertEquals(AppScreen.SETUP, viewModel.state.value.screen)
        assertTrue(viewModel.state.value.players.isEmpty())

        viewModel.addPlayer("Charlie")
        assertEquals(listOf("Charlie"), viewModel.state.value.players.map { it.name })
    }

    @Test
    fun signedScoreAdjustmentsAreParsedAndApplied() {
        val viewModel = ScoreTrackerViewModel()
        viewModel.addPlayer("Alice")
        val aliceId = viewModel.state.value.players.single().id

        assertEquals(12, parseScoreAdjustment("+12"))
        assertEquals(-4, parseScoreAdjustment("-4"))
        assertEquals(0, parseScoreAdjustment("0"))

        viewModel.changeScore(aliceId, requireNotNull(parseScoreAdjustment("+12")))
        viewModel.changeScore(aliceId, requireNotNull(parseScoreAdjustment("-4")))

        assertEquals(8, viewModel.state.value.players.single().score)
    }

    @Test
    fun invalidOrOverflowingScoreAdjustmentsAreRejected() {
        assertEquals(null, parseScoreAdjustment(""))
        assertEquals(null, parseScoreAdjustment("1.5"))
        assertEquals(null, parseScoreAdjustment("999999999999"))

        val viewModel = ScoreTrackerViewModel()
        viewModel.addPlayer("Alice")
        val aliceId = viewModel.state.value.players.single().id
        viewModel.changeScore(aliceId, Int.MAX_VALUE)
        viewModel.changeScore(aliceId, 1)

        assertEquals(Int.MAX_VALUE, viewModel.state.value.players.single().score)
    }

    @Test
    fun playersAreRankedBySelectedVictoryModeAndTiesAreAllWinners() {
        val players = listOf(
            Player(id = 1, name = "Alice", score = 4),
            Player(id = 2, name = "Bob", score = 1),
            Player(id = 3, name = "Charlie", score = 1),
            Player(id = 4, name = "Drew", score = 8),
        )

        assertEquals(
            listOf("Drew", "Alice", "Bob", "Charlie"),
            rankPlayers(players, VictoryMode.MOST_POINTS).map { it.name },
        )
        assertEquals(
            listOf("Bob", "Charlie"),
            winningPlayers(players, VictoryMode.FEWEST_POINTS).map { it.name },
        )
        assertEquals(
            listOf("Drew"),
            winningPlayers(players, VictoryMode.MOST_POINTS).map { it.name },
        )
    }

    @Test
    fun victoryModeDefaultsToMostPoints() {
        assertEquals(VictoryMode.MOST_POINTS, ScoreTrackerViewModel().state.value.victoryMode)
        assertEquals(AppScreen.HOME, ScoreTrackerViewModel().state.value.screen)
    }

    @Test
    fun replayKeepsSelectedVictoryMode() {
        val viewModel = ScoreTrackerViewModel()
        viewModel.addPlayer("Alice")
        viewModel.addPlayer("Bob")
        viewModel.setVictoryMode(VictoryMode.FEWEST_POINTS)
        viewModel.startGame()
        viewModel.finishGame()

        viewModel.replay()

        assertEquals(VictoryMode.FEWEST_POINTS, viewModel.state.value.victoryMode)
    }
}
