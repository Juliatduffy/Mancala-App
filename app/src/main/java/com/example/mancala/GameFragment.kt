package com.example.mancala

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.mancala.databinding.FragmentGameBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.mancala.GameViewModel.GameViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

// Front end stuff for the game
class GameFragment : Fragment() {
    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(Dispatchers.Main)
    }
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private lateinit var holes: List<FrameLayout>
    private val marbleSizeDp = 30

    private sealed class AnimationEvent {
        data class Move(val fromPit: Int, val toPit: Int) : AnimationEvent()
        data class Capture(val landingPit: Int, val storePit: Int) : AnimationEvent()
        data object ComputerTurn : AnimationEvent()
    }

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

        // store references to all of the holes
        holes = listOf(
            binding.pit0,
            binding.pit1,
            binding.pit2,
            binding.pit3,
            binding.pit4,
            binding.pit5,
            binding.pit6,
            binding.pit7,
            binding.pit8,
            binding.pit9,
            binding.pit10,
            binding.pit11,
            binding.pit12,
            binding.pit13
        )

        // populate the board with 4 marbles / hole at the beginning of the game
        redrawAllPits(viewModel.marbles.value)

        // set on-clicks for all holes
        for (i in holes.indices) {
            val container = holes[i]
            // player cannot click computer side
            if (i in 6..13) {
                container.isClickable = false
            }
            // all other holes are clickable and call the vm's move() method
            else {
                container.setOnClickListener {
                    if (viewModel.moveInProgress.value ||
                        viewModel.currentPlayer.value == 1 ||
                        viewModel.marbles.value[i] == 0
                    ) return@setOnClickListener
                    viewModel.move(i)
                }
            }
        }
        val animEvents: Flow<AnimationEvent> = merge(
            viewModel.moveMarbleEvent.map { (from, to) -> AnimationEvent.Move(from, to) },
            viewModel.playerCaptureEvent.map { (landing, store) -> AnimationEvent.Capture(landing, store) },
            viewModel.computerTurnEvent.map {  AnimationEvent.ComputerTurn }
        )

        lifecycleScope.launch {
            animEvents.collect { event ->
                when (event) {
                    is AnimationEvent.Move -> {
                        animateSingleMarbleMove(event.fromPit, event.toPit)
                    }

                    is AnimationEvent.Capture -> {
                        animateSingleMarbleMove(event.landingPit, event.storePit)
                        val opposite = 12 - event.landingPit
                        repeat(holes[opposite].childCount) {
                            animateSingleMarbleMove(opposite, event.storePit)
                        }
                    }
                    AnimationEvent.ComputerTurn -> {
                        delay(1000)
                        viewModel.move(0)
                    }
                }
            }
        }
    }

    private fun redrawAllPits(counts: List<Int>) {
        if (::holes.isInitialized.not() || counts.size < holes.size) return
        val sizePx = (marbleSizeDp * resources.displayMetrics.density).toInt()

        // for each hole in the grid
        for (i in holes.indices) {
            val container = holes[i]
            container.removeAllViews()
            repeat(counts[i]) {
                val marble = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.blue)
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                        gravity = Gravity.CENTER
                    }
                    translationX = Random.nextInt(-40, 40).toFloat()
                    translationY = Random.nextInt(-60, 60).toFloat()
                }
                container.addView(marble)
            }
        }
    }
    // needed help with this from chatgpt
    private suspend fun animateSingleMarbleMove(fromPit: Int, toPit: Int) =
        suspendCancellableCoroutine { cont ->
            val overlay = binding.animationOverlay
            if (fromPit !in holes.indices || toPit !in holes.indices) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

        val overlayLoc = IntArray(2).also { overlay.getLocationOnScreen(it) }
        val fromView = holes[fromPit]
        val fromLoc = IntArray(2).also { fromView.getLocationOnScreen(it) }
        val fromCenterX = fromLoc[0] + fromView.width / 2f
        val fromCenterY = fromLoc[1] + fromView.height / 2f

        val toView = holes[toPit]
        val toLoc = IntArray(2).also { toView.getLocationOnScreen(it) }
        val toCenterX = toLoc[0] + toView.width / 2f
        val toCenterY = toLoc[1] + toView.height / 2f

        val sizePx = (marbleSizeDp * resources.displayMetrics.density).toInt()
        val half = sizePx / 2

        val flyingMarble = ImageView(requireContext()).apply {
            setImageResource(R.drawable.blue)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            x = fromCenterX - half - overlayLoc[0]
            y = fromCenterY - half - overlayLoc[1]
        }
        overlay.addView(flyingMarble)

        flyingMarble.animate()
            .x(toCenterX - half - overlayLoc[0])
            .y(toCenterY - half - overlayLoc[1])
            .setDuration(300L)
            .withEndAction {
                overlay.removeView(flyingMarble)
                val sourceContainer = holes[fromPit]
                if (sourceContainer.childCount > 0) {
                    sourceContainer.removeViewAt(sourceContainer.childCount - 1)
                }
                val destContainer = holes[toPit]
                val newMarble = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.blue)
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                        gravity = Gravity.CENTER
                        translationX = Random.nextInt(-40, 40).toFloat()
                        translationY = Random.nextInt(-60, 60).toFloat()
                    }
                }
                destContainer.addView(newMarble)
                // resume coroutine
                cont.resume(Unit)
            }
            .start()

            // if the coroutine is cancelled before end, remove the view
            cont.invokeOnCancellation { overlay.removeView(flyingMarble) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

