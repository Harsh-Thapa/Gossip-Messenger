package com.app.gossipmessenger.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.gossipmessenger.R
import com.app.gossipmessenger.databinding.ActivityUserProfileBinding
import com.bumptech.glide.Glide

class UserProfileActivity : AppCompatActivity() {
    private var binding: ActivityUserProfileBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        val userName = intent.getStringExtra("name")
        val profilePic = intent.getStringExtra("profilePic")
        val userStatus = intent.getStringExtra("userStatus")
        val phoneNumber = intent.getStringExtra("phoneNumber")

        setupActionBar()

        Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_picture_holder).into(binding?.userProfilePicture!!)
        binding?.tvUserProfileName?.text = userName
        binding?.tvUserPhoneNumber?.text = phoneNumber
        binding?.tvUserStatusCurrent?.text = userStatus
    }

    private fun setupActionBar() {

        setSupportActionBar(binding?.toolbarUserProfile)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white)
            actionBar.title = "User Profile"
        }

        binding?.toolbarUserProfile?.setNavigationOnClickListener { onBackPressed() }
    }
}