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
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A fragment representing a list of Items.
 */
class BotFragment : Fragment() {

    private var columnCount = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bot, container, false)

        // Set the adapter
        with(view) {
            val recyclerView: RecyclerView = findViewById(R.id.list)
            recyclerView.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }

            // TODO: Actually get messages
            val messages: List<Message> = listOf(
                Message(System.currentTimeMillis(), "hello, world!", "bot"),
                Message(System.currentTimeMillis(), "hello, world!", "bot")
            )

            recyclerView.adapter = MessagesRecyclerViewAdapter(messages)

            val button: Button = findViewById(R.id.button)
            val progressBar: ProgressBar = findViewById(R.id.progressBar)

            button.setOnClickListener {
                button.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    // TODO: Funny ChatGPT magic
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        // UPDATE UI HERE
                        button.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }
            }

        }
        return view
    }

}