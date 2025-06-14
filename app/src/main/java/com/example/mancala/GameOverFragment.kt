package com.example.mancala

import android.os.Bundle
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
        val tv = view.findViewById<TextView>(R.id.tvMessage)
        tv.text = if (winner == 0) "Player Wins!" else "Computer Wins!"

        view.findViewById<Button>(R.id.btnPlayAgain).setOnClickListener {
            findNavController().navigate(R.id.action_gameOverFragment_to_gameFragment)
        }

        view.findViewById<Button>(R.id.btnHome).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

}