package com.glyph.oskihealth

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.Navigation

import com.glyph.oskihealth.databinding.FragmentChatItemBinding

/**
 * [RecyclerView.Adapter] that can display a [PlaceholderItem].
 * TODO: Replace the implementation with code for your data type.
 */
class ChatsListRecyclerViewAdapter(
    private val values: List<Contact>
) : RecyclerView.Adapter<ChatsListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentChatItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        when (item.isPsych) {
            true -> holder.typeIcon.setImageResource(R.drawable.outline_medical_information_24)
            false -> holder.typeIcon.setImageResource(R.drawable.outline_person_24)
        }
        if (item.isBot) holder.typeIcon.setImageResource(R.drawable.outline_smart_toy_24)
        holder.contentView.text = item.name

        holder.root.setOnClickListener {
            if (!item.isBot) {
                Navigation.findNavController(holder.root).navigate(R.id.action_chatFragment_to_messagesFragment, bundleOf("name" to item.name))
            } else {
                Navigation.findNavController(holder.root).navigate(R.id.action_chatFragment_to_botFragment)
            }
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentChatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val typeIcon: ImageView = binding.itemIcon
        val contentView: TextView = binding.content
        val root: View = binding.root

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}