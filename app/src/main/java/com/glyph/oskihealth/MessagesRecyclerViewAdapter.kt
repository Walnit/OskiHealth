package com.glyph.oskihealth

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.glyph.oskihealth.placeholder.PlaceholderContent.PlaceholderItem
import com.glyph.oskihealth.databinding.FragmentMessagesItemBinding

/**
 * [RecyclerView.Adapter] that can display a [Message].
 * TODO: Replace the implementation with code for your data type.
 */
class MessagesRecyclerViewAdapter(
    private val values: List<Message>
) : RecyclerView.Adapter<MessagesRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentMessagesItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.sender.text = item.sender
        holder.content.text = item.content
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentMessagesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val sender: TextView = binding.messageSender
        val content: TextView = binding.messageContent

        override fun toString(): String {
            return super.toString() + " '" + content.text + "'"
        }
    }

}