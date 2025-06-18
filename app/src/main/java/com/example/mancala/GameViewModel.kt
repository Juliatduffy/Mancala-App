package com.example.mancala

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch


// Store the position of marbles, scores, and game state for persistence
class GameViewModel(private val ioDispatcher: CoroutineDispatcher) : ViewModel() {

    private val _playerScore = MutableStateFlow(0)
    val playerScore: StateFlow<Int> = _playerScore.asStateFlow()

    private val _computerScore = MutableStateFlow(0)
    val computerScore: StateFlow<Int> = _computerScore.asStateFlow()

    private val _moveMarbleEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    val moveMarbleEvent: SharedFlow<Pair<Int,Int>> = _moveMarbleEvent.asSharedFlow()

    private val _playerCaptureEvent = MutableSharedFlow<Pair<Int,Int>>(extraBufferCapacity = 1)
    val playerCaptureEvent: SharedFlow<Pair<Int,Int>> get() = _playerCaptureEvent.asSharedFlow()

    private val _winEvent = MutableSharedFlow<Int>(replay = 1)
    val winEvent: SharedFlow<Int> = _winEvent.asSharedFlow()

    private val _endOfGameEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val endOfGameEvent: SharedFlow<Unit> = _endOfGameEvent.asSharedFlow()

    private val _moveInProgress = MutableStateFlow(false)
    val moveInProgress: StateFlow<Boolean> = _moveInProgress.asStateFlow()

    private val _computerTurnEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val computerTurnEvent: SharedFlow<Unit> get() = _computerTurnEvent

    private val _playerTurnEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playerTurnEvent: SharedFlow<Unit> get() = _playerTurnEvent

    private val _marbles = MutableStateFlow(listOf(4,4,4,4,4,4, 0, 4,4,4,4,4,4, 0))
    val marbles: StateFlow<List<Int>> = _marbles.asStateFlow()

    private val _currentPlayer = MutableStateFlow(0)
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    private val _gameMode = MutableStateFlow(0)
     val gameMode: StateFlow<Int> get() = _gameMode

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(ioDispatcher + viewModelJob)
    private val moveMutex = Mutex()
    private var moveCount = 0

    // could add who plays first here eventually
    fun startGame(gameMode: String){
        _gameMode.value = when (gameMode) {
            "easy" -> 0
            "medium" -> 1
            "hard" -> 2
            else -> 0
        }
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
        println("Move($hole)")
        val actualHole = if (currentPlayer.value == 1)  calculateComputerMove() else hole

        // check, then set moveInProgress to prohibit user interaction
        if (moveInProgress.value) return
        else _moveInProgress.value = true

        viewModelScope.launch {
            moveMutex.withLock {

        // Num marbles in hole that was clicked on
        val initMarbleCount = marbles.value[actualHole]

        if (initMarbleCount == 0) {
            _moveInProgress.value = false
            logBoardState()
            return@launch
        }
        // Num marbles left to distribute
        var currMarbleCount = initMarbleCount
        // Mutable copy of list of marble counts
        val marblesCopy = marbles.value.toMutableList()
        // Hole we are currently updating
        var holeToUpdate = (actualHole + 1) % 14

            // play single marble animations, distribute all but the last marble
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

            // Calculate opposite hole for capture scenarios
            val oppositeHole = 12 - holeToUpdate

            // CASE 1: LAST MARBLE LANDS IN PLAYER STORE (AND IT IS PLAYER'S TURN), EXTRA TURN IS AWARDED
            if (holeToUpdate == 6) {

                // animation
                println("moving marble from $actualHole to $holeToUpdate")
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // game state
                marblesCopy[holeToUpdate]++
                marblesCopy[actualHole]--
                _marbles.value = marblesCopy.toList()
                _playerScore.value += 1

                // check for win
                println("Checking for win...")
                checkForWin(marblesCopy)

                // extra turn awarded (don't switch currPlayer)
                _moveInProgress.value = false
                _playerTurnEvent.emit(Unit)
                logBoardState()
                return@launch
            }

            // CASE 2: LAST MARBLE LANDS IN COMPUTER STORE,(AND IT IS COMPUTER'S TURN), EXTRA TURN IS AWARDED
            else if (holeToUpdate == 13) {
                // animation
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // game state
                marblesCopy[holeToUpdate]++
                marblesCopy[actualHole]--
                _marbles.value = marblesCopy.toList()
                _computerScore.value += 1

                checkForWin(marblesCopy)

                // extra turn awarded (don't switch currPlayer)
                _moveInProgress.value = false
                // left game fragment know that computer gets an extra turn
                _computerTurnEvent.emit(Unit)
                logBoardState()
                return@launch
            }

            // CASE 3: PLAYER SIDE CAPTURE, SWITCH TURNS
            else if (holeToUpdate in 0..5 && marblesCopy[holeToUpdate] == 0 && marblesCopy[oppositeHole] > 0 && currentPlayer.value == 0) {

                // animation
                _moveMarbleEvent.emit(actualHole to holeToUpdate)
                _playerCaptureEvent.emit(holeToUpdate to 6)

                // update game stats
                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                _playerScore.value += capturedCount
                marblesCopy[6] += capturedCount
                marblesCopy[actualHole] = 0
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                _marbles.value = marblesCopy.toList()

                // switch to next player
                _currentPlayer.value = 1

                // check for win
                checkForWin(marblesCopy)

                _moveInProgress.value = false

                // let the game fragment know that it is now the computer's turn
                _computerTurnEvent.emit(Unit)
                logBoardState()

                return@launch
            }

            // CASE 4: COMPUTER SIDE CAPTURE, SWITCH TURNS
            else if (holeToUpdate in 7..12 && marblesCopy[holeToUpdate] == 0 && marblesCopy[oppositeHole] > 0 && currentPlayer.value == 1) {
                println("About to capture opposite hole: $oppositeHole  current hole = $holeToUpdate  marblesCopy=$marblesCopy curr player = $currentPlayer.value")
                _moveMarbleEvent.emit(actualHole to holeToUpdate)
                _playerCaptureEvent.emit(holeToUpdate to 13)

                // update game stats
                val capturedCount = 1 + marblesCopy[oppositeHole]
                _computerScore.value += capturedCount
                marblesCopy[13] += capturedCount
                marblesCopy[actualHole] = 0
                marblesCopy[holeToUpdate] = 0
                marblesCopy[oppositeHole] = 0
                _marbles.value = marblesCopy

                // switch to next player
                _currentPlayer.value = 0

                // check for win
                checkForWin(marblesCopy)

                _moveInProgress.value = false
                _playerTurnEvent.emit(Unit)
                logBoardState()

                return@launch
            }

            // CASE 5: NORMAL LAST MARBLE PLACEMENT, SWITCH TURNS
            else {
                // animation
                _moveMarbleEvent.emit(actualHole to holeToUpdate)

                // update game state
                marblesCopy[holeToUpdate]++
                marblesCopy[actualHole]--
                _marbles.value = marblesCopy.toList()

                // switch to next player
                _currentPlayer.value = if (_currentPlayer.value == 1) 0 else 1

                // check for win
                checkForWin(marblesCopy)
                _moveInProgress.value = false

                if (_currentPlayer.value == 1) _computerTurnEvent.emit(Unit)
                else _playerTurnEvent.emit(Unit)
                logBoardState()

                return@launch
            }
        }

        }
    }

    /*
    This method checks to see if there was a winner and if so, moves all remaining marbles on
    the non-empty side of the board to their respective store. A moveMarbleEvent emit is
    sent for each of these moves, so in the ui, all marbles can be animated individually.
    Then, an winEvent emit is sent to the game fragment so that a win message can be displayed.
     */
    private fun checkForWin(marblesCopy : MutableList<Int>) {
        val playerOutOfMarbles= (0..5).all { marblesCopy[it] == 0 }
        val computerOutOfMarbles = (7..12).all { marblesCopy[it] == 0 }

        if (!playerOutOfMarbles && !computerOutOfMarbles)
            return

        viewModelScope.launch {
            _endOfGameEvent.emit(Unit)

            if (playerOutOfMarbles) {
                for (i in 7 .. 12) {
                    for (j in 0 until marblesCopy[i]) {
                        _moveMarbleEvent.emit(i to 13)
                        delay(300)
                        marblesCopy[i]--
                        marblesCopy[13]++
                        _computerScore.value++
                        _marbles.value = marblesCopy.toList()
                    }
                }
            }
            if (computerOutOfMarbles) {
                for (i in 0 .. 5) {
                    for (j in 0 until marblesCopy[i]) {
                        _moveMarbleEvent.emit(i to 6)
                        delay(300)
                        marblesCopy[i]--
                        marblesCopy[6]++
                        _playerScore.value++ // TODO get rid of play and computer score and just reference the marbles array indices 6 and 13
                        _marbles.value = marblesCopy.toList()
                    }
                }
            }
            val winner = if (marblesCopy[6] > marblesCopy[13]) 0 else if (marblesCopy[6] > marblesCopy[13]) 1 else 2
            _winEvent.emit(winner)
        }
    }
    // TODO make sure we can't cause an infinite loop here
    private fun calculateComputerMove() : Int {

        val noPossibleMoves = (7..12).all { marbles.value[it] == 0 }
        if(noPossibleMoves) return 0

        var boardCopy = marbles.value.toMutableList()
        val move = when (gameMode.value) {
            0 -> ComputerPlayer.easy()
            1 -> ComputerPlayer.medium(boardCopy)
            2 -> ComputerPlayer.hard(boardCopy)
            else -> 7
        }
        return if (marbles.value[move] == 0)
            calculateComputerMove()
        else
            move
    }

    private fun logBoardState(tag: String = "GameViewModel") {
        moveCount++
        val b = marbles.value
        val topRow    = b.subList(7, 13).reversed()
            .joinToString(" ") { it.toString().padStart(2, ' ') }
        val bottomRow = b.subList(0, 6)
            .joinToString(" ") { it.toString().padStart(2, ' ') }
        val leftStore  = b[13].toString().padStart(2, ' ')
        val rightStore = b[6].toString().padStart(2, ' ')
        val board = buildString {
            appendLine("    $topRow")
            appendLine("[$leftStore]${" ".repeat(topRow.length + 2)}[$rightStore]")
            append("    $bottomRow")
        }
        Log.d(tag, "----------------Turn $moveCount--------------------")
        Log.d(tag, board)
        Log.d(tag, "---------------------------------------------------")
    }

    // Helpers for testing --------------------------------------------
    fun setMarbles(newBoard: List<Int>) { _marbles.value = newBoard }
    fun clearScores() {
        _playerScore.value = 0
        _computerScore.value = 0
    }
    fun clearCurrentPlayer() { _currentPlayer.value = 0 }
    fun forceComputerTurn() { _currentPlayer.value = 1 }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    // View model factory --------------------------------------------------
    class GameViewModelFactory(
        private val dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GameViewModel(dispatcher) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}