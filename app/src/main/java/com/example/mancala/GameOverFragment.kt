/**
Author: Julia Duffy
Last Edited: 6/20/2025
 */
package com.example.mancala

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * End of game screen displaying who won, the final scores, and two buttons- one
 * to play again (on the same difficulty) amd one to go back to the home screen
 */
class GameOverFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game_over, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val difficulty = requireArguments().getString("difficulty", "easy")
        val playerScore = requireArguments().getInt("playerScore", -1)
        val computerScore = requireArguments().getInt("computerScore", -1)
        val tv = view.findViewById<TextView>(R.id.tvMessage)
        tv.text = if (playerScore > computerScore) "Player Wins!" else if (computerScore > playerScore) "Computer Wins!" else "It's a Tie!"
        val s = view.findViewById<TextView>(R.id.scoreMessage)
        Log.d("GameOverFragment", "args = ${arguments}")
        s.text = "Bot: $computerScore                You: $playerScore"

        view.findViewById<Button>(R.id.btnPlayAgain).setOnClickListener {
            val args = Bundle().apply { putString("difficulty", difficulty) }
            findNavController().navigate(R.id.action_gameOverFragment_to_gameFragment, args)
        }

        view.findViewById<Button>(R.id.btnHome).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

}