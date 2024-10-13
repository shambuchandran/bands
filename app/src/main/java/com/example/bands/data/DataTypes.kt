package com.example.bands.data

enum class Availability{
    ONLINE,OFFLINE,IN_CALL
}
data class UserData(
    var userId: String? = null,
    var name: String? = null,
    var phoneNumber: String? = null,
    var imageUrl: String? = null,
    var availability: Availability = Availability.OFFLINE
)
data class ChatData(
    val chatId :String? ="",
    val user1:ChatUser =ChatUser(),
    val user2:ChatUser =ChatUser()
)
data class ChatUser(
    val userId: String? = "",
    val name :String? = "",
    val phoneNumber :String? = "",
    val imageUrl: String? = ""
)
data class Message(
    val sendBy: String? = "",
    val message :String? = "",
    val timeStamp :String? = "",
)
data class Status(
    val user: ChatUser = ChatUser(),
    val imageUrl: String? = null,
    val timeStamp :Long? = null,
)
data class MessageModel(
    val type:String,
    val name:String?=null,
    val target:String?=null,
    val data:Any?=null
)
data class IceCandidateModel(
    val sdpMid:String,
    val sdpMLineIndex:Double,
    val sdpCandidate:String
)


