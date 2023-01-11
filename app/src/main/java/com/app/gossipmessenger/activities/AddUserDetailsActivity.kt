package com.app.gossipmessenger.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.gossipmessenger.R
import com.app.gossipmessenger.databinding.ActivityAddUserDetailsBinding
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class AddUserDetailsActivity : BaseActivity() {
    private var mUserImageURL: String = ""
    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails: User
    private var binding: ActivityAddUserDetailsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAddUserDetailsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)
        loadUserData()

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                mSelectedImageFileUri = data!!.data

                try {
                    binding?.civProfilePicture?.let {
                        Glide
                            .with(this)
                            .load(Uri.parse(mSelectedImageFileUri.toString()))
                            .centerCrop()
                            .placeholder(R.drawable.ic_profile_picture_holder)
                            .into(it)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        binding?.civProfilePicture?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent()
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "image/*"
                resultLauncher.launch(intent)
            }else{
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), Constants.READ_STORAGE_PERMISSION_CODE)
            }
        }

        binding?.btnGoToApp?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val userName = binding?.etUserName?.text.toString()
            if (mSelectedImageFileUri != null && userName.isNotEmpty()){
                uploadUserImageAndUserName()
                startActivity(intent)
                finish()
            }else if (mSelectedImageFileUri == null && userName.isNotEmpty()){
                addProfileData()
                startActivity(intent)
                finish()
            }else {
                Toast.makeText(this, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        doubleBackToExit()
    }

    private fun uploadUserImageAndUserName(){
        showCustomProgressDialog()
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
            "PROFILE_PICTURE" + System.currentTimeMillis() + "." + BaseActivity().getFileExtension(this, mSelectedImageFileUri)
        )
        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->

            taskSnapshot.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { uri ->

                    mUserImageURL = uri.toString()

                    addProfileData()
                }
        }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this@AddUserDetailsActivity,
                    exception.message,
                    Toast.LENGTH_LONG
                ).show()

                hideProgressDialog()
            }
    }

    private fun addProfileData() {

        val userHashMap = HashMap<String, Any>()

        if (mUserImageURL.isNotEmpty() && mUserImageURL != User().profilePicture) {
            userHashMap[Constants.PROFILE_PICTURE] = mUserImageURL
        }

        if (binding?.etUserName?.toString() != User().name){
            userHashMap[Constants.NAME] = binding?.etUserName?.text.toString()
        }

        if (binding?.etUserStatus?.toString() != User().userStatus){
            userHashMap[Constants.USER_STATUS] = binding?.etUserStatus?.text.toString()
        }

        mFirestore.collection(Constants.USERS).document(auth.currentUser!!.uid).update(userHashMap)
    }

    private fun setUserDataInUI(user: User) {

        mUserDetails = user

        Glide
            .with(this@AddUserDetailsActivity)
            .load(user.profilePicture)
            .centerCrop()
            .placeholder(R.drawable.ic_profile_picture_holder)
            .into(binding?.civProfilePicture!!)

        binding?.etUserName?.setText(user.name)

        binding?.etUserStatus?.setText(user.userStatus)
    }

    private fun loadUserData(){
        mFirestore.collection(Constants.USERS).document(auth.currentUser!!.uid).get().addOnSuccessListener {
            document ->
            val loggedInUser = document.toObject(User::class.java)
            if (loggedInUser != null) {
                setUserDataInUI(loggedInUser)
            }
        }
    }
}