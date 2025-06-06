package com.example.mancala

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.Assert.*
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
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
            val expectedBoard = listOf(4,4,4,4,4,4,  0,  4,4,4,4,4,4,  0)
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
            val expectedAfter = listOf( 0,5,5,5,5,4,  0,  4,4,4,4,4,4,  0)
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
        val board = listOf(0,0,0,0,0,1,  0,  0,0,0,0,0,0,  0)
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
        val expected = listOf(0,0,0,0,0,0,  1,  0,0,0,0,0,0,  0)
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
        val boardBefore = listOf(0,1,0,0,0,0,  0,  0,0,0,3,0,0,  0)
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
        val expectedAfter = listOf(0,0,0,0,0,0,  4,  0,0,0,0,0,0,  0)
        assertEquals(expectedAfter, viewModel.marbles.value)
        // ensure player score increased by 4
        assertEquals(4, viewModel.playerScore.value)
        assertEquals(0, viewModel.computerScore.value)
        // Turn switches to computer
        assertEquals(1, viewModel.currentPlayer.value)
        // Move should have completed
        assertFalse(viewModel.moveInProgress.value)
    }

    @Test
    fun `tapping empty hole does nothing`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        val board = listOf(0,4,4,4,4,4,  0,  4,4,4,4,4,4,  0)
        viewModel.setMarbles(board)
        viewModel.clearScores()
        viewModel.clearCurrentPlayer()
        viewModel.moveMarbleEvent.test {
            viewModel.move(0)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // State should remain unchanged
        assertEquals(board, viewModel.marbles.value)
        assertEquals(0, viewModel.playerScore.value)
        assertEquals(0, viewModel.computerScore.value)
        assertEquals(0, viewModel.currentPlayer.value)
        assertFalse(viewModel.moveInProgress.value)
    }

    @Test
    fun `computer move distributes marbles without capture or extra turn`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        val custom = listOf(1,0,0,0,0,0,  0  ,0,3,0,0,0,0,  0)
        viewModel.setMarbles(custom)
        viewModel.clearScores()
        viewModel.forceComputerTurn()
        // expected: (8 -> 9), (8 -> 10), (8 -> 11)
        viewModel.moveMarbleEvent.test {
            viewModel.move(hole = 0) // hole should be ignored since currentPlayer = 1
            advanceUntilIdle()
            assertEquals(8 to 9, awaitItem())
            assertEquals(8 to 10, awaitItem())
            assertEquals(8 to 11, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // after: hole 8: 0 | hole9: 1 | hole10: 1 | hole11: 1
        val expected = listOf(1,0,0,0,0,0,  0,  0,0,1,1,1,0,  0)
        assertEquals(expected, viewModel.marbles.value)
        // No captures, so no score change
        assertEquals(0, viewModel.playerScore.value)
        assertEquals(0, viewModel.computerScore.value)
        // Turn should switch back to player
        assertEquals(0, viewModel.currentPlayer.value)
        assertFalse(viewModel.moveInProgress.value)
    }

    @Test
    fun `capture on computer side moves marbles and switches turns`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        // computer hole8 has 1 marble, hole9 is empty, opposite for 9 is 3
        // 2 marbles in hole3 so capture is possible
        val boardBefore = listOf( 0,0,0,2,0,0,  0,  0,1,0,0,0,0,  0)
        viewModel.setMarbles(boardBefore)
        viewModel.clearScores()
        viewModel.forceComputerTurn()
        // Collect the single capture animation: (9 -> 13)
        viewModel.playerCaptureEvent.test {
            viewModel.move(hole = -1) // hole arg is ignored for computer
            advanceUntilIdle()
            // Expect computer capture emit from holeToUpdate=9 to store13
            assertEquals(9 to 13, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // After capture: hole8→0, hole9→0, hole3→0, store13→(1+2)=3
        val expectedAfter = listOf(0,0,0,0,0,0,  0  ,0,0,0,0,0,0, 3)
        assertEquals(expectedAfter, viewModel.marbles.value)
        // Score should reflect captured 3 marbles for computer
        assertEquals(0, viewModel.playerScore.value)
        assertEquals(3, viewModel.computerScore.value)
        // Turn switches to player
        assertEquals(0, viewModel.currentPlayer.value)
        assertFalse(viewModel.moveInProgress.value)
    }

    @Test
    fun `win when player side scoops computer side and emits win`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        val boardBefore = listOf(0,0,0,0,0,  1  ,0,2,0,0,1,0,0,  0)
        viewModel.setMarbles(boardBefore)
        viewModel.clearScores()
        viewModel.clearCurrentPlayer()

        viewModel.moveMarbleEvent.test {
            viewModel.move(hole = 5)
            advanceUntilIdle()
            assertEquals(5 to 6, awaitItem())
            assertEquals(7 to 13, awaitItem())
            assertEquals(7 to 13, awaitItem())
            assertEquals(10 to 13, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.winEvent.test {
            assertEquals(1, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        val expectedAfter = listOf(0,0,0,0,0,0,  1,  0,0,0,0,0,0,  3)
        assertEquals(expectedAfter, viewModel.marbles.value)
        assertEquals(1, viewModel.playerScore.value)
        assertEquals(3, viewModel.computerScore.value)
    }

    // FIXME
    @Test
    fun `last marble lands in computer store and computer wins no infinite loop`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = GameViewModel(ioDispatcher = testDispatcher)
        val board = listOf(2,2,2,2,2,2,  0,  0,0,0,0,0,1,  0 )
        viewModel.setMarbles(board)
        viewModel.clearScores()
        viewModel.forceComputerTurn()
        viewModel.moveMarbleEvent.test {
            viewModel.move(hole = 0)
            advanceUntilIdle()
            assertEquals(12 to 13, awaitItem())
            assertEquals(0 to 6, awaitItem())
            assertEquals(0 to 6, awaitItem())
            assertEquals(1 to 6, awaitItem())
            assertEquals(1 to 6, awaitItem())
            assertEquals(2 to 6, awaitItem())
            assertEquals(2 to 6, awaitItem())
            assertEquals(3 to 6, awaitItem())
            assertEquals(3 to 6, awaitItem())
            assertEquals(4 to 6, awaitItem())
            assertEquals(4 to 6, awaitItem())
            assertEquals(5 to 6, awaitItem())
            assertEquals(5 to 6, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        val expected =listOf(0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1)
        assertEquals(expected, viewModel.marbles.value)
        assertEquals(12, viewModel.playerScore.value)
        assertEquals(1, viewModel.computerScore.value)
        assertEquals(1, viewModel.currentPlayer.value)
        assertFalse(viewModel.moveInProgress.value)
    }
}