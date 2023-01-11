package com.app.gossipmessenger.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.app.gossipmessenger.R
import com.app.gossipmessenger.activities.ChatDetailActivity
import com.app.gossipmessenger.databinding.ChatItemLayoutBinding
import com.app.gossipmessenger.models.Chats
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

class FriendsAdapter(private val context: Context?, private val list: ArrayList<User>): RecyclerView.Adapter<FriendsAdapter.ViewHolder>(){
    class ViewHolder(binding: ChatItemLayoutBinding): RecyclerView.ViewHolder(binding.root){
        val ivProfileImage = binding.ivUserImage
        val tvUserName = binding.tvUserName
        val tvLastMessage = binding.tvLastMessage
        val tvLastMessageTime = binding.tvLastMessageTime
        val options = binding.options
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ChatItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        val senderId = FirebaseAuth.getInstance().currentUser!!.uid
        val senderRoom = senderId + item.uId

        FirebaseFirestore.getInstance().collection(Constants.CHATS).document(senderRoom).addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            documentSnapshot?.let {
                val chats = it.toObject(Chats::class.java)
                if (chats!!.lastMessage.isNotEmpty()){
                    val lastMessage = chats.lastMessage
                    val time = chats.lastMessageTime
                    val sdf = SimpleDateFormat("hh:mm a")
                    holder.tvLastMessageTime.text = sdf.format(time)
                    holder.tvLastMessage.text = lastMessage
                }
            }
        }

        if (context != null) {
            Glide.with(context).load(item.profilePicture).centerCrop().placeholder(R.drawable.ic_profile_picture_holder).into(holder.ivProfileImage)
        }
        holder.tvUserName.text = item.name
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatDetailActivity::class.java)
            intent.putExtra("userId", item.uId)
            intent.putExtra("profilePic", item.profilePicture)
            intent.putExtra("name", item.name)
            intent.putExtra("phoneNumber", item.phoneNumber)
            intent.putExtra("fcmToken", item.fcmToken)
            intent.putExtra("userStatus", item.userStatus)
            intent.putExtra("phoneNumber", item.phoneNumber)
            context?.startActivity(intent)
        }

        holder.options.setOnClickListener {
            val popUpMenu = PopupMenu(context, it)
            popUpMenu.inflate(R.menu.menu_options)

            popUpMenu.setOnMenuItemClickListener { menu: MenuItem ->
                when (menu.itemId) {
                    R.id.removeFriend -> {
                        val builder = AlertDialog.Builder(context, R.style.ThemeOverlay_MyApp_MaterialAlertDialog)
                        builder.setTitle("Remove Friend")
                        builder.setMessage("This will delete all the messages and also remove your friend. Do you want to continue?")
                        builder.setPositiveButton("Yes"){ dialog, _ ->
                            FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("friends", FieldValue.arrayRemove(item.uId)).addOnSuccessListener {
                                FirebaseFirestore.getInstance().collection(Constants.USERS).document(item.uId).update("friends", FieldValue.arrayRemove(FirebaseAuth.getInstance().currentUser!!.uid))
                            }
                            dialog.dismiss()
                        }

                        builder.setNegativeButton("No"){ dialog, _ ->
                            dialog.dismiss()
                        }

                        builder.show()

                    }
                    else -> {}
                }
                true
            }
            popUpMenu.show()
        }
}

    override fun getItemCount(): Int {
        return list.size
    }
}