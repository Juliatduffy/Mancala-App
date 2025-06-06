package com.example.mancala

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


// Store the position of marbles, scores, and game state for persistence
class GameViewModel : ViewModel() {

    private val _playerScore = MutableStateFlow(0)
    private val playerScore: StateFlow<Int> get() = _playerScore

    private val _computerScore = MutableStateFlow(0)
    private val computerScore: StateFlow<Int> get() = _computerScore

    private val _moveMarbleEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    val moveMarbleEvent: SharedFlow<Pair<Int, Int>> get() = _moveMarbleEvent

    private val _playerCaptureEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    val playerCaptureEvent: SharedFlow<Pair<Int,Int>> get() = _playerCaptureEvent

    private val _moveInProgress = MutableStateFlow(false)
    private val moveInProgress: StateFlow<Boolean> get() = _moveInProgress

    private val _marbles = MutableStateFlow<List<Int>>(listOf(4,4,4,4,4,4, 0, 4,4,4,4,4,4, 0))
    private val marbles: StateFlow<List<Int>> get() = _marbles

    private val _currentPlayer = MutableStateFlow(0)
    private val currentPlayer: StateFlow<Int> get() = _currentPlayer

    private val _gameMode = MutableStateFlow(0)
    private val gameMode: StateFlow<Int> get() = _gameMode

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

        val actualHole = if (currentPlayer.value == 1) {
            calculateComputerMove()
        } else {
            hole
        }
        // check, then set moveInProgress to prohibit user interaction
        if (moveInProgress.value) return
        else _moveInProgress.value = true
        // Num marbles in hole that was clicked on
        val initMarbleCount = marbles.value[actualHole]
        // TODO try to remove this check
        if (initMarbleCount == 0) {
            _moveInProgress.value = false
            return
        }
        // Num marbles left to distribute
        var currMarbleCount = initMarbleCount
        // Mutable copy of list of marble counts
        val marblesCopy = marbles.value.toMutableList()
        // Hole we are currently updating
        var holeToUpdate = (actualHole + 1) % 14

        viewModelScope.launch {
            // play single marble animations
            while (currMarbleCount > 1) {
                // on player turn, skip computer store
                if (currentPlayer.value == 0 && holeToUpdate == 13) holeToUpdate = 0
                // on computer turn skip player store
                else if (currentPlayer.value == 1 && holeToUpdate == 6) holeToUpdate = 7

                // signal animation to move marble from one hole to another
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // update player score
                if (holeToUpdate == 6) _playerScore.value++
                // update computer score
                else if (holeToUpdate == 13) _computerScore.value++

                // increment next hole
                marblesCopy[holeToUpdate]++
                // decrement starting hole
                marblesCopy[actualHole]--
                // decrement marble count
                currMarbleCount--
                // update game stats
                _marbles.value = marblesCopy.toList()
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
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // game state
                marblesCopy[actualHole]--
                marblesCopy[holeToUpdate]++
                _marbles.value = marblesCopy.toList()
                _playerScore.value += 1

                // extra turn awarded (don't switch currPlayer)
                _moveInProgress.value = false
                return@launch
            }
            // update computer store WE KNOW it is computer turn bc of above statement
            else if (holeToUpdate == 13) {
                // animation
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // game state
                marblesCopy[actualHole]--
                marblesCopy[holeToUpdate]++
                _marbles.value = marblesCopy.toList()
                _computerScore.value += 1

                // extra turn awarded (don't switch currPlayer)
                _moveInProgress.value = false
                return@launch
            }

            // Check if it is a capture
            else if (holeToUpdate in 0..5 && marblesCopy[holeToUpdate] == 0 && marblesCopy[12 - holeToUpdate] > 0 && currentPlayer.value == 0) {
                val oppositeHole =  12 - holeToUpdate
                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                _playerScore.value += capturedCount
                marblesCopy[6] += capturedCount
                marblesCopy[actualHole] = 0
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                _marbles.value = marblesCopy

                // switch to next player
                _currentPlayer.value = 1
                _moveInProgress.value = false
                return@launch
            }

            else if (holeToUpdate in 7..12 && marblesCopy[holeToUpdate] == 0 && marblesCopy[12 - holeToUpdate] > 0 && currentPlayer.value == 1) {
                val oppositeHole =  12 - holeToUpdate

                // if opposite hole has marbles, play capture animation
                if (marblesCopy[oppositeHole] > 0) {
                    _playerCaptureEvent.emit(holeToUpdate to oppositeHole)
                }

                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                _computerScore.value += capturedCount
                marblesCopy[13]+= capturedCount
                marblesCopy[actualHole] = 0
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                _marbles.value = marblesCopy

                // switch to next player
                _currentPlayer.value = 0
                _moveInProgress.value = false
                return@launch
            }
            else {
                // animation
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // update game state
                if (currentPlayer.value == 0 && holeToUpdate == 6) _playerScore.value++
                if (currentPlayer.value == 1 && holeToUpdate == 13) _computerScore.value++
                marblesCopy[actualHole]--
                marblesCopy[holeToUpdate]++
                _marbles.value = marblesCopy.toList()

                // switch to next player
                if (currentPlayer.value == 1) _currentPlayer.value = 0
                else _currentPlayer.value = 1
                _moveInProgress.value = false
                return@launch
            }
        }
    }

    // TODO make sure we can't cause an infinite loop here
    private fun calculateComputerMove() : Int {
        val move = when (gameMode.value) {
            0 -> ComputerPlayer.easy(marbles.value)
            1 -> 7
            2 -> 7
            else -> 7
        }
        return if (marbles.value[move] == 0)
            calculateComputerMove()
        else
            move
    }

    fun logBoardState(tag: String = "GameViewModel") {
        viewModelScope.launch {
            // delay before logging
            delay(1000L)
            val board = marbles.value
            val pScore = playerScore.value
            val cScore = computerScore.value
            val turn   = if (currentPlayer.value == 0) "PLAYER" else "COMPUTER"
            Log.d(tag, "Board: $board | P=$pScore | C=$cScore | Turn=$turn")
        }
    }
}