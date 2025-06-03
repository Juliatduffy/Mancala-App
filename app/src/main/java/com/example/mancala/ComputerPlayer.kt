package com.example.mancala

public class ComputerPlayer {

    companion object {
        @JvmStatic
        fun easy(boardState: List<Int>): Int {
            return kotlin.random.Random.nextInt(7, 12)
        }

    }
    // TODO implement medium algorithm (greedy approach)
    fun medium( boardState : List<Int>): Int {
        return kotlin.random.Random.nextInt(7, 12)
    }
    // TODO implement hard algorithm (recursive)
    fun hard( boardState : List<Int>) : Int {
        return kotlin.random.Random.nextInt(7, 12)
    }
}