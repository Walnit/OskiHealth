package com.glyph.oskihealth

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A fragment representing a list of Items.
 */
class BotFragment : Fragment() {

    private var columnCount = 1
    private val messages = ArrayList<Message>()
    private lateinit var queue: RequestQueue
    private lateinit var securePrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bot, container, false)
        queue = Volley.newRequestQueue(requireContext())

        // Set the adapter
        with(view) {
            val recyclerView: RecyclerView = findViewById(R.id.list)
            recyclerView.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }

            securePrefs = EncryptedSharedPreferences(requireContext(), "secure_prefs", MasterKey(requireContext()))
            val chunk = securePrefs.getString("chatgpt",
                "[{\"role\":\"system\",\"content\":\"${getString(R.string.chatgpt_init)}\"}]")!!
            val gson = Gson()
            val listType = object : TypeToken<ArrayList<BotMessage>>(){}.type
            val list = gson.fromJson<ArrayList<BotMessage>>(chunk, listType)

            val now = System.currentTimeMillis()
            for (message in list) {
                if (message.role == "system") continue
                else if (message.role == "user") messages.add(Message(now, message.content, "You"))
                else messages.add(Message(now, message.content, "Wellness Bot"))
            }

            recyclerView.adapter = MessagesRecyclerViewAdapter(messages)

            val button: Button = findViewById(R.id.button)
            val content = findViewById<TextInputEditText>(R.id.botMessageContent).text
            val progressBar: ProgressBar = findViewById(R.id.progressBar)

            button.setOnClickListener {
                if (content.isNullOrBlank() || content.isEmpty()) return@setOnClickListener
                button.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                messages.add(Message(System.currentTimeMillis(), content.toString(), "You"))
                content.clear()
                val request = object : AuthorisedRequest(Method.POST, "/chatgpt",
                    { response ->
                        button.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        messages.add(Message(System.currentTimeMillis(), response, "Wellness Bot"))
                        recyclerView.adapter?.notifyItemInserted(messages.size-1)
                        recyclerView.scrollToPosition(messages.size-1)
                    }, {}
                ) {
                    override fun getBody(): ByteArray { return serialise(gson).toByteArray() }
                    override fun getBodyContentType(): String { return "application/json" }
                }
                queue.add(request)
            }
        }
        return view
    }
    fun serialise(gson: Gson): String {
        val data = arrayListOf(BotMessage("system", getString(R.string.chatgpt_init)))
        for (msg in messages) {
            val role = if (msg.sender == "You") "user" else "assistant"
            data.add(BotMessage(role, msg.content))
        }
        return gson.toJson(data)
    }

    override fun onPause() {
        val edit = securePrefs.edit()
        edit.putString("chatgpt", serialise(Gson()))
        edit.apply()
        super.onPause()
    }
}

data class BotMessage(val role: String, val content: String)