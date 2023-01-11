package com.app.gossipmessenger.models

data class Chats(
    var lastMessage: String = "",
    var lastMessageTime: Long = 0,
    var messages: ArrayList<MessageModel> = ArrayList()
)