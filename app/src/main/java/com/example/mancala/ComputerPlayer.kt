package com.example.mancala

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

public class ComputerPlayer {

    companion object {
        @JvmStatic
        fun easy(): Int {
            return kotlin.random.Random.nextInt(7, 13) // until excludes the upper bound
        }

        fun medium(boardState: List<Int>): Int {
            var bestMove = -1
            var bestScore = -1

            // First check captures
            for (i in 7..12) {
                if (boardState[i] == 0) continue
                val marblesCaptured = marblesCaptured(boardState, i)
                if (marblesCaptured > 1 && marblesCaptured > bestScore) {
                    bestScore = marblesCaptured
                    bestMove = i
                }
            }

            if (bestMove != -1) return bestMove

            // Then check extra turn
            for (i in 7..12) {
                if (boardState[i] == 0) continue
                if (extraTurnAwarded(boardState, i)) {
                    val moveResult = totalMarblesMoved(boardState, i)
                    if (moveResult > bestScore) {
                        bestScore = moveResult
                        bestMove = i
                    }
                }
            }
            if (bestMove != -1) return bestMove

            // Fallback to number of marbles moved
            for (i in 7..12) {
                val marbles = boardState[i]
                if (marbles == 0) continue
                var score = marbles

                if (i + marbles >= 13) {
                    score += 10 // bonus for landing in store
                }
                if (score > bestScore) {
                    bestScore = score
                    bestMove = i
                }
            }
            return bestMove
        }

        private fun marblesCaptured(boardState: List<Int>, startingHole: Int): Int {
            val pits = boardState.toMutableList()
            val marbles = pits[startingHole]
            if (marbles == 0) return 0

            pits[startingHole] = 0
            var currentIndex = startingHole

            var remaining = marbles
            while (remaining > 0) {
                currentIndex = (currentIndex + 1) % 14
                if (currentIndex != 6) {
                    pits[currentIndex] += 1
                    remaining--
                }
            }

            // Check capture
            val isOnComputerSide = currentIndex in 7..12
            val wasEmptyBefore = boardState[currentIndex] -1 == 0
            val oppositeIndex = 12 - currentIndex

            return if (isOnComputerSide && wasEmptyBefore && boardState[oppositeIndex] > 0) {
                boardState[oppositeIndex] + 1 // captured + final marble
            } else {
                0
            }
        }

        private fun extraTurnAwarded(boardState: List<Int>, startingHole: Int): Boolean {
            return computeLandingHole(boardState, startingHole) == 13
        }

        private fun totalMarblesMoved(boardState: List<Int>, startingHole: Int): Int {
            return boardState[startingHole]
        }

        private fun computeLandingHole(boardState: List<Int>, startingHole: Int): Int {
            val marbles = boardState[startingHole]
            var current = startingHole
            var toDrop = marbles

            while (toDrop > 0) {
                current = (current + 1) % 14
                if (current == 6) continue // skip player's store
                toDrop--
            }

            return current
        }
        //////////////////////////////////////////////////////////////////////////////////////////

        // TODO implement hard algorithm (recursive)
        fun hard(boardState: List<Int>): Int {
            return minimax(boardState, 1, 5).second
        }

        // return best score, best move
        private fun minimax(boardState : List<Int>, currentPlayer : Int, depth: Int) : Pair<Int, Int> {

//        If the game is over (one side has no marbles) or the depth <= 0
//        return ai score - player score (maximizes ai score minimizes player score) and the best move

            val playerOutOfMarbles= (0..5).all { boardState[it] == 0 }
            val computerOutOfMarbles = (7..12).all { boardState[it] == 0 }

            // this symbolizes the bottom of the recursive tree
            if(playerOutOfMarbles || computerOutOfMarbles || depth <= 0 )
                return evaluateScore(boardState.toMutableList()) to -1 // placeholder move gets returned here

            // if it is the ai turn
            if(currentPlayer == 1) {
                var bestResult = -10000 to -1
                for(i in 7 .. 12) {
                    if(boardState[i] == 0) continue
                    var newResult = -10000 to -1
                    val newBoard = boardState.toMutableList()
                    // perform returns true if player gets extra move
                    newResult = if (performMove(newBoard, i, currentPlayer)) {
                        minimax(newBoard, currentPlayer, depth -1)
                    }
                    // no extra move
                    else {
                        minimax(newBoard, 0, depth -1)
                    }
                    if (newResult.first > bestResult.first)
                        bestResult = newResult.first to i
                }
                return bestResult
            }
            else {
                var bestResult = 10000 to -1
                for(i in 0 .. 5) {
                    if(boardState[i] == 0) continue
                    var newResult = -10000 to -1
                    val newBoard = boardState.toMutableList()
                    // perform returns true if player gets extra move
                    newResult = if (performMove(newBoard, i, currentPlayer)) {
                        minimax(newBoard, currentPlayer, depth -1) // don't decrement depth here
                    }
                    else {
                        minimax(newBoard, 1, depth -1)
                    }
                    if (newResult.first < bestResult.first)
                        bestResult = newResult.first to i
                }
                return bestResult
            }
        }

        private fun performMove(boardCopy: MutableList<Int>, hole: Int, currentPlayer: Int) : Boolean {

            var marbleCount = boardCopy[hole]
            var holeToUpdate = hole

            // MOVE ALL MARBLES EXCEPT THE LAST ONE -----------------------------
            // place in following pits
            while (marbleCount > 1) {
                holeToUpdate = (holeToUpdate + 1) % 14
                // on player turn, skip computer store
                if (currentPlayer == 0 && holeToUpdate == 13) holeToUpdate = 0
                // on computer turn skip player store
                else if (currentPlayer == 1 && holeToUpdate == 6) holeToUpdate = 7
                // increment next hole
                boardCopy[holeToUpdate]++
                // decrement starting hole
                boardCopy[hole]--
                // decrement marble count
                marbleCount--
                // calculate hole to update
            }
            holeToUpdate = (holeToUpdate + 1) % 14

            // MOVE LAST MARBLE AND CALCULATE FINAL SCORES -------------------------
            // on player turn, skip computer store
            if (currentPlayer == 0 && holeToUpdate == 13) holeToUpdate = 0
            // on computer turn skip player store
            if (currentPlayer == 1 && holeToUpdate == 6) holeToUpdate = 7
            // Calculate opposite hole for capture scenarios
            val oppositeHole = 12 - holeToUpdate

            // CASE 1: LAST MARBLE LANDS IN PLAYER STORE (AND IT IS PLAYER'S TURN), EXTRA TURN IS AWARDED
            if (holeToUpdate == 6) {
                // game state
                boardCopy[holeToUpdate]++
                boardCopy[hole]--
                return true
            }
            // CASE 2: LAST MARBLE LANDS IN COMPUTER STORE,(AND IT IS COMPUTER'S TURN), EXTRA TURN IS AWARDED
            else if (holeToUpdate == 13) {
                boardCopy[holeToUpdate]++
                boardCopy[hole]--
                return true
            }
            // CASE 3: PLAYER SIDE CAPTURE, SWITCH TURNS
            else if (holeToUpdate in 0..5 && boardCopy[holeToUpdate] == 0 && boardCopy[oppositeHole] > 0 && currentPlayer == 0) {
                // update game stats
                val capturedCount = 1 + boardCopy[oppositeHole]
                boardCopy[6] += capturedCount
                boardCopy[hole] = 0
                boardCopy[holeToUpdate] = 0
                boardCopy[oppositeHole] = 0
                return false
            }

            // CASE 4: COMPUTER SIDE CAPTURE, SWITCH TURNS
            else if (holeToUpdate in 7..12 && boardCopy[holeToUpdate] == 0 && boardCopy[oppositeHole] > 0 && currentPlayer == 1) {
                // update game stats
                val capturedCount = 1 + boardCopy[oppositeHole]
                boardCopy[13] += capturedCount
                boardCopy[hole] = 0
                boardCopy[holeToUpdate] = 0
                boardCopy[oppositeHole] = 0
                return false
            }

            // CASE 5: NORMAL LAST MARBLE PLACEMENT, SWITCH TURNS
            else {
                Log.d( "ComputerPlayer", "$boardCopy")
                boardCopy[holeToUpdate]++
                boardCopy[hole]--
                return false
            }
        }

        // returns ai score - player score
        private fun evaluateScore(boardState: List<Int>): Int {
            // sum of stones left in each side’s pits
            val playerRemaining   = (0..5).sumOf { boardState[it] }
            val computerRemaining = (7..12).sumOf { boardState[it] }

            // total in each store after the “sweep up”
            val playerTotal   = boardState[6]   + playerRemaining
            val computerTotal = boardState[13] + computerRemaining

            return computerTotal - playerTotal
        }
    }
}