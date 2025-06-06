package com.example.mancala

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.Assert.*
import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
/**
 * Testing class for the GameViewModel class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
        @Test
        fun `initial state is correct`() = runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val viewModel = GameViewModel(ioDispatcher = testDispatcher)
            val expectedBoard = listOf(4,4,4,4,4,4, 0, 4,4,4,4,4,4, 0)
            assertEquals(expectedBoard, viewModel.marbles.value)
            // For now, the player always goes first TODO implement first move choice
            assertEquals(0, viewModel.currentPlayer.value)
            // Scores start at zero
            assertEquals(0, viewModel.playerScore.value)
            assertEquals(0, viewModel.computerScore.value)
            // No move in progress
            assertFalse(viewModel.moveInProgress.value)
        }

        @Test
        fun `move distributes marbles without capture or extra turn`() = runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val viewModel = GameViewModel(ioDispatcher = testDispatcher)
            // open a turbine test
            viewModel.moveMarbleEvent.test {
                viewModel.move(0)
                // In a runTest context this call moves all pending coroutine work to completion.
                // In other words, it “fast-forwards” through any launch { } and delay() calls inside move(0)
                // and adds all of the emits from move(0) to a turbine queue. awaitItem() pulls these emits from
                // the queue and then they are checked one by one
                advanceUntilIdle()
                assertEquals(0 to 1, awaitItem())
                assertEquals(0 to 2, awaitItem())
                assertEquals(0 to 3, awaitItem())
                assertEquals(0 to 4, awaitItem())
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            // Final board state
            val expectedAfter = listOf( 0,5,5,5,5,4,0,4,4,4,4,4,4,0)
            assertEquals(expectedAfter, viewModel.marbles.value)
            // No score change
            assertEquals(0, viewModel.playerScore.value)
            assertEquals(0, viewModel.computerScore.value)
            // Turn switches to computer
            assertEquals(1, viewModel.currentPlayer.value)
            // Move should have completed
            assertFalse(viewModel.moveInProgress.value)
        }

    @Test
    fun `move ending in player store grants extra turn`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        // Set up a custom board
        val board = listOf(0,0,0,0,0,1, 0, 0,0,0,0,0,0, 0)
        viewModel.setMarbles(board)
        viewModel.clearScores()
        viewModel.clearCurrentPlayer()
        // check that there is only one emission
        viewModel.moveMarbleEvent.test {
            viewModel.move(5)
            advanceUntilIdle()
            assertEquals(5 to 6, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // the marble at hole 5 should now be iin hole 6 and all other holes should be empty
        val expected = listOf(0,0,0,0,0,0, 1, 0,0,0,0,0,0, 0)
        assertEquals(expected, viewModel.marbles.value)
        // Player score incremented by 1
        assertEquals(1, viewModel.playerScore.value)
        assertEquals(0, viewModel.computerScore.value)
        // currentPlayer remains 0 (extra turn)
        assertEquals(0, viewModel.currentPlayer.value)
        // Move should have completed
        assertFalse(viewModel.moveInProgress.value)
    }

    @Test
    fun `move results in capture on player side`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        // create a capture scenario
        val boardBefore = listOf(0,1,0,0,0,0, 0, 0,0,0,3,0,0, 0)
        viewModel.setMarbles(boardBefore)
        viewModel.clearScores()
        viewModel.clearCurrentPlayer()
        // there should be exactly one capture emission: (2→10) indicates we captured from 10.
        // In our code, we emit (landedPit to oppositePit) before updating state,
        // so we expect (2 to 10)
        viewModel.playerCaptureEvent.test {
            viewModel.move(1)
            advanceUntilIdle()
            assertEquals(2 to 6, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // After capture
        val expectedAfter = listOf(0,0,0,0,0,0,4,0,0,0,0,0,0,0)
        assertEquals(expectedAfter, viewModel.marbles.value)
        // ensure player score increased by 4
        assertEquals(4, viewModel.playerScore.value)
        assertEquals(0, viewModel.computerScore.value)
        // Turn switches to computer
        assertEquals(1, viewModel.currentPlayer.value)
        // Move should have completed
        assertFalse(viewModel.moveInProgress.value)
    }
}