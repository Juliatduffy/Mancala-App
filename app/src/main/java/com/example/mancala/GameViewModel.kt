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
    fun playerMove(hole: Int) {
        // check, then set moveInProgress to prohibit user interaction
        if (moveInProgress.value) return
        else moveInProgress.value = true
        // Num marbles in hole that was clicked on
        val initMarbleCount = marbles.value[hole]
        // Num marbles left to distribute
        var currMarbleCount = initMarbleCount
        // Mutable copy of list of marble counts
        val marblesCopy = marbles.value.toMutableList()
        // Hole we are currently updating
        var holeToUpdate = (hole + 1) % 14
        // boolean to award extra turns
        var extraTurn = false

        viewModelScope.launch {
            // play single marble animations
            while (currMarbleCount > 1) {
                // skip computer's store
                if (holeToUpdate == 13) holeToUpdate = 0
                // signal animation to move marble from one hole to another
                moveMarbleEvent.emit(hole to holeToUpdate)
                // allow time to play the animation
                delay(300)
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
            if (holeToUpdate == 13) holeToUpdate = 0

            // if last marble goes in player store, award extra turn
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

            // Check if it is a capture
            else if (holeToUpdate in 0..5) {
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

                // switch to the computer's turn
                currentPlayer.value = 1
                moveInProgress.value = false
                return@launch
            }
            else {
                // animation
                moveMarbleEvent.emit(hole to holeToUpdate)
                delay(200)

                // update game state
                marblesCopy[hole]--
                marblesCopy[holeToUpdate]++
                marbles.value = marblesCopy.toList()

                // switch to computer's turn
                currentPlayer.value = 1
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