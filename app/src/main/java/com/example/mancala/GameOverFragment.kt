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

        val winner = requireArguments().getInt("winner", 0)
        val difficulty = requireArguments().getString("difficulty", "easy")
        val playerScore = requireArguments().getInt("playerScore", -1)
        val computerScore = requireArguments().getInt("computerScore", -1)
        val tv = view.findViewById<TextView>(R.id.tvMessage)
        tv.text = if (winner == 0) "Player Wins!" else "Computer Wins!"
        val s = view.findViewById<TextView>(R.id.scoreMessage)
        Log.d("GameOverFragment", "args = ${arguments}")
        s.text = "$computerScore - $playerScore"

        view.findViewById<Button>(R.id.btnPlayAgain).setOnClickListener {
            val args = Bundle().apply { putString("difficulty", difficulty) }
            findNavController().navigate(R.id.action_gameOverFragment_to_gameFragment, args)
        }

        view.findViewById<Button>(R.id.btnHome).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

}