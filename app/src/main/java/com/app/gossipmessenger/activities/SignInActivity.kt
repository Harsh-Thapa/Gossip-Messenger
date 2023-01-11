package com.app.gossipmessenger.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.app.gossipmessenger.R
import com.app.gossipmessenger.databinding.ActivitySignInBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SignInActivity : BaseActivity() {
    private lateinit var phoneNumber: String
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private var binding: ActivitySignInBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySignInBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)

        binding?.btnGetOtp?.setOnClickListener {
            phoneNumber = binding?.phoneNumber?.text.toString()
            if (phoneNumber.isNotEmpty()){
                if (phoneNumber.length == 10){
                    dialogEditNumber()
                }else{
                    Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                }
            }else {
                Toast.makeText(this, "Mobile number cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        doubleBackToExit()
    }

    @SuppressLint("SetTextI18n")
    private fun dialogEditNumber(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_check_number)
        val tvEdit = dialog.findViewById<TextView>(R.id.tv_edit)
        val tvYes = dialog.findViewById<TextView>(R.id.tv_Yes)
        val tvEditNumber = dialog.findViewById<TextView>(R.id.tvEditNumber)
        tvEditNumber.text = "+91-" + binding?.phoneNumber?.text.toString()

        tvYes.setOnClickListener {
            binding?.sendOtpProgressBar?.visibility = View.VISIBLE
            binding?.btnGetOtp?.visibility = View.GONE
            sendOtp()
            dialog.dismiss()
        }
        tvEdit.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun sendOtp(){
        phoneNumber = binding?.phoneNumber?.text.toString()
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {

            }

            override fun onVerificationFailed(e: FirebaseException) {
                binding?.sendOtpProgressBar?.visibility = View.GONE
                binding?.btnGetOtp?.visibility = View.VISIBLE
                Toast.makeText(this@SignInActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }

            @SuppressLint("SetTextI18n")
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                val intent = Intent(this@SignInActivity, VerifyOtpActivity::class.java)
                intent.putExtra("phone", phoneNumber)
                intent.putExtra("verificationId", verificationId)
                startActivity(intent)
                finish()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber("+91$phoneNumber").setTimeout(60L, TimeUnit.SECONDS).setActivity(this).setCallbacks(callbacks).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}