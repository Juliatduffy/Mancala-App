/**
Author: Julia Duffy
Last Edited: 6/20/2025
 */
package com.example.mancala

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mancala.databinding.FragmentHomeBinding

/**
 * This is the front-end logic for the home screen
 */
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()


        binding.easyButton.setOnClickListener {
            val args = Bundle().apply {
                putString("difficulty", "easy")
                putString("firstMove", viewModel.firstMove.toString())
            }
            nav.navigate(R.id.action_home_to_game, args)
        }
        binding.mediumButton.setOnClickListener {
            val args = Bundle().apply {
                putString("difficulty", "medium")
                "firstMove" to viewModel.firstMove.toString()
            }
            nav.navigate(R.id.action_home_to_game, args)
        }
        binding.hardButton.setOnClickListener {
            val args = Bundle().apply {
                putString("difficulty", "hard")
                "firstMove" to viewModel.firstMove.toString()
            }
            nav.navigate(R.id.action_home_to_game, args)
        }
        binding.settingsButton.setOnClickListener {
            nav.navigate(R.id.settingsFragment)
        }
        binding.helpButton.setOnClickListener {
            nav.navigate(R.id.howToFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
