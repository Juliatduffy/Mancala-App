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
    }