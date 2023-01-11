package com.app.gossipmessenger.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.gossipmessenger.activities.MainActivity
import com.app.gossipmessenger.databinding.ItemStatusBinding
import com.app.gossipmessenger.models.User
import com.bumptech.glide.Glide
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory


class TopStatusAdapter(private val context: Context?, private val userStatus: ArrayList<User>): RecyclerView.Adapter<TopStatusAdapter.ViewHolder>() {

    class ViewHolder(binding: ItemStatusBinding): RecyclerView.ViewHolder(binding.root) {
        val circleStatusView = binding.clCircleStatusView
        val lastStatus = binding.civLastStatus
        val circleStatusCount = binding.circularStatusView
        val tvStatus = binding.tvStatus
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var user = userStatus[position]
        holder.tvStatus.text = user.name
        val lastUpdate = user.status[user.status.size - 1]
        Glide.with(context!!).load(lastUpdate.imageUrl).into(holder.lastStatus)
        holder.circleStatusCount.setPortionsCount(user.status.size)
        holder.circleStatusView.setOnClickListener {
            val story = ArrayList<MyStory>()
            for (status in user.status){
                story.add(MyStory(status.imageUrl))
            }
            StoryView.Builder((context as MainActivity).supportFragmentManager)
                .setStoriesList(story) // Required
                .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                .setTitleText(user.name) // Default is Hidden
                .setSubtitleText("") // Default is Hidden
                .setTitleLogoUrl(user.profilePicture) // Default is Hidden
                .setStoryClickListeners(object : StoryClickListeners {
                    override fun onDescriptionClickListener(position: Int) {
                    }

                    override fun onTitleIconClickListener(position: Int) {
                    }
                })
                .build()
                .show()
        }
    }

    override fun getItemCount(): Int {
        return userStatus.size
    }
}