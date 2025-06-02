package com.example.mancala

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Store the position of marbles, scores, and game state for persistence
class GameViewModel : ViewModel() {

    val playerScore = MutableStateFlow<Int>(0)
    val computerScore = MutableStateFlow<Int>(0)
    val marbles = MutableStateFlow<List<Int>>(listOf(4,4,4,4,4,4, 4,4,4,4,4,4))
    val isPlaying = MutableStateFlow<Boolean>(false)
    val currentPlayer = MutableStateFlow<Int>(0) // 0 is player 1 is computer
    val gameMode = MutableStateFlow<Int>(0) // 0 is easy 1 is med 2 is hard
    private val moveMarbleEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    private val playerCaptureEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    private val moveInProgress = MutableStateFlow(false)

    // could add who plays first here eventually
    fun startGame(gameMode: String){

    }

    /* callback for when a player clicks on a hole.
    1. If the hole contains no marbles, do nothing
    2. If the hole does have marbles, add 1 to the next x
    indices of the list
    3. If hole + x = 7 and x == 1 break from the loop and add 1 to player score
    4. If hole + x is between 1 - 6 and the hole across from hole + x has marbles, capture

    Things that need to be returned by this method:
    */
    fun move(hole: Int) {
        // check, then set moveInProgress to prohibit user interaction
        if (moveInProgress.value) return
        else moveInProgress.value = true
        // Num marbles in hole that was clicked on
        val initMarbleCount = marbles.value[hole]
        // TODO try to remove this check
        if (initMarbleCount == 0) {
            moveInProgress.value = false
            return
        }
        // Num marbles left to distribute
        var currMarbleCount = initMarbleCount
        // Mutable copy of list of marble counts
        val marblesCopy = marbles.value.toMutableList()
        // Hole we are currently updating
        var holeToUpdate = (hole + 1) % 14

        viewModelScope.launch {
            // play single marble animations
            while (currMarbleCount > 1) {
                // on player turn, skip computer store
                if (currentPlayer.value == 0 && holeToUpdate == 13) holeToUpdate = 0
                // on computer turn skip player store
                else if (currentPlayer.value == 1 && holeToUpdate == 6) holeToUpdate = 7

                // signal animation to move marble from one hole to another
                moveMarbleEvent.emit(hole to holeToUpdate)
                // allow time to play the animation
                delay(300)

                // update player score
                if (holeToUpdate == 6) playerScore.value++
                // update computer score
                else if (holeToUpdate == 13) computerScore.value++

                // increment next hole
                marblesCopy[holeToUpdate]++
                // decrement starting hole
                marblesCopy[hole]--
                // decrement marble count
                currMarbleCount--
                // update game stats
                marbles.value = marblesCopy.toList()
                // calculate hole to update
                holeToUpdate = (holeToUpdate + 1) % 14
            }

            // on player turn, skip computer store
            if (currentPlayer.value == 0 && holeToUpdate == 13) holeToUpdate = 0
            // on computer turn skip player store
            if (currentPlayer.value == 1 && holeToUpdate == 6) holeToUpdate = 7

            // if last marble goes in player store, award extra turn and points WE KNOW it is player turn bc of prev statement
            if (holeToUpdate == 6) {

                // animation
                moveMarbleEvent.emit(hole to holeToUpdate)
                delay(300)

                // game state
                marblesCopy[hole]--
                marblesCopy[holeToUpdate]++
                marbles.value = marblesCopy.toList()
                playerScore.value += 1

                // extra turn awarded (don't switch currPlayer)
                moveInProgress.value = false
                return@launch
            }
            // update computer store WE KNOW it is computer turn bc of above statement
            else if (holeToUpdate == 13) {
                // animation
                moveMarbleEvent.emit(hole to holeToUpdate)
                delay(300)

                // game state
                marblesCopy[hole]--
                marblesCopy[holeToUpdate]++
                marbles.value = marblesCopy.toList()
                computerScore.value += 1

                // extra turn awarded (don't switch currPlayer)
                moveInProgress.value = false
                return@launch
            }

            // Check if it is a capture
            else if (holeToUpdate in 0..5 && currentPlayer.value == 0) {
                val oppositeHole =  12 - holeToUpdate

                // if opposite hole has marbles, play capture animation
                if (marblesCopy[oppositeHole] > 0) {
                    playerCaptureEvent.emit(holeToUpdate to oppositeHole)
                    delay(300)
                }

                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                playerScore.value += capturedCount
                marblesCopy[hole]--
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                marbles.value = marblesCopy

                // switch to next player
                currentPlayer.value = 1
                moveInProgress.value = false
                return@launch
            }

            else if (holeToUpdate in 7..12 && currentPlayer.value == 1) {
                val oppositeHole =  12 - holeToUpdate

                // if opposite hole has marbles, play capture animation
                if (marblesCopy[oppositeHole] > 0) {
                    playerCaptureEvent.emit(holeToUpdate to oppositeHole)
                    delay(300)
                }

                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                computerScore.value += capturedCount
                marblesCopy[hole]--
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                marbles.value = marblesCopy

                // switch to next player
                currentPlayer.value = 0
                moveInProgress.value = false
                return@launch
            }
            else {
                // animation
                moveMarbleEvent.emit(hole to holeToUpdate)
                delay(200)

                // update game state
                if (currentPlayer.value == 0 && holeToUpdate == 6) playerScore.value++
                if (currentPlayer.value == 1 && holeToUpdate == 13) computerScore.value++
                marblesCopy[hole]--
                marblesCopy[holeToUpdate]++
                marbles.value = marblesCopy.toList()

                // switch to next player
                if (currentPlayer.value == 1) currentPlayer.value = 0
                else currentPlayer.value = 1
                moveInProgress.value = false
                return@launch
            }
        }
    }

    fun calculateComputerMove() {
        // call method in another class to get the next computer move
    }

    // for debugging
    fun logBoardState(){

    }
}