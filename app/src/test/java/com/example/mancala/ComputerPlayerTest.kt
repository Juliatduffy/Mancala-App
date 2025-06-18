package com.example.mancala

import org.junit.Test
import org.junit.Assert.*

class ComputerPlayerTest {

    @Test
    fun `marblesCaptured returns correct count when capture is possible`() {
        val board = listOf(0, 0, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)
        val captured = ComputerPlayer.marblesCaptured(board, 8)

        // board overview:
        //   0 0 0 0 1 0
        // 0             0
        //   0 0 0 2 0 0
        // check: moving hole 8 results in the capture of 3 marbles

        assertEquals( 3, captured)
    }

    @Test
    fun `marblesCaptured returns zero when starting pit is empty`() {
        val board = List(14) { 0 }
        val captured = ComputerPlayer.marblesCaptured(board, 5)

        // board overview:
        //   0 0 0 0 0 0
        // 0             0
        //   0 0 0 0 0 0
        // check: moving hole 0 results in no capture

        assertEquals(0, captured)
    }

    @Test
    fun `marblesCaptured returns zero when no capture scenario`() {
        val board = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)
        val captured = ComputerPlayer.marblesCaptured(board, 8)

        // board overview:
        //   0 0 0 0 1 0
        // 0             0
        //   0 0 0 0 0 0
        // check: opposite pit empty results in no capture

