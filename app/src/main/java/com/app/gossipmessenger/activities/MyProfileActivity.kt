package com.app.gossipmessenger.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.gossipmessenger.R
import com.app.gossipmessenger.databinding.ActivityMyProfileBinding
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class MyProfileActivity : BaseActivity() {
    private var binding: ActivityMyProfileBinding? = null
    private var mUserImageURL: String = ""
    private var mSelectedImageFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        setupActionBar()

        setUserDataInUI()

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent = result.data!!
                mSelectedImageFileUri = data.data

                try {
                    binding?.myProfilePicture?.let {
                        Glide
                            .with(this)
                            .load(Uri.parse(mSelectedImageFileUri.toString())) // URI of the image
                            .centerCrop() // Scale type of the image.
                            .placeholder(R.drawable.ic_profile_picture_holder) // A default place holder
                            .into(it)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                uploadUserImageAndUserName()
            }
        }

        binding?.clStatus?.setOnClickListener {
            dialogUpdateStatus()
        }

        binding?.clMyProfilePicture?.setOnClickListener {
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

        binding?.llMyName?.setOnClickListener {
            dialogUpdateName()
        }
    }

    private fun setupActionBar() {

        setSupportActionBar(binding?.toolbarMyProfileActivity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
            actionBar.title = resources.getString(R.string.nav_my_profile)
        }

        binding?.toolbarMyProfileActivity?.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setUserDataInUI() {

        database.collection(Constants.USERS).document(auth.currentUser!!.uid).get().addOnSuccessListener {
                document ->
            val user = document.toObject(User::class.java)
            Glide
            .with(this@MyProfileActivity)
            .load(user!!.profilePicture)
            .centerCrop()
            .placeholder(R.drawable.ic_profile_picture_holder)
            .into(binding?.myProfilePicture!!)
            binding?.tvProfileName?.text = user.name

            binding?.tvPhoneNumber?.text = user.phoneNumber

            binding?.tvUserStatus?.text = user.userStatus
        }
    }

    private fun uploadUserImageAndUserName(){
        showCustomProgressDialog()
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
            "PROFILE_PICTURE" + System.currentTimeMillis() + "." + BaseActivity().getFileExtension(this, mSelectedImageFileUri)
        )
        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
            Log.e(
                "Board Image URL",
                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            )

            taskSnapshot.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { uri ->
                    Log.e("Downloadable Image URL", uri.toString())

                    mUserImageURL = uri.toString()

                    addProfileData()
                }
        }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this@MyProfileActivity,
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

        if (binding?.tvProfileName?.toString() != User().name){
            userHashMap[Constants.NAME] = binding?.tvProfileName?.text.toString()
        }

        if (binding?.tvStatusTitle?.toString() != User().userStatus){
            userHashMap[Constants.USER_STATUS] = binding?.tvUserStatus?.text.toString()
        }

        mFirestore.collection(Constants.USERS).document(auth.currentUser!!.uid).update(userHashMap)
        hideProgressDialog()
        Toast.makeText(this, "Profile Data Updated", Toast.LENGTH_SHORT).show()
    }

    private fun dialogUpdateName(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_update_user_name)
        val tvUpdate = dialog.findViewById<TextView>(R.id.tvUpdate)
        val etUserNameUpdate = dialog.findViewById<EditText>(R.id.etUserNameChange)
        val tvCancel = dialog.findViewById<TextView>(R.id.tv_cancelUser)

        tvUpdate.setOnClickListener {
            val userName = etUserNameUpdate.text.toString()
            if (userName.isNotEmpty()){
                dialog.dismiss()
                binding?.tvProfileName?.text = etUserNameUpdate.text
                database.collection(Constants.USERS).document(auth.currentUser!!.uid).update("name", userName)
            }
        }

        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dialogUpdateStatus(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_update_user_status)
        val tvUpdate = dialog.findViewById<TextView>(R.id.tvUpdateStatus)
        val etUserStatusUpdate = dialog.findViewById<EditText>(R.id.etUserStatusChange)
        val tvCancel = dialog.findViewById<TextView>(R.id.tv_cancelStatus)

        tvUpdate.setOnClickListener {
            val userStatus = etUserStatusUpdate.text.toString()
            if (userStatus.isNotEmpty()){
                dialog.dismiss()
                binding?.tvUserStatus?.text = etUserStatusUpdate.text
                database.collection(Constants.USERS).document(auth.currentUser!!.uid).update(Constants.USER_STATUS, userStatus)
            }
        }

        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}