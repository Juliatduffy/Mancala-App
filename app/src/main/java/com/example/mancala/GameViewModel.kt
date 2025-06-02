package com.example.mancala

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

// Store the position of marbles, scores, and game state for persistence
class GameViewModel : ViewModel() {

    val playerScore = MutableStateFlow<Int>(0)
    val computerScore = MutableStateFlow<Int>(0)
    var marbles = MutableStateFlow<List<Int>>(listOf(4,4,4,4,4,4, 4,4,4,4,4,4))
    val isPlaying = MutableStateFlow<Boolean>(false)
    val currentPlayer = MutableStateFlow<Int>(0) // 0 is player 1 is computer
    val gameMode = MutableStateFlow<Int>(0) // 0 is easy 1 is med 2 is hard

    // could add who plays first here eventually
    fun startGame(gameMode: String){

    }

    /* callback for when a player clicks on a hole.
    1. If the hole contains no marbles, do nothing
    2. If the hole does have marbles, add 1 to the next x
    indicies of the list, if x is the number of marbles
    */
    fun playerMove(hole: Int) {
        val marbleCount = marbles.value[hole]
        if (marbleCount == 0)
            return
        val mutableList = marbles.value.toMutableList()
        for(i in 0..marbleCount) {
            var holeToUpdate = hole + i % 12
            mutableList[holeToUpdate] += 1
        }


    }

    fun computerMove(pit: Int, marbleCount: Int) {
        // call method in
    }

    // for debugging
    fun logBoardState(){

    }
}