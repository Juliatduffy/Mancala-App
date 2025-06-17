package com.example.mancala

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
            val wasEmptyBefore = boardState[currentIndex] == 0
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
            return kotlin.random.Random.nextInt(7, 13)
        }


//        pseudocode for hard player:
//
//        minimax(boardState, currrentPlayer)
//
//        initialize the best move as -1 or NONE
//
//        If the game is over (one side has no marbles) or the depth <= 0
//            return ai score - player score (maximizes ai score minimizes player score) and the best move
//
//        IF: if it is the AI's turn:
//            - initialize the best score as -inf
//            - foreach possible move:
//                - copy the board
//                - perform a move and see if the ai gets another turn
//
//            - if ai gets another turn:
//                  call minimax again passing in the new board state with the ai as
//                  the current player and the same depth
//                  (we don't increment the depth until we switch turns because we
//                  don't want to penalize the ai for doing something good (getting an extra turn))
//
//            - if no extra turn
//                call minimax again with the player as current player and decrement the depth
//
//            If the resulting score is better than the current best:
//                - update the best score and best move
//        ELSE: If it's the PLAYER's turn:
//            - initialize the best score as inf (very bad from the
//              AI's perspective, anything worse than inf is better)
//            - initialize best move as -1 or NONE
//                - foreach move the PLAYER can make:
//                    - copy the board
//                    - perform each move
//                    if results in an extra player turn, call minimax again with the same player and depth
//
//                    else: it doesn't result in an extra turn:
//                        call minimax again with depth decremented and switch to ai player.
//
//        If the resulting score is better (less than for the player) than the current best:
//               - update the best score and best move
//
//        After all moves return the best score and best move





    }
}