package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.glyph.oskihealth.databinding.FragmentSecond2Binding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.HashMap

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class OnboardingFragment2 : Fragment() {

    private var _binding: FragmentSecond2Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var queue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecond2Binding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        queue = Volley.newRequestQueue(context)
        binding.usernameEditText.doOnTextChanged { text, _, _, _ ->
            binding.obSignin.isEnabled = !(binding.passwordEditText.text.isNullOrBlank() || text.isNullOrBlank())
        }
        binding.passwordEditText.doOnTextChanged { text, _, _, _ ->
            binding.obSignin.isEnabled = !(binding.usernameEditText.text.isNullOrBlank() || text.isNullOrBlank())
        }

        binding.obSignin.setOnClickListener {
            val username: String = binding.usernameEditText.text.toString()
            val password: String = binding.passwordEditText.text.toString()

            val createRequest = object : AuthorisedRequest(Method.POST, "/create-user",
                { response ->
                    if (response == "good") {
                        success(username, password)
                    } else {
                        val signInRequest = AuthorisedRequest(Method.GET, "/login",
                            { success(username, password) },
                            {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Error!")
                                    .setMessage("Sorry, we are not able to process your request at this time. Please check your username/password and try again later.")
                                    .show()
                            }
                        )
                        queue.add(signInRequest)
                    }
                }, {}
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val old = super.getParams()
                    val new = HashMap<String, String>()
                    if (old != null) for ((key, value) in old) new[key] = value
                    new["username"] = username
                    new["password"] = password
                    return new
                }
            }
            queue.add(createRequest)
        }
    }

    fun success(username: String, password: String) {
        val securePrefs = EncryptedSharedPreferences(
            requireContext(),
            "secure_prefs",
            MasterKey(requireContext()),
        )
        securePrefs.edit().putString("name", username)
            .putString("password", password).apply()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}