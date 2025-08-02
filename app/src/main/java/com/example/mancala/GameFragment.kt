/**
Author: Julia Duffy
Last Edited: 6/20/2025
 */
package com.example.mancala

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.mancala.databinding.FragmentGameBinding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.mancala.GameViewModel.GameViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Path
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.activityViewModels
import kotlin.math.min


/**
 * This class implements the front-end logic for the Mancala game screen. Marble animations are
 * triggered by events that are collected from the game view model. All of the holes on the board
 * have counts that display how many marbles there are in that hole. Also, there are messages that
 * are displayed at the top of the screen that describe each step of the game to the user.
 */
class GameFragment : Fragment() {
    // Stores the game settings (right now this is just the first move choice)
    private val homeViewModel: HomeViewModel by activityViewModels()
    // Stores a reference to the game vm which contains the majority of the game logic.
    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(Dispatchers.Main)
    }
    // view binding stuff
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    // List of the hole ui elements
    private lateinit var holes: List<FrameLayout>
    // The ui "counts" displays on the screen
    private lateinit var holeCounts:List<TextView>
    // Marble size
    private val marbleSizeDp = 25

    // All of the animation events
    private sealed class AnimationEvent {
        data class Move(val fromPit: Int, val toPit: Int) : AnimationEvent()
        data class Capture(val landingPit: Int, val storePit: Int) : AnimationEvent()
        data object ComputerTurn : AnimationEvent()
        data object PlayerTurn : AnimationEvent()
        data class GameOver(val winner: Int) : AnimationEvent()
        data object EndOfGame : AnimationEvent()
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
        // store references to all hole count ui elements
        holeCounts = listOf(
            binding.hole0Count,
            binding.hole1Count,
            binding.hole2Count,
            binding.hole3Count,
            binding.hole4Count,
            binding.hole5Count,
            binding.rightStoreCount,
            binding.hole7Count,
            binding.hole8Count,
            binding.hole9Count,
            binding.hole10Count,
            binding.hole11Count,
            binding.hole12Count,
            binding.leftStoreCount
        )
        // difficulty passed in via nav args
        val difficulty = arguments?.getString("difficulty") ?: "easy"

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
        // nifty way to collect and respond to a bunch of different animation events at once
        val animEvents: Flow<AnimationEvent> = merge(
            viewModel.moveMarbleEvent.map { (from, to) -> AnimationEvent.Move(from, to) },
            viewModel.playerCaptureEvent.map { (landing, store) -> AnimationEvent.Capture(landing, store) },
            viewModel.computerTurnEvent.map {  AnimationEvent.ComputerTurn },
            viewModel.playerTurnEvent.map{ AnimationEvent.PlayerTurn },
            viewModel.winEvent.map{ w  -> AnimationEvent.GameOver(w) },
            viewModel.endOfGameEvent.map{AnimationEvent.EndOfGame }
        )

        binding.backButtonGame.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        viewModel.startGame(difficulty, homeViewModel.firstMove)
        if(viewModel.currentPlayer.value == 0)
            binding.gameCaptions.text = "Player Turn"
        else binding.gameCaptions.text = "Computer Turn"

        // collect emits from the vm to trigger animations
        lifecycleScope.launch {
            animEvents.collect { event ->
                when (event) {
                    is AnimationEvent.Move -> {
                        animateSingleMarbleMove(event.fromPit, event.toPit)
                        var newCount= viewModel.marbles.value[event.toPit]
                        holeCounts[event.toPit].text = "$newCount"
                        newCount =  viewModel.marbles.value[event.fromPit]
                        holeCounts[event.fromPit].text = "$newCount"
                    }

                    is AnimationEvent.Capture -> {
                        binding.gameCaptions.text = "Capture!"
                        animateSingleMarbleMove(event.landingPit, event.storePit, 0.8F)
                        var newCount =  viewModel.marbles.value[event.landingPit]
                        holeCounts[event.landingPit].text = "$newCount"
                        newCount = viewModel.marbles.value[event.storePit]
                        holeCounts[event.storePit].text = "$newCount"

                        val opposite = 12 - event.landingPit
                        repeat(holes[opposite].childCount) {
                            animateSingleMarbleMove(opposite, event.storePit, 0.8F)
                        }
                        newCount = viewModel.marbles.value[opposite]
                        holeCounts[opposite].text = "$newCount"
                        newCount =  viewModel.marbles.value[event.storePit]
                        holeCounts[event.storePit].text = "$newCount"
                        if (viewModel.currentPlayer.value == 1) binding.gameCaptions.text = "Computer Turn" else binding.gameCaptions.text =
                            "Player Turn"
                    }
                    is AnimationEvent.ComputerTurn -> {
                        binding.gameCaptions.text = "Computer Turn"
                        delay(2000)
                        viewModel.move(0)
                    }
                    is AnimationEvent.PlayerTurn -> {
                        binding.gameCaptions.text = "Player Turn"
                    }
                    is AnimationEvent.GameOver   -> {
                        delay(2000)
                        findNavController().navigate(
                            R.id.action_game_to_game_over,
                            bundleOf("winner" to event.winner, "difficulty" to difficulty,
                                "playerScore" to viewModel.marbles.value[6],
                                "computerScore" to viewModel.marbles.value[13])
                        )

                    }
                    is AnimationEvent.EndOfGame -> {
                        binding.gameCaptions.text = "Calculating Final Score..."
                    }
                }
            }
        }
    }

    /**
     * Sets up the board with 4 marbles / hole
     */
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
                    translationX = Random.nextInt(-20, 20).toFloat()
                    translationY = Random.nextInt(-20, 20).toFloat()
                }
                container.addView(marble)
            }
        }
    }
    /**
     * Animate a single marble from pit a to pit b
     * Full disclaimer chatGPT helped me a lot with this one
    */
    private suspend fun animateSingleMarbleMove(fromPit: Int, toPit: Int, speed: Float = 1f ) =
        suspendCancellableCoroutine { cont ->

            if (!isAdded || view == null || !::holes.isInitialized) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            val overlay = binding.animationOverlay
            if (fromPit !in holes.indices || toPit !in holes.indices) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            // screen coords
            val overlayLoc = IntArray(2).also { overlay.getLocationOnScreen(it) }
            val toView = holes[toPit]
            val toLoc = IntArray(2).also { toView.getLocationOnScreen(it) }
            val toCenterX = toLoc[0] + toView.width / 2f
            val toCenterY = toLoc[1] + toView.height / 2f

            // marble view and starting pos
            val sizePx = (marbleSizeDp * resources.displayMetrics.density).toInt()
            val half = sizePx / 2
            val marbleView = holes[fromPit].getChildAt(holes[fromPit].childCount - 1)
            if (marbleView == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            marbleView.elevation = 10f
            val marblePos = IntArray(2).also { marbleView.getLocationOnScreen(it) }
            val startX = (marblePos[0] - overlayLoc[0]).toFloat()
            val startY = (marblePos[1] - overlayLoc[1]).toFloat()

            // create flying marble
            val flyingMarble = ImageView(requireContext()).apply {
                setImageResource(R.drawable.blue)
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                x = startX
                y = startY
            }
            overlay.addView(flyingMarble)

            // build the curved path
            val endX = toCenterX - half - overlayLoc[0]
            val endY = toCenterY - half - overlayLoc[1]
            val path = Path().apply {
                moveTo(startX, startY)
                // lift the arc above the straight line
                quadTo(
                    (startX + endX) / 2f,
                    min(startY, endY) - 100f,
                    endX,
                    endY
                )
            }

            // animate along that path
            ObjectAnimator.ofFloat(flyingMarble, View.X, View.Y, path).apply {
                duration = (200.0 * speed).toLong()
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // check to prevent crashes
                        if (!isAdded || view == null) {
                            cont.resume(Unit)
                            return
                        }
                        // cleanup and update pits
                        overlay.removeView(flyingMarble)
                        holes[fromPit]
                            .takeIf { it.childCount > 0 }
                            ?.also { pit -> pit.removeViewAt(pit.childCount - 1) }
                        holes[toPit].addView(ImageView(requireContext()).apply {
                            setImageResource(R.drawable.blue)
                            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                                gravity = Gravity.CENTER
                                translationX = Random.nextInt(-20, 20).toFloat()
                                translationY = Random.nextInt(-10, 10).toFloat()
                            }
                        })
                        cont.resume(Unit)
                    }
                })
                start()
            }

            cont.invokeOnCancellation { overlay.removeView(flyingMarble) }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

