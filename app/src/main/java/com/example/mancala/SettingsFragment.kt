/**
Author: Julia Duffy
Last Edited: 6/20/2025
 */
package com.example.mancala

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mancala.databinding.SettingsFragmentBinding

/**
 * Fragment for the settings page, stores the first move choice in the home vm.
 * Uses an android slider to allow the user to choose who goes first
 */
class SettingsFragment: Fragment() {

    private val vm: HomeViewModel by activityViewModels()
    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.firstMoveSlider.value = vm.firstMove.toFloat()

        binding.backButtonSettings.setOnClickListener {
            val first = binding.firstMoveSlider.value
            vm.firstMove = first.toInt()
            findNavController().navigate(R.id.action_settings_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}