        assertEquals(0, captured)
    }

    @Test
    fun `extraTurnAwarded detects true when landing in computer store`() {
        val board = MutableList(14) { 0 }.also { it[12] = 1 }
        val extra = ComputerPlayer.extraTurnAwarded(board, 12)

        // board overview:
        //   1 0 0 0 0 0
        // 0             0
        //   0 0 0 0 0 0
        // check: moving hole 12 lands in computer store results in extra turn

        assertTrue(extra)
    }

    @Test
    fun `extraTurnAwarded detects false when not landing in store`() {
        val board = MutableList(14) { 0 }.also { it[7] = 1 }
        val extra = ComputerPlayer.extraTurnAwarded(board, 7)

        // board overview:
        //   0 0 0 0 0 1
        // 0             0
        //   0 0 0 0 0 0
        // check: moving hole 7 lands in pit 8 results in no extra turn

        assertFalse(extra)
    }

    @Test
    fun `totalMarblesMoved returns the correct pit count`() {
        val board = MutableList(14) { 0 }.also { it[10] = 5 }
        val moved = ComputerPlayer.totalMarblesMoved(board, 10)

        // board overview:
        //   0 0 5 0 0 0
        // 0             0
        //   0 0 0 0 0 0
        // check: pit 10 contains 5 marbles

        assertEquals(5, moved)
    }

    @Test
    fun `computeLandingHole handles skips and wrapping correctly`() {
        val board = MutableList(14) { 0 }.also { it[11] = 4 }
        val landing = ComputerPlayer.computeLandingHole(board, 11)

        // board overview:
        //   0 4 0 0 0 0
        // 0             0
        //   0 0 0 0 0 0
        // check: 11→12→13(skip? no skip on comp turn)→0→1 → lands at 1

        assertEquals(1, landing)
    }

    @Test
    fun `computeLandingHole handles skips and wrapping correctly full wrap around`() {
        val board = MutableList(14) { 0 }.also { it[12] = 15 }
        val landing = ComputerPlayer.computeLandingHole(board,  12)

        // board overview:
        //   15 0 0 0 0 0
        // 0             0
        //   0 0 0 0 0 9
        // check: 11→12→13(skip? no skip on comp turn)→0→1 → lands at 1

        assertEquals(0, landing)
    }

    @Test
    fun `medium picks a capture over everything else`() {
        val board = listOf(0, 0, 0, 2, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0)
        val move = ComputerPlayer.medium(board)

        // board overview:
        //   0 1 1 0 1 0
        // 0             0
        //   0 0 0 2 0 0
        // check: medium player chooses to capture

        assertEquals(8, move)
    }

    @Test
    fun `medium picks an extra-turn move when no captures exist`() {
        val board = listOf(0,1,0,0,1,0,0,0,2,3,0,0,1,0)
        val move = ComputerPlayer.medium(board)

        // board overview:
        //   1 0 0 3 2 0
        // 0             0
        //   0 1 0 0 1 0
        // check: only pit 12 gives an extra turn

        assertEquals(12, move)
    }

    @Test
    fun `medium falls back to highest-marble move with store bonus`() {
        val board = MutableList(14) { 0 }.also {
            it[8] = 2
            it[9] = 3
        }
        val move = ComputerPlayer.medium(board)

        // board overview:
        //   0 0 0 3 2 0
        // 0             0
        //   0 0 0 0 0 0
        // check: pit 9 has 3 marbles and bonus → best fallback

        assertEquals(9, move)
    }

    ////////////////////////////////////// HARD MODE TESTS ////////////////////////////////////////////////

    // PERFORM MOVE TESTS

    @Test
    fun `performMove distributes marbles without capture or extra turn`() {
        val board = MutableList(14) { 0 }.also { it[7] = 3 }
        val copy  = board.toMutableList()
        val extra = ComputerPlayer.performMove(copy, 7, currentPlayer = 1)

        // board overview (before move):
        //   0 0 0 0 0 3
        // 0             0
        //   0 0 0 0 0 0
        // check that after the move, 7 == 0, 8 == 1, 9 == 1, 10 == 1

        assertFalse(extra)
        assertEquals( listOf(0,0,0,0,0,0, 0, 0,1,1,1,0,0,0), copy )
    }

    @Test
    fun `performMove returns true when last marble lands in computer store`() {
        val board = MutableList(14) { 0 }.also { it[12] = 1 }
        val copy  = board.toMutableList()
        val extra = ComputerPlayer.performMove(copy, 12, currentPlayer = 1)

        // board overview (before move):
        //   1 0 0 0 0 0
        // 0             0
        //   0 0 0 0 0 0

        assertTrue(extra)
        assertEquals( listOf(0,0,0,0,0,0,0,0,0,0,0,0,0,1), copy )
    }

    @Test
    fun `performMove captures stones on computer side and returns false`() {
        val board = MutableList(14) { 0 }.also {
            it[8] = 1
            it[3] = 2
        }
        val copy  = board.toMutableList()
        val extra = ComputerPlayer.performMove(copy, 8, currentPlayer = 1)

        // board overview (before move):
        //   0 0 0 0 1 0
        // 0             0
        //   0 0 0 2 0 0
        // check: after capture, pits 3,8,9 go to 0; store13 gets 3
        assertFalse(extra)
        assertEquals( listOf(0,0,0,0,0,0, 0, 0,0,0,0,0,0,3), copy )
    }


    // MINIMAX TESTS

    @Test
    fun `minimax returns terminal node on depth zero`() {
        val board = List(14) { 1 }      // all pits & stores = 1
        val (score, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 0)

        // board overview:
        //   1 1 1 1 1 1
        // 1             1
        //   1 1 1 1 1 1
        // check: depth=0 → immediate terminal, move = -1

        assertEquals(-1, move)
    }

    @Test
    fun `minimax picks capture move at depth one`() {
        val board = listOf(0,0,0,2,0,0,0,0,1,0,0,1,0,0)
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 1)

        // board overview:
        //   0 1 0 0 1 0
        // 0             0
        //   0 0 0 2 0 0
        // check: pit 9→10 lands on empty + opposite[2]=0? No capture.
        //       pit 8→9 lands on empty + opposite[3]=2 → capture. choose 8

        assertEquals(8, move)
    }

    @Test
    fun `minimax picks extra-turn move at depth one when no capture`() {
        val board = MutableList(14) { 0 }.also {
            it[12] = 1
            it[4] = 1
        }
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 1)

        // board overview:
        //   1 0 0 0 0 0
        // 0             0
        //   0 0 0 0 1 0
        // check: only move 12 → extra turn → picks 12

        assertEquals(12, move)
    }

    @Test
    fun `minimax chooses capture`() {
        val board = MutableList(14) { 0 }.also {
            it[8] = 2
            it[9] = 4
            it[0]= 1
            it [5] = 1
        }
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 1)

        // board overview:
        //   0 0 0 3 2 0
        // 0             0
        //   1 0 0 0 0 1

        assertEquals(9, move)
    }

    @Test
    fun `if one side is empty minimax returns -1`() {
        val board = MutableList(14) { 0 }.also { it[7] = 4 }
        assertEquals(-1, ComputerPlayer.hard(board))
    }

    @Test
    fun `evaluateScore computes computer minus player including sweep`() {
        val board = listOf(1, 1, 1, 0, 0, 0, 2,  1, 1, 1, 1, 0, 0, 5)
        val score = ComputerPlayer.evaluateScore(board)

        // board overview:
        //   0 0 1 1 1 1
        // 5             2
        //   1 1 1 0 0 0
        // check: (5+1+1+1+1+0+0) - (2+1+1+1+0+0+0) = 9 - 5 = 4

        assertEquals(4, score)
    }

    @Test
    fun `hard returns -1 when no computer pits are non-empty`() {
        val board = List(14) { idx -> if (idx == 6) 5 else 0 }
        val move = ComputerPlayer.hard(board)

        // board overview:
        //   0 0 0 0 0 0
        // 0             5
        //   0 0 0 0 0 0
        // check: computer out of marbles → immediate terminal

        assertEquals(-1, move)
    }

    @Test
    fun `hard picks the only available move on a trivial board`() {
        val board = listOf(0, 1, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 1)
        val move = ComputerPlayer.hard(board)

        // board overview:
        //   0 0 0 0 0 3
        // 0             0
        //   0 1 0 0 0 0
        // check: only pit 7 is non-empty

        assertEquals(7, move)
    }
    @Test
    fun `minimax picks the huge capture over smaller moves at depth 2`() {
        val board = listOf(0,3,0,0,1,0,0,25,2,3,0,3,0,0)
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 2)

        // board overview:
        //   0 3 0 3 2 25
        // 0              0
        //   0 3 0 0 1 0
        // check: select pit 7 to avoid huge player capture next turn

        assertEquals(7, move)
    }
    ////////////////////////////////////// ADDITIONAL MINIMAX TESTS //////////////////////////////////////

    @Test
    fun `minimax prefers larger capture over smaller capture at depth one`() {

        val board = listOf(0, 0, 3, 0, 2, 0, 0, 1, 0, 1, 0, 0, 0, 0)
        // board overview:
        //   0 0 0 1 0 1
        // 0             0
        //   0 0 3 0 2 0
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 1)
        assertEquals(9, move)
    }

    @Test
    fun `minimax picks extra move then capture instead of greedy capture immediately`() {

        val board = listOf(0,0,4,0,1,0,0,1,2,0,3,0,0,0)
        // board overview:
        //   0 0 3 0 2 1
        // 0             0
        //   0 0 4 0 1 0
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 3)
        assertEquals(10, move)
    }

    @Test
    fun `minimax returns -1 when computer side has no stones`() {
        val board = listOf(1,1,1,1,1,1,3,0,0,0,0,0,0,3)
        // board overview:
        //   0 0 0 0 0 0
        // 3             3
        //   1 1 1 1 1 1
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 3)
        assertEquals(-1, move)
    }

    @Test
    fun `minimax picks least-worst move when all give opponent captures at depth two`() {
        val board = listOf(0,1,0,1,0,0,0,0,3,0,2,0,0,0)
        // board overview:
        //   0 0 2 0 3 0
        // 0             0
        //   0 1 0 1 0 0
        val (_, move) = ComputerPlayer.minimax(board, currentPlayer = 1, depth = 2)
        assertEquals(8, move)
    }

}
