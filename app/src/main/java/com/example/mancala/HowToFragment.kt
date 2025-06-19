package com.example.mancala

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mancala.databinding.FragmentHowToBinding

class HowToFragment : Fragment() {

    private var _binding: FragmentHowToBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHowToBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()

//        binding.easyButton.setOnClickListener {
//            val args = Bundle().apply { putString("difficulty", "easy") }
//            nav.navigate(R.id.action_home_to_game, args)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}