package com.example.mancala

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.mancala.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
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
            val args = Bundle().apply { putString("difficulty", "easy") }
            nav.navigate(R.id.action_home_to_game, args)
        }
        binding.mediumButton.setOnClickListener {
            val args = Bundle().apply { putString("difficulty", "medium") }
            nav.navigate(R.id.action_home_to_game, args)
        }
        binding.hardButton.setOnClickListener {
            val args = Bundle().apply { putString("difficulty", "hard") }
            nav.navigate(R.id.action_home_to_game, args)
        }

        // TODO: hook up settings/help ImageButtons:
        binding.settingsButton.setOnClickListener { }
        binding.helpButton.setOnClickListener { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
