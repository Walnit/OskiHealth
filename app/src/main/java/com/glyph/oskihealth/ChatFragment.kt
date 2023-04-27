package com.glyph.oskihealth

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.volley.Request.Method
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.gson.Gson

class chatFragment : Fragment() {
    private var columnCount = 1
    private lateinit var queue: RequestQueue

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        queue = Volley.newRequestQueue(context)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                val chats = arrayListOf(Contact("Wellness Bot", isBot = true))
                adapter = ChatsListRecyclerViewAdapter(chats)
                val request = AuthorisedRequest(Method.GET, "/my-chats",
                    { response ->
                        val gson = Gson()
                        val stuff = gson.fromJson(response, ContactList::class.java)!!
                        for (name in stuff.contactList) {
                            chats.add(Contact(name))
                        }
                        adapter?.notifyItemRangeInserted(1, stuff.contactList.size)
                    }, {}
                )
                queue.add(request)
            }
        }
        return view
    }

    fun refreshContacts() {
        val request = AuthorisedRequest(Method.GET, "/my-chats",
            { response ->
                val gson = Gson()
                val stuff = gson.fromJson(response, ContactList::class.java)!!
            }, {}
        )
        queue.add(request)
    }
}

data class ContactList(val contactList: List<String>)