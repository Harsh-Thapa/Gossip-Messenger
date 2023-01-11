package com.app.gossipmessenger.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.gossipmessenger.R
import com.app.gossipmessenger.databinding.MessageLayoutBinding
import com.app.gossipmessenger.models.MessageModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*


class ChattingAdapter(private val context: Context, private val list: ArrayList<MessageModel>): RecyclerView.Adapter<ChattingAdapter.ViewHolder>() {

    class ViewHolder(binding: MessageLayoutBinding): RecyclerView.ViewHolder(binding.root){
        var senderMsg: TextView = binding.tvSentMessage
        var receiverMsg = binding.tvReceivedMessage
        var sendTime = binding.tvSentTime
        var receiveTime = binding.tvReceiveTime
        var rlReceive = binding.rlReceivedMessage
        var rlSend = binding.rlSentMessage
        var ivReceivedImage = binding.ivReceivedImage
        var ivSentImage = binding.ivSentImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(MessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        when (item.phoneNumber) {
            FirebaseAuth.getInstance().currentUser!!.phoneNumber -> {
                val messageSentTime = item.timeStamp
                val sdf = SimpleDateFormat("hh:mm a")
                holder.sendTime.text = sdf.format(messageSentTime)
                holder.rlReceive.visibility = View.GONE
                if (item.message == "photo") {
                    holder.senderMsg.visibility = View.GONE
                    Glide.with(context).load(item.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder).into(holder.ivSentImage)
                } else {
                    holder.senderMsg.text = item.message
                    holder.senderMsg.visibility = View.VISIBLE
                    holder.ivSentImage.visibility = View.GONE
                }
            }
            else -> {
                val messageReceiveTime = item.timeStamp
                val sdf = SimpleDateFormat("hh:mm a")
                holder.receiveTime.text = sdf.format(messageReceiveTime)
                holder.rlSend.visibility = View.GONE
                if (item.message == "photo") {
                    holder.receiverMsg.visibility = View.GONE
                    Glide.with(context).load(item.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder).into(holder.ivReceivedImage)
                } else {
                    holder.receiverMsg.text = item.message
                    holder.receiverMsg.visibility = View.VISIBLE
                    holder.ivReceivedImage.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}