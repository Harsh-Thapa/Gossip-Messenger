package com.app.gossipmessenger.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.gossipmessenger.R
import com.app.gossipmessenger.adapters.FriendsAdapter
import com.app.gossipmessenger.databinding.FragmentChatsBinding
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.HashMap

class ChatsFragment : Fragment() {
    private var binding: FragmentChatsBinding? = null
    var database = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatsBinding.inflate(inflater, container, false)

        updateUi()

        binding?.rvChat?.showShimmerAdapter()

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            database.collection(Constants.USERS).document(auth).update("fcmToken", it)
        }

        binding?.fabAddFriends?.setOnClickListener {
            dialogSearchMember()
        }

        return binding?.root
    }

    private fun dialogSearchMember(){
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_search_member)
        val tvAdd = dialog.findViewById<TextView>(R.id.tv_add)
        val etPhoneSearch = dialog.findViewById<TextView>(R.id.et_phone_search_member)
        val tvCancel = dialog.findViewById<TextView>(R.id.tv_cancel)

        tvAdd.setOnClickListener {
            val phoneNumber = etPhoneSearch.text.toString()
            if (phoneNumber.isNotEmpty()){
                dialog.dismiss()
                database.collection(Constants.USERS).whereEqualTo(Constants.PHONE_NUMBER, phoneNumber).get().addOnSuccessListener {
                    document ->
                    if (document.documents.size > 0 ){
                        val user = document.documents[0].toObject(User::class.java)
                        if (user!!.uId != FirebaseAuth.getInstance().currentUser!!.uid) {

                            val friend = user.uId
                            database.collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("friends", FieldValue.arrayUnion(friend))
                            database.collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnSuccessListener { document ->
                                val currentUser = document.toObject(User::class.java)
                                database.collection(Constants.USERS).document(friend).update("friends", FieldValue.arrayUnion(currentUser!!.uId))

                                val senderAndReceiverId =  HashMap<String, Any>()
                                senderAndReceiverId["uId"] = auth
                                senderAndReceiverId["receiverId"] = friend
                                val senderRoom = auth + friend
                                val receiverRoom = friend + auth
                                FirebaseFirestore.getInstance().collection(Constants.CHATS).document(senderRoom).set(senderAndReceiverId).addOnSuccessListener {
                                    FirebaseFirestore.getInstance().collection(Constants.CHATS).document(receiverRoom).set(senderAndReceiverId)
                                }
                            }
                        }else {
                            Toast.makeText(context, "Please check your phone number", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(context, "This number is not on Gossip Messenger", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateUi(){
        database.collection(Constants.USERS).whereArrayContains(Constants.FRIENDS, auth).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val friendsList = ArrayList<User>()
                for (document in querySnapshot.documents){
                    val user = document.toObject(User::class.java)
                    friendsList.add(user!!)
                }
                binding?.rvChat?.hideShimmerAdapter()
                val adapter = FriendsAdapter(context, friendsList)
                binding?.rvChat?.adapter = adapter
                binding?.rvChat?.layoutManager = LinearLayoutManager(context)
                binding?.rvChat?.setHasFixedSize(true)
            }
        }
    }

    override fun onStart() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Online")
        super.onStart()
    }

    override fun onResume() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Online")
        super.onResume()
    }

    override fun onPause() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Offline")
        super.onPause()
    }
}