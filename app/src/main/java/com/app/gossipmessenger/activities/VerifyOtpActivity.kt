package com.app.gossipmessenger.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.app.gossipmessenger.databinding.ActivityVerifyOtpBinding
import com.app.gossipmessenger.models.User
import com.app.gossipmessenger.utils.Constants
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class VerifyOtpActivity : BaseActivity() {
    lateinit var phoneNumber: String
    private var binding: ActivityVerifyOtpBinding? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        phoneNumber = intent.getStringExtra("phone").toString()
        binding?.enteredMobileNumber?.text = "+91-$phoneNumber"


        binding?.btnVerifyOtp?.setOnClickListener {
            binding?.btnVerifyOtp?.visibility = View.GONE
            binding?.verifyOtpProgressBar?.visibility = View.VISIBLE
            validateOtp()

        }
        numberOtpMove()
    }

    private fun validateOtp() {
        val input1 = binding?.inputOtp1?.text.toString()
        val input2 = binding?.inputOtp2?.text.toString()
        val input3 = binding?.inputOtp3?.text.toString()
        val input4 = binding?.inputOtp4?.text.toString()
        val input5 = binding?.inputOtp5?.text.toString()
        val input6 = binding?.inputOtp6?.text.toString()
        if (input1.isNotEmpty() && input2.isNotEmpty() && input3.isNotEmpty() && input4.isNotEmpty() && input5.isNotEmpty() && input6.isNotEmpty()) {
            val verificationId = intent.getStringExtra("verificationId")
            if (verificationId != null){
                val code = input1 + input2 + input3 + input4 + input5 + input6
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                signInWithPhoneAuthCredential(credential)
            }
        } else {
            Toast.makeText(this, "Please enter a valid otp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser!!.uid
                    phoneNumber = intent.getStringExtra("phone").toString()

                    mFirestore.collection(Constants.USERS).document(user).get().addOnCompleteListener { task ->
                        if (task.result.exists()) {
                            val intent = Intent(this@VerifyOtpActivity, AddUserDetailsActivity::class.java)
                            startActivity(intent)
                            finish()
                        }else {
                            mFirestore.collection(Constants.USERS).document(user).set(User(phoneNumber = phoneNumber, uId = auth.currentUser!!.uid))
                            val intent = Intent(this@VerifyOtpActivity, AddUserDetailsActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }


                } else {
                    binding?.btnVerifyOtp?.visibility = View.VISIBLE
                    binding?.verifyOtpProgressBar?.visibility = View.GONE
                    Toast.makeText(this, "Please enter a valid otp", Toast.LENGTH_SHORT).show()

                }
            }
    }

    private fun numberOtpMove() {
        binding?.inputOtp1?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding?.inputOtp2?.requestFocus()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        binding?.inputOtp2?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding?.inputOtp3?.requestFocus()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        binding?.inputOtp3?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding?.inputOtp4?.requestFocus()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        binding?.inputOtp4?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding?.inputOtp5?.requestFocus()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
        binding?.inputOtp5?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().trim().isNotEmpty()){
                    binding?.inputOtp6?.requestFocus()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })
    }

    override fun onBackPressed() {
        doubleBackToExit()
    }
}