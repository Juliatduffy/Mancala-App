package com.example.mancala

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
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


        lifecycleScope.launch {
            viewModel.moveMarbleEvent.collect { (fromPit, toPit) ->
                animateSingleMarbleMove(fromPit, toPit)
            }
        }

        lifecycleScope.launch {
            viewModel.playerCaptureEvent.collect { (landingPit, oppositePit) ->
                animateCapture(landingPit, oppositePit)
            }
        }
    }

    fun animateSingleMarbleMove(fromPit: Int, toPit: Int) {
        Log.d("animation","Move $fromPit to $toPit")
    }

    fun animateCapture(landingPit: Int, oppositePit: Int) {
        Log.d("animation","marble landed: $landingPit Capture: $oppositePit")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
