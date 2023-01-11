package com.app.gossipmessenger.models

import android.os.Parcel
import android.os.Parcelable

data class User(
    var uId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profilePicture: String = "",
    val friends: ArrayList<String> = ArrayList(),
    val status: ArrayList<Status> = ArrayList(),
    val lastStatusTimeStamp: Long = 0,
    val fcmToken: String = "",
    var userStatus: String = "",
    var onlineStatus:String = ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(phoneNumber)
        parcel.writeString(profilePicture)
        parcel.writeString(uId)
        parcel.writeStringList(friends)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}