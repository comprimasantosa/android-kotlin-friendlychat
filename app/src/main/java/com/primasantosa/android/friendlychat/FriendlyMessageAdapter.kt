package com.primasantosa.android.friendlychat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendlyMessageAdapter(private var friendlyMessageData: List<FriendlyMessage>) :
    RecyclerView.Adapter<FriendlyMessageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTv: TextView = itemView.findViewById(R.id.nameTextView)
        val messageTv: TextView = itemView.findViewById(R.id.messageTextView)
        val photoTv: ImageView = itemView.findViewById(R.id.photoImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return friendlyMessageData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = friendlyMessageData[position]

        // Set name
        holder.nameTv.text = message.name

        // Set message
        holder.messageTv.text = message.text

        // Set photo
//        if (message?.photoUrl != null) {
//            holder.photoTv.visibility = View.VISIBLE
//            holder.messageTv.visibility = View.GONE
//
//            Glide.with(holder.itemView.context)
//                .load(message.photoUrl)
//                .into(holder.photoTv)
//        } else {
//            holder.photoTv.visibility = View.GONE
//            holder.photoTv.visibility = View.VISIBLE
//        }
    }
}