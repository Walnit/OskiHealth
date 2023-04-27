package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.HashMap

/**
 * A fragment representing a list of Items.
 */
class MessagesFragment : Fragment() {

    private var columnCount = 1
    private lateinit var queue: RequestQueue
    private lateinit var person: String

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        person = requireArguments().getString("name")!!
        val view = inflater.inflate(R.layout.fragment_messages_list, container, false)
        // Set the adapter
        with(view) {
            val recyclerView: RecyclerView = findViewById(R.id.list)
            recyclerView.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }

            queue = Volley.newRequestQueue(context)

            val messages = ArrayList<Message>()
            val request = object : AuthorisedRequest(Method.POST, "/messages",
                { response ->
                    val gson = Gson()
                    val listType = object : TypeToken<ArrayList<Message>>(){}.type
                    val list = gson.fromJson<ArrayList<Message>>(response, listType)
                    for (msg in list) messages.add(msg)
                    recyclerView.adapter = MessagesRecyclerViewAdapter(messages)
                    recyclerView.scrollToPosition(messages.size-1)
                }, {}
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val old = super.getParams()
                    val new = HashMap<String, String>()
                    if (old != null) for ((key, value) in old) new[key] = value
                    new["username"] = person
                    return new
                }
            }
            queue.add(request)

            val sendButton = findViewById<Button>(R.id.button)
            val content = findViewById<TextInputEditText>(R.id.sendMessageContent)
            sendButton.setOnClickListener {
                // add to recyclerview
                val now = Calendar.getInstance().timeInMillis
                val text = content.text.toString()
                messages.add(Message(now, text, AuthorisedRequest.USERNAME))
                recyclerView.adapter?.notifyItemInserted(messages.size-1)
                content.text?.clear()

                val sendRequest = object : AuthorisedRequest(Method.POST, "/send", {}, {}) {
                    override fun getParams(): MutableMap<String, String>? {
                        val old = super.getParams()
                        val new = HashMap<String, String>()
                        if (old != null) for ((key, value) in old) new[key] = value
                        new["recipient"] = person
                        new["content"] = text
                        return new
                    }
                }
                queue.add(sendRequest)
            }

            content.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                recyclerView.scrollToPosition(messages.size-1)
            }
        }
        return view
    }
}