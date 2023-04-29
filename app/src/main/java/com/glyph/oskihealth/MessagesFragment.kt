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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.patrykandpatrick.vico.core.extension.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.util.Calendar
import java.util.HashMap

/**
 * A fragment representing a list of Items.
 */
class MessagesFragment : Fragment() {

    private var columnCount = 1
    private lateinit var queue: RequestQueue
    private lateinit var person: String
    private lateinit var connection: HttpURLConnection

    companion object {
        val connections = HashMap<String, HttpURLConnection>()
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        person = requireArguments().getString("name")!!
        val view = inflater.inflate(R.layout.fragment_messages_list, container, false)
        val sentiment_prefs = EncryptedSharedPreferences(requireContext(), "sentimentData", MasterKey(requireContext()))
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
                if (!content.text.isNullOrEmpty()) {
                    // add to recyclerview
                    val now = Calendar.getInstance().timeInMillis
                    val text = content.text.toString()
                    messages.add(Message(now, text, AuthorisedRequest.USERNAME))
                    recyclerView.adapter?.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                    content.text?.clear()

                    val sendRequest = object : AuthorisedRequest(Method.POST, "/send",
                        { response ->
                            val gson = Gson()
                            val something = response.substring(1, response.length - 2)
                            val nlp = gson.fromJson(something, NLPResult::class.java)
                            if (text.split(" ").size > 1) {
                                var resultValue: Float
                                if (nlp.label == "positive") {
                                    resultValue = 3f
                                } else if (nlp.label == "negative") {
                                    resultValue = -3f
                                } else {
                                    resultValue = 0f
                                }
                                val newValue =
                                    (sentiment_prefs.getFloat(
                                        (System.currentTimeMillis() / 86400000f).floor.toString(),
                                        0f
                                    ) + resultValue) /
                                            (sentiment_prefs.getInt(
                                                "${(System.currentTimeMillis() / 86400000f).floor.toString()}_count",
                                                0
                                            ) + 1)
                                sentiment_prefs.edit()
                                    .putFloat(
                                        (System.currentTimeMillis() / 86400000f).floor.toString(),
                                        newValue
                                    )
                                    .putInt(
                                        "${(System.currentTimeMillis() / 86400000f).floor.toString()}_count",
                                        sentiment_prefs.getInt(
                                            "${(System.currentTimeMillis() / 86400000f).floor.toString()}_count",
                                            0
                                        ) + 1
                                    )
                                    .apply()
                                Snackbar.make(this@with, nlp.label, Snackbar.LENGTH_SHORT).show()
                            }
                        }, {}) {
                        override fun getParams(): MutableMap<String, String> {
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
            }

            content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                recyclerView.scrollToPosition(messages.size-1)
            }


            // event stream yes
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            connections[person]?.disconnect()
            coroutineScope.launch {
                val url = URL(AuthorisedRequest.HOST + "/subscribe")
                connection = withContext(Dispatchers.IO) { url.openConnection() } as HttpURLConnection
                connections[person] = connection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Accept", "text/event-stream")
                connection.setRequestProperty("X-Username", AuthorisedRequest.USERNAME)
                connection.setRequestProperty("X-Password", AuthorisedRequest.PASSWORD)

                val postData = "username=$person".toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Length", postData.size.toString())
                connection.doOutput = true
                val outputStream = connection.outputStream
                withContext(Dispatchers.IO) { outputStream.write(postData) }
                withContext(Dispatchers.IO) { outputStream.flush() }

                val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)

                try {
                    while (true) {
                        val line = withContext(Dispatchers.IO) { reader.readLine() }
                        if (line == null) break
                        else if (line.isEmpty()) continue
                        val dataField = Regex("^data:(.*)$").find(line)?.groupValues?.get(1)
                        if (dataField != null) {
                            val message = Gson().fromJson(dataField, Message::class.java)
                            if (message.sender == AuthorisedRequest.USERNAME) continue
                            withContext(Dispatchers.Main) {
                                messages.add(message)
                                recyclerView.adapter?.notifyItemInserted(messages.size - 1)
                                recyclerView.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                } catch (_: SocketException) {} // cry about it
            }
        }
        return view
    }
}

data class NLPResult(val label: String, val score: Float)