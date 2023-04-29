package com.glyph.oskihealth

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChatFragment : Fragment() {
    private var columnCount = 1
    private lateinit var queue: RequestQueue
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        queue = Volley.newRequestQueue(context)

        // Set the adapter
        with(view) {
            recyclerView = findViewById(R.id.list)
            recyclerView.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            refresh()

            val addContactButton: Button = findViewById(R.id.addContactButton)
            addContactButton.setOnClickListener {
                val usernameEditText = EditText(requireContext())
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Contact")
                    .setMessage("Enter the username of the person you want to contact: ")
                    .setView(usernameEditText)
                    .setPositiveButton("Done") { dialogInterface: DialogInterface, _ ->
                        dialogInterface.dismiss()
                        if (!usernameEditText.text.isNullOrBlank()) {
                            val username = usernameEditText.text.toString()
                            findNavController().navigate(R.id.action_chatFragment_to_messagesFragment, bundleOf("name" to username))
                            Snackbar.make(recyclerView, "Messaging: $username", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(recyclerView, "Empty username!", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ -> }
                    .show()
            }
        }
        return view
    }

     fun refresh() {
         val request = AuthorisedRequest(Method.GET, "/my-chats",
             { response ->
                 val chats = arrayListOf(Contact("Wellness Bot", isBot = true))
                 val gson = Gson()
                 val listType = object : TypeToken<ArrayList<ContactItem>>() {}.type
                 val list = gson.fromJson<ArrayList<ContactItem>>(response, listType)
                 for (name in list) chats.add(Contact(name.username, name.psych))
                 recyclerView.adapter = ChatsListRecyclerViewAdapter(chats)
             }, {}
         )
         queue.add(request)
     }
}

data class ContactItem(val username: String, val psych: Boolean)