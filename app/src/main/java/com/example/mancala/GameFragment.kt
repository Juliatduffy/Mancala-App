package com.example.mancala

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.mancala.databinding.FragmentGameBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Front end stuff for the game
class GameFragment : Fragment() {
    private val viewModel: GameViewModel by viewModels()
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSubmitMove.setOnClickListener {
            val text = binding.editPitIndex.text.toString().trim()
            val pitIndex = text.toIntOrNull()
            if (pitIndex == null || pitIndex !in 0..12) {
                Toast.makeText(requireContext(), "Enter 0–12", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Call move(...) with that pit, then log:
            viewModel.move(pitIndex)
            viewModel.logBoardState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
