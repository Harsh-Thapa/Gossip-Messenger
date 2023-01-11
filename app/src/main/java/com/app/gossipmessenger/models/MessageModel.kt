package com.app.gossipmessenger.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth

data class MessageModel(

    var message: String = "",
    var timeStamp: Long = 0,
    var phoneNumber: String? = FirebaseAuth.getInstance().currentUser!!.phoneNumber,
    var imageUrl: String = ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(message)
        parcel.writeLong(timeStamp)
        parcel.writeString(phoneNumber)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MessageModel> {
        override fun createFromParcel(parcel: Parcel): MessageModel {
            return MessageModel(parcel)
        }

        override fun newArray(size: Int): Array<MessageModel?> {
            return arrayOfNulls(size)
        }
    }
}