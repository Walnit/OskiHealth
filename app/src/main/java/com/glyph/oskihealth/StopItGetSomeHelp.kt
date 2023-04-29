package com.glyph.oskihealth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.android.volley.Request.Method
import com.android.volley.toolbox.Volley
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar


/**
 * A simple [Fragment] subclass.
 * Use the [StopItGetSomeHelp.newInstance] factory method to
 * create an instance of this fragment.
 */
class StopItGetSomeHelp : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_stop_it_get_some_help, container, false)
        with (view) {
            findViewById<Button>(R.id.open_chats).setOnClickListener {
                findNavController().navigate(R.id.action_stopItGetSomeHelp_to_chatFragment)
            }
            findViewById<Button>(R.id.chat_ai).setOnClickListener {
                findNavController().navigate(R.id.action_stopItGetSomeHelp_to_botFragment)
            }
            findViewById<Button>(R.id.open_article).setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.healthhub.sg/live-healthy/1926/10-Essentials-for-Mental-Well-Being")))
            }
            findViewById<Button>(R.id.copy_xmr).setOnClickListener {
                val clipboardManager: ClipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("XMR", "49eVpZLA61UBWo4zRmn32HNWk9fEDSUEuBShEHuTyTEQUSwoSKb94XJ8wsKromdoNyHBqFVgLjUvWjoofXMmadheHjhifK9")
                clipboardManager.setPrimaryClip(clipData)
                Snackbar.make(view, "Copied to clipboard!", Snackbar.LENGTH_SHORT).show()
            }
            findViewById<Button>(R.id.subscribe).setOnClickListener {
                val queue = Volley.newRequestQueue(requireContext())
                val request = AuthorisedRequest(Method.POST, "/get-help",
                    { response ->
                        findNavController().navigate(R.id.action_stopItGetSomeHelp_to_messagesFragment, bundleOf("name" to response))
                    }, {}
                )
                queue.add(request)
                Snackbar.make(view, "Success! A psychologist will contact you shortly.", Snackbar.LENGTH_SHORT).show()
            }
        }
        return view
    }

}