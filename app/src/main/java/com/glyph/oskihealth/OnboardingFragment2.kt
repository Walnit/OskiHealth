package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.glyph.oskihealth.databinding.FragmentSecond2Binding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class OnboardingFragment2 : Fragment() {

    private var _binding: FragmentSecond2Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecond2Binding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.usernameEditText.doOnTextChanged { text, _, _, _ ->
            binding.obSignin.isEnabled = !text.isNullOrBlank()
        }
        binding.passwordEditText.doOnTextChanged { text, _, _, _ ->
            binding.obSignin.isEnabled = !text.isNullOrBlank()
        }

        binding.obSignin.setOnClickListener {
            val username: String = binding.usernameEditText.text.toString()
            val password: String = binding.passwordEditText.text.toString()

            val securePrefs = EncryptedSharedPreferences(
                requireContext(),
                "secure_prefs",
                MasterKey(requireContext()),
            )

            securePrefs.edit().putString("name", username).putString("password", password).apply()

            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}