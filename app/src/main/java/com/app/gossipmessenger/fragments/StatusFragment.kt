package com.app.gossipmessenger.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.gossipmessenger.R
import com.app.gossipmessenger.activities.MainActivity
import com.app.gossipmessenger.adapters.TopStatusAdapter
import com.app.gossipmessenger.databinding.FragmentStatusBinding
import com.app.gossipmessenger.models.Status
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class StatusFragment : Fragment() {
    var binding: FragmentStatusBinding? = null
    private lateinit var mProgressDialog: Dialog
    private val database = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance().currentUser!!.uid

    private var mSelectedImageFileUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatusBinding.inflate(layoutInflater)

        deleteStatusAfterExpire()
        updateMyStatus()
        updateStatusUi()

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent = result.data!!
                mSelectedImageFileUri = data.data
                if (data.data != null){
                    showCustomProgressDialog()
                    val storage = FirebaseStorage.getInstance()
                    val date = Date()
                    val reference = storage.reference.child("status").child(date.time.toString())

                    reference.putFile(data.data!!).addOnCompleteListener{ task ->
                        if (task.isSuccessful){
                            reference.downloadUrl.addOnSuccessListener { Uri ->

                                FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnSuccessListener {
                                    val status = Status(imageUrl = Uri.toString(), timeStamp = date.time)
                                    FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("lastStatusTimeStamp", date.time).addOnSuccessListener {
                                        FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("status", FieldValue.arrayUnion(status))
                                    }
                                }
                                hideProgressDialog()
                            }
                        }
                    }
                }
            }
        }

        binding?.statusDelete?.setOnClickListener {
            val builder = AlertDialog.Builder(context, R.style.ThemeOverlay_MyApp_MaterialAlertDialog)
            builder.setTitle("Delete Statuses")
            builder.setMessage("This will delete all the statuses. Do you want to continue?")
            builder.setPositiveButton("Yes"){ dialog, _ ->
                FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val user = it.toObject(User::class.java)
                        for (status in user!!.status) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["imageUrl"] = status.imageUrl
                            hashMap["timeStamp"] = status.timeStamp
                            FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("status", FieldValue.arrayRemove(hashMap))
                        }
                    }
                }
                binding?.textStatus?.visibility = View.GONE
                binding?.clMyProfile?.visibility = View.GONE
                dialog.dismiss()
            }

            builder.setNegativeButton("No"){ dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }

        binding?.fabAddStatus?.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            resultLauncher.launch(intent)
        }

        return binding?.root
    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(requireContext())
        mProgressDialog.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)
        mProgressDialog.show()
    }

    private fun hideProgressDialog(){
        mProgressDialog.dismiss()
    }

    private fun updateMyStatus(){
        FirebaseFirestore.getInstance().collection(Constants.USERS).whereEqualTo(Constants.UID, FirebaseAuth.getInstance().currentUser?.uid).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                for (document in querySnapshot.documents){
                    val user = document.toObject(User::class.java)
                    if (user!!.status.isNotEmpty()){
                        binding?.textStatus?.visibility = View.VISIBLE
                        binding?.clMyProfile?.visibility = View.VISIBLE
                        val lastUpdate = user.status[user.status.size - 1]
                        Glide.with(requireContext()).load(lastUpdate.imageUrl).into(binding?.civLastStatus!!)
                        binding?.circularStatusView?.setPortionsCount(user.status.size)
                        binding?.clMyProfile?.setOnClickListener {
                            val story = ArrayList<MyStory>()
                            for (status in user.status) {
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
                }
            }
        }
    }

    private fun deleteStatusAfterExpire(){
        FirebaseFirestore.getInstance().collection(Constants.USERS).whereEqualTo(Constants.UID, FirebaseAuth.getInstance().currentUser!!.uid).addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                for (document in querySnapshot.documents) {
                    val user = document.toObject(User::class.java)
                    if (user!!.status.isNotEmpty()){
                        for (status in user.status) {
                            val timeDelete = status.timeStamp + 86400000
                            if (timeDelete <= Date().time) {
                                val hashMap = HashMap<String, Any>()
                                hashMap["imageUrl"] = status.imageUrl
                                hashMap["timeStamp"] = status.timeStamp
                                FirebaseFirestore.getInstance().collection(Constants.USERS).document(FirebaseAuth.getInstance().currentUser!!.uid).update("status", FieldValue.arrayRemove(hashMap))
                                updateMyStatus()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStatusUi(){
        FirebaseFirestore.getInstance().collection(Constants.USERS).whereArrayContains(Constants.FRIENDS, FirebaseAuth.getInstance().currentUser!!.uid).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            querySnapshot?.let{
                val users = ArrayList<User>()
                for (document in querySnapshot.documents){
                    val user = document.toObject(User::class.java)
                    if (user!!.status.isNotEmpty()){
                        users.add(user)
                    }
                }
                val adapter = TopStatusAdapter(context, users)
                binding?.rvStatus?.adapter = adapter
                binding?.rvStatus?.layoutManager = LinearLayoutManager(context)
            }
        }
    }

    override fun onStart() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Online")

        FirebaseDatabase.getInstance().reference.child("presence").child(auth).setValue("Online")
        super.onStart()
    }

    override fun onResume() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Online")

        FirebaseDatabase.getInstance().reference.child("presence").child(auth).setValue("Online")
        super.onResume()
    }

    override fun onPause() {
        database.collection(Constants.USERS).document(auth).update("onlineStatus", "Offline")

        FirebaseDatabase.getInstance().reference.child("presence").child(auth).setValue("Offline")
        super.onPause()
    }
}