package com.app.gossipmessenger.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.app.gossipmessenger.R
import com.app.gossipmessenger.adapters.ChattingAdapter
import com.app.gossipmessenger.databinding.ActivityChatDetailBinding
import com.app.gossipmessenger.models.Chats
import com.app.gossipmessenger.models.MessageModel
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatDetailActivity : BaseActivity() {
    private var binding: ActivityChatDetailBinding? = null
    private val data = FirebaseDatabase.getInstance()
    private val senderId = auth.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        binding?.ivBack?.setOnClickListener {
            onBackPressed()
        }



        auth = FirebaseAuth.getInstance()
        val receiverId = intent.getStringExtra("userId")
        val userName = intent.getStringExtra("name")
        val profilePic = intent.getStringExtra("profilePic")
        val fcmToken = intent.getStringExtra("fcmToken")
        val userStatus = intent.getStringExtra("userStatus")
        val phoneNumber = intent.getStringExtra("phoneNumber")

        binding?.llUserStatus?.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", receiverId)
            intent.putExtra("profilePic", profilePic)
            intent.putExtra("name", userName)
            intent.putExtra("phoneNumber", phoneNumber)
            intent.putExtra("userStatus", userStatus)
            startActivity(intent)
        }

        val senderRoom: String = senderId + receiverId
        val receiverRoom: String = receiverId + senderId

        binding?.ivOptions?.setOnClickListener {
            val popUpMenu = PopupMenu(this, it)
            popUpMenu.inflate(R.menu.menu_options_chat_detail)

            popUpMenu.setOnMenuItemClickListener { menu: MenuItem ->
                when (menu.itemId) {
                    R.id.clearChat -> {
                        val builder = AlertDialog.Builder(this, R.style.ThemeOverlay_MyApp_MaterialAlertDialog)
                        builder.setTitle("Clear Chat")
                        builder.setMessage("This will clear all the messages from your chat. Do you want to continue?")
                        builder.setPositiveButton("Yes"){ dialog, _ ->
                            val hashmap = HashMap<String, Any>()
                            hashmap["lastMessage"] = ""
                            hashmap["lastMessageTime"] = 0
                            database.collection(Constants.CHATS).document(senderRoom).update(hashmap).addOnSuccessListener {
                                database.collection(Constants.CHATS).document(receiverRoom).update(hashmap).addOnSuccessListener {
                                    database.collection(Constants.CHATS).document(senderRoom).update("messages", FieldValue.delete()).addOnSuccessListener {
                                        database.collection(Constants.CHATS).document(receiverRoom).update("messages", FieldValue.delete())
                                    }
                                }
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


        database.collection(Constants.CHATS).document(senderRoom).addSnapshotListener{ documentSnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            documentSnapshot?.let {
                val messageList: ArrayList<MessageModel> = ArrayList()
                val messages = it.toObject(Chats::class.java)
                Log.e("MESSAGES", "$messages")
                for (messages in messages!!.messages){
                    messageList.add(messages)
                }
                val chatAdapter = ChattingAdapter(this, messageList)
                binding?.rvChatDetail?.adapter = chatAdapter
                val linearLayoutManager = LinearLayoutManager(this)
                linearLayoutManager.stackFromEnd = true
                binding?.rvChatDetail?.layoutManager = linearLayoutManager
            }
        }

        data.reference.child("presence").child(receiverId!!).addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val status = snapshot.getValue(String::class.java)
                    if (status!!.isNotEmpty()){
                        if(status == "Offline"){
                            binding?.tvStatus?.visibility = View.GONE
                        }else {
                            binding?.tvStatus?.text = status
                            binding?.tvStatus?.visibility = View.VISIBLE
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK ) {
                showCustomProgressDialog()
                val data: Intent? = result.data
                Log.e("IMAGE", "$data")
                if (data != null){
                    if (data.data != null){
                        val selectedImageUri = data.data
                        Log.e("CAMERA IMAGE", "$selectedImageUri")
                        val reference = FirebaseStorage.getInstance().reference.child("chats").child(
                            "Attachment" + System.currentTimeMillis() + "." + BaseActivity().getFileExtension(this, selectedImageUri))
                        if (selectedImageUri != null) {
                            reference.putFile(selectedImageUri).addOnCompleteListener{ task->
                                hideProgressDialog()
                                if (task.isSuccessful){
                                    reference.downloadUrl.addOnSuccessListener {
                                        val filePath = it.toString()
                                        val message = "photo"
                                        val model = MessageModel(message)
                                        model.imageUrl = filePath
                                        model.timeStamp = Date().time
                                        val lastMessageObject =  HashMap<String, Any>()
                                        lastMessageObject["lastMessage"] = model.message
                                        lastMessageObject["lastMessageTime"] = model.timeStamp
                                        database.collection(Constants.CHATS).document(senderRoom).update(lastMessageObject)
                                        database.collection(Constants.CHATS).document(receiverRoom).update(lastMessageObject)
                                        binding?.etMessage?.setText("")
                                        database.collection(Constants.CHATS).document(senderRoom).update("messages", FieldValue.arrayUnion(model)).addOnSuccessListener {
                                            database.collection(Constants.CHATS).document(receiverRoom).update("messages", FieldValue.arrayUnion(model))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK ) {
                showCustomProgressDialog()
                val data: Intent? = result.data
                Log.e("CAMERA IMAGE", "$data")
                if(data != null){
                    if (data.data != null){
                        val pic = data.getParcelableExtra<Bitmap>("data")
                    }
                }
            }
        }

        binding?.attachment?.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            resultLauncher.launch(intent)
        }

        binding?.tvReceiverName?.text = userName
        binding?.ivReceiverImage?.let {
            Glide.with(this).load(profilePic).centerCrop().placeholder(R.drawable.ic_profile_picture_holder).into(it)
        }



        val handler = Handler()
        binding?.etMessage?.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                data.reference.child("presence").child(senderId!!).setValue("Typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }
            val userStoppedTyping: Runnable = Runnable {
                data.reference.child("presence").child(senderId!!).setValue("Online")
            }
        })

        binding?.ivSend?.setOnClickListener {
            if (binding?.etMessage?.text.toString().isNotEmpty()){
                val message = binding?.etMessage?.text.toString()
                val model = MessageModel(message = message)
                model.timeStamp = Date().time
                val lastMessageObject =  HashMap<String, Any>()
                lastMessageObject["lastMessage"] = model.message
                lastMessageObject["lastMessageTime"] = model.timeStamp
                database.collection(Constants.CHATS).document(senderRoom).update(lastMessageObject)
                database.collection(Constants.CHATS).document(receiverRoom).update(lastMessageObject)
                binding?.etMessage?.setText("")
                database.collection(Constants.CHATS).document(senderRoom).update("messages", FieldValue.arrayUnion(model)).addOnSuccessListener {
                    database.collection(Constants.CHATS).document(receiverRoom).update("messages", FieldValue.arrayUnion(model)).addOnSuccessListener {
                        database.collection(Constants.USERS).document(senderId!!).get().addOnSuccessListener {
                            val user = it.toObject(User::class.java)
                            sendNotification(user!!.name, message, fcmToken!!)
                        }
                    }
                }
            }
        }
    }

    private fun sendNotification(name: String, message: String, fcmToken: String){
        try {
            val queue = Volley.newRequestQueue(this)
            val url = "https://fcm.googleapis.com/fcm/send"

            val data = JSONObject()
            data.put("title", name)
            data.put("body", message)

            val notificationData = JSONObject()
            notificationData.put("notification", data)
            notificationData.put("to", fcmToken)

            val request = object: JsonObjectRequest(url, notificationData, { }, {  }){
                override fun getHeaders(): MutableMap<String, String> {
                    val map = HashMap<String, String>()
                    map["Authorization"] = "key = AAAAOZwJyXI:APA91bHpi9AlBSM5M7o0Vv5lYRVJ1LtdY-xWjblk1obn172ut5sazrMAfzKeoemNzpG4IcnnbbXwQqwozdElKIXSLptIo9kSDChDciCpOVaWDf99AiFqLAlolSQCA3OnkmQgNeQ18z7o"
                    map["Content-Type"] = "application/json"
                    Log.e("MAP", "$map")
                    return map
                }
            }
            queue.add(request)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onStart() {
        database.collection(Constants.USERS).document(auth.currentUser!!.uid).update("onlineStatus", "Online")
        super.onStart()
    }

    override fun onPause() {
        database.collection(Constants.USERS).document(auth.currentUser!!.uid).update("onlineStatus", "Offline")
        super.onPause()
    }
}