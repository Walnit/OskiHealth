package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.glyph.oskihealth.placeholder.PlaceholderContent

/**
 * A fragment representing a list of Items.
 */
class MessagesFragment : Fragment() {

    private var columnCount = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages_list, container, false)

        // Set the adapter
        with(view) {
            val recyclerView: RecyclerView = findViewById(R.id.list)
            recyclerView.layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }

            // TODO: Actually get messages
            val messages: List<Message> = listOf(
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit"),
                Message(System.currentTimeMillis(), "hello, world!", "walnit")
            )

            recyclerView.adapter = MessagesRecyclerViewAdapter(messages)
        }
        return view
    }

}