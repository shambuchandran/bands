package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.bands.R
import com.example.bands.data.Availability
import com.example.bands.data.CHATS
import com.example.bands.data.ChatData
import com.example.bands.data.ChatUser
import com.example.bands.data.Event
import com.example.bands.data.MESSAGE
import com.example.bands.data.Message
import com.example.bands.data.STATUS
import com.example.bands.data.Status
import com.example.bands.data.USER_NODE
import com.example.bands.data.UserData
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@SuppressLint("StaticFieldLeak")
@HiltViewModel
class BandsViewModel @Inject constructor(
    val auth: FirebaseAuth,
    var db: FirebaseFirestore,
    var storage: FirebaseStorage,
     @ApplicationContext val context: Context,
) : ViewModel(){


    var inProgress = mutableStateOf(false)
    var inProgressChats = mutableStateOf(false)
    var eventMutableState = mutableStateOf<Event<String>?>(null)
    var signIn = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val chats = mutableStateOf<List<ChatData>>(listOf())
    //val chatMessages = mutableStateOf<List<Message>>(listOf())
    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages.asStateFlow()
    var inProgressChatMessages = mutableStateOf(false)
    var lastVisibleMessage: DocumentSnapshot? = null
    var currentChatListener: ListenerRegistration? = null
    var showStickyHeader by mutableStateOf(false)

    //val status = mutableStateOf<List<Status>>(listOf())
    private val _status = MutableStateFlow<List<Status>>(emptyList())
    val status: StateFlow<List<Status>> = _status.asStateFlow()

    //var inProgressStatus = mutableStateOf(false)
    var inProgressStatus = MutableStateFlow(false)

    var verificationId = mutableStateOf("")
    var otpInProgress = mutableStateOf(false)
    var otpSent = mutableStateOf(false)
    var resentToken: PhoneAuthProvider.ForceResendingToken? = null



    init {
        val currentUser = auth.currentUser
        Log.d("bandsView-uid",currentUser?.uid.toString())
        signIn.value = currentUser != null
        currentUser?.uid.let {
            if (it != null) {
                getUserData(it)
            }
        }
    }
    fun toggleStickyHeader() {
        showStickyHeader = !showStickyHeader
    }
    fun signUp(name: String, phoneNumber: String, email: String, password: String) {
        inProgress.value = true
        if (name.isEmpty() or phoneNumber.isEmpty() or email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill all fields")
            return
        }
        inProgress.value = true
        db.collection(USER_NODE).whereEqualTo("phoneNumber", phoneNumber).get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            signIn.value = true
                            createOrUpdateProfile(name, phoneNumber)
                            Log.d("Auth", "user logged IN")
                        } else {
                            handleException(it.exception, customMessage = "SignUp Failed")
                        }
                    }
                } else {
                    handleException(customMessage = "Number already exists")
                    inProgress.value = false
                }
            }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill all fields")
            return
        } else {
            inProgress.value = true
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    signIn.value = true
                    updateAvailability(Availability.ONLINE)
                    inProgress.value = false
                    auth.currentUser?.uid.let {
                        if (it != null) {
                            getUserData(it)
                        }
                    }
                } else {
                    handleException(exception = it.exception, customMessage = "Login failed")
                }
            }
        }

    }

    fun uploadProfileImage(uri: Uri) {
        uploadImage(uri) {
            createOrUpdateProfile(imageUrl = it.toString())
        }
    }

    private fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProgress.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("images/$uuid")
        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl
            result?.addOnSuccessListener(onSuccess)
            inProgress.value = false
        }.addOnFailureListener {
            handleException(it)
        }
    }

    fun createOrUpdateProfile(
        name: String? = null,
        phoneNumber: String? = null,
        imageUrl: String? = null,
        availability: Availability=Availability.OFFLINE,
        city:String? = null
    ) {
        val uid = auth.currentUser?.uid ?:return
        Log.d("bandsView-uid",uid.toString())
        val userData = UserData(
            userId = uid,
            name = name ?: this.userData.value?.name,
            phoneNumber = phoneNumber ?: this.userData.value?.phoneNumber,
            imageUrl = imageUrl ?: this.userData.value?.imageUrl,
            availability= availability,
            city = city
        )
        db.collection(USER_NODE).document(uid).set(userData, SetOptions.merge())
            .addOnSuccessListener {
                inProgress.value = false
                getUserData(uid)
                updateUserDetailsInChatsAndStatus(userData)
                updateAvailability(Availability.ONLINE)
            }
            .addOnFailureListener {
                handleException(it, customMessage = "Cannot update user")
                Log.d("createOrUpdateProfile on fail", it.toString())
                inProgress.value = false
            }
    }

    fun updateUserDetailsInChatsAndStatus(updatedUserData: UserData){
        val userId = updatedUserData.userId
        if (userId != null) {
            db.collection(CHATS).where(
                Filter.or(
                    Filter.equalTo("user1.userId", userId),
                    Filter.equalTo("user2.userId", userId)
                )
            ).get().addOnSuccessListener { documents ->
                for (doc in documents) {
                    val chatData = doc.toObject(ChatData::class.java)
                    val chatRef = db.collection(CHATS).document(doc.id)

                    if (chatData.user1.userId == userId) {
                        chatRef.update(
                            "user1.name", updatedUserData.name,
                            "user1.imageUrl", updatedUserData.imageUrl
                        )
                    } else if (chatData.user2.userId == userId) {
                        chatRef.update(
                            "user2.name", updatedUserData.name,
                            "user2.imageUrl", updatedUserData.imageUrl
                        )
                    }
                }
            }.addOnFailureListener {
                handleException(it, "Failed to update details in chat data")
            }

            db.collection(STATUS)
                .whereEqualTo("user.userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val statusRef = db.collection(STATUS).document(doc.id)
                        statusRef.update(
                            "user.name", updatedUserData.name,
                            "user.imageUrl", updatedUserData.imageUrl
                        )
                    }
                }.addOnFailureListener {
                    handleException(it, "Failed to update details in status data")
                }
        }
    }

    fun logout() {
        updateAvailability(Availability.OFFLINE)
        auth.signOut()
        signIn.value = false
        userData.value = null
        releaseMessages()
        currentChatListener = null
        eventMutableState.value = Event("logout")
    }

    fun onAddChat(phoneNumber: String) {
        if (phoneNumber.isEmpty() ||
            (!phoneNumber.startsWith("+") && !phoneNumber.isDigitsOnly()) ||
            (phoneNumber.startsWith("+") && !phoneNumber.drop(1).isDigitsOnly())) {
            handleException(customMessage = "Enter numbers only")
            Log.d("onAddChat-1", "Failure")
            return
        } else {
            db.collection(CHATS).where(
                Filter.or(
                    Filter.and(
                        (Filter.equalTo("user1.phoneNumber", phoneNumber)),
                        (Filter.equalTo("user2.phoneNumber", userData.value?.phoneNumber))
                    ),
                    Filter.and(
                        (Filter.equalTo("user1.phoneNumber", userData.value?.phoneNumber)),
                        (Filter.equalTo("user2.phoneNumber", phoneNumber))
                    )
                )
            ).get().addOnSuccessListener {
                if (it.isEmpty) {
                    db.collection(USER_NODE).whereEqualTo("phoneNumber", phoneNumber).get()
                        .addOnSuccessListener {
                            if (it.isEmpty) {
                                handleException(customMessage = "Number not found")
                                Log.d("onAddChat0", "Failure")
                            } else {
                                val chatPartner = it.toObjects<UserData>()[0]
                                val id = db.collection(CHATS).document().id

                                val chatData = ChatData(
                                    chatId = id,
                                    user1 = ChatUser(
                                        userData.value?.userId,
                                        userData.value?.name,
                                        userData.value?.phoneNumber,
                                        userData.value?.imageUrl,
                                        userData.value?.city
                                    ),
                                    user2 = ChatUser(
                                        chatPartner.userId,
                                        chatPartner.name,
                                        chatPartner.phoneNumber,
                                        chatPartner.imageUrl,
                                        chatPartner.city
                                    )
                                )
                                db.collection(CHATS).document(id).set(chatData)
                            }
                        }.addOnFailureListener {
                            handleException(it)
                            Log.d("onAddChat1", "Failure")
                        }
                } else {
                    handleException(customMessage = "Contact already exists")
                }
            }.addOnFailureListener {
                handleException(it)
                Log.d("onAddChat2", "Failure")
            }
        }

    }

    fun loadChat() {
        inProgressChats.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)
                Log.d("loadChat",error.toString())

            }
            if (value != null) {
                chats.value = value.documents.mapNotNull {
                    it.toObject<ChatData>()
                }
                inProgressChats.value = false
            }

        }
    }
    fun deleteChatById(chatId: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).delete().await()
                chats.value = chats.value.filterNot { it.chatId == chatId }
            } catch (e: Exception) {
                Log.e("BandsViewModel", "Error deleting chat: ${e.message}")
            }
        }
    }


    fun deleteAllMessagesInChat(chatId: String) {
        inProgressChats.value = true
        val chatMessagesRef = db.collection("chats").document(chatId).collection("message")
        chatMessagesRef.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    document.reference.delete()
                }
                _chatMessages.value = emptyList()
                eventMutableState.value = Event("All chats deleted successfully")
            }
            .addOnFailureListener { exception ->
                eventMutableState.value = Event("Error deleting chats: ${exception.message}")
            }
            .addOnCompleteListener {
                inProgressChats.value = false
            }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true
        db.collection(USER_NODE).document(uid).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error, "Cannot retrieve User")
                Log.d("getUserData on error",error.toString())
            }
            if (value != null) {
                val user = value.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                updateAvailability(Availability.ONLINE)
                loadChat()
                Log.d("loadchat",loadChat().toString())
                loadStatuses()
                Log.d("loadchat",loadStatuses().toString())
            }
        }
    }

    fun onSendReply(chatId: String, message: String) {
        val time = Calendar.getInstance().time.toString()
        val messageId = UUID.randomUUID().toString()
        val chatMessage = Message(id = messageId,userData.value?.userId, message, time)
        _chatMessages.value += chatMessage
        db.collection(CHATS).document(chatId).collection(MESSAGE).document(messageId).set(chatMessage)
            .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val senderId = chatMessage.sendBy
                val messageText = chatMessage.message
                if (senderId != null && messageText != null) {
                    postNotificationToUsers(chatId, senderId, messageText)
                } else {
                    Log.w("onSendReply", "Message or sender ID is null. Notification not sent.")
                }
            } else {
                task.exception?.let { exception ->
                    Log.e("onSendReply", "Failed to send message: ${exception.message}")
                }
            }
        }

    }

    fun addReactionToMessage(chatId: String,messageId: String, emoji: String) {
        val messageToUpdate = chatMessages.value.find { it.id == messageId }
        messageToUpdate?.let {
            if (it.reactions.contains(emoji)) {
                it.reactions.remove(emoji)
            } else {
                it.reactions.add(emoji)
            }
            _chatMessages.value = chatMessages.value.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(reactions = it.reactions)
                } else {
                    msg
                }
            }
            db.collection(CHATS).document(chatId)
                .collection(MESSAGE).document(it.id)
                .update("reactions", it.reactions)
                .addOnSuccessListener {
                    Log.d("addReactionToMessage", "Reaction updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("addReactionToMessage", "Error updating reaction: ${e.message}")
                }
        }
    }
    private suspend fun uploadImage(imageUri: Uri): String? {
        return try {
            val storageRef = storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            Log.e("uploadImage", "Failed to upload image: ${e.message}")
            null
        }
    }
    fun sendImageMessage(chatId: String, imageUri: Uri,caption:String? = null) {
        viewModelScope.launch {
            try {
                val imageUrl = uploadImage(imageUri)
                if (imageUrl != null) {
                    val messageId = UUID.randomUUID().toString()
                    val timeStamp = Calendar.getInstance().time.toString()
                    val imageMessage = Message(id = messageId, sendBy = userData.value?.userId, imageUrl = imageUrl, timeStamp = timeStamp, caption = caption)
                    _chatMessages.value += imageMessage
                    db.collection(CHATS).document(chatId).collection(MESSAGE).document(messageId).set(imageMessage)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Log.e("sendImageMessage", "Failed to send image message")
                            }
                        }
                } else {
                    Log.e("sendImageMessage", "Failed to upload image")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e("sendImageMessage", "Error sending image message: ${e.message}")
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                db.collection(CHATS).document(chatId).collection(MESSAGE).document(messageId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val message = document.toObject(Message::class.java)
                            message?.imageUrl?.let { imageUrl ->
                                deleteImageFromFirebaseStorage(imageUrl)
                            }
                            db.collection(CHATS).document(chatId).collection(MESSAGE).document(messageId).delete()
                                .addOnSuccessListener {
                                    _chatMessages.value = _chatMessages.value.filter { it.id != messageId }
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("deleteMessage", "Error deleting message: ${e.message}")
                                    Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.e("deleteMessage", "Message not found")
                            Toast.makeText(context, "Message not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("deleteMessage", "Error retrieving message: ${e.message}")
                        Toast.makeText(context, "Failed to retrieve message", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("deleteMessage", "Error deleting message: ${e.message}")
                Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show()
            }
        }
    }
//    fun loadMessages(chatId: String, limit: Long = 50) {
//        inProgressChatMessages.value = true
//        currentChatListener?.remove()
//        var query = db.collection(CHATS).document(chatId).collection(MESSAGE)
//            .orderBy("timeStamp", Query.Direction.DESCENDING)
//            .limit(limit)
//        lastVisibleMessage?.let {
//            query = query.startAfter(it)
//        }
//        query.get().addOnSuccessListener { snapshot ->
//            inProgressChatMessages.value = false
//
//            if (!snapshot.isEmpty) {
//                val messages = snapshot.documents.mapNotNull {
//                    it.toObject<Message>()
//                }.sortedBy { it.timeStamp }
//                _chatMessages.value = messages
//                lastVisibleMessage = snapshot.documents.last()
//            }
//        }.addOnFailureListener { error ->
//            inProgressChatMessages.value = false
//            handleException(error)
//        }
//        subscribeForNotification(chatId)
//    }
fun loadMessages(chatId: String) {
    inProgressChatMessages.value = true
    currentChatListener = db.collection(CHATS).document(chatId).collection(MESSAGE)
        .addSnapshotListener { value, error ->
            inProgressChatMessages.value = false
            if (error != null) {
                handleException(error)
            }
            if (value != null) {
                _chatMessages.value = value.documents.mapNotNull {
                    it.toObject<Message>()
                }.sortedBy { it.timeStamp }
            }
        }
    subscribeForNotification(chatId)
}

    fun subscribeForNotification(chatId:String){
        FirebaseMessaging.getInstance().subscribeToTopic("chatGroup_$chatId").addOnCompleteListener {
            if (it.isSuccessful){
                Log.d("Notification"," success chatGroup_$chatId")
            }else{
                Log.d("Notification"," failed chatGroup_$chatId")
            }
        }
    }
     fun postNotificationToUsers(chatId: String,senderId:String,message: String){
         if (senderId != userData.value?.userId) {
             Log.d("Notification", "Skipping notification for the sender")
             return
         }
         db.collection(USER_NODE).document(senderId).get().addOnSuccessListener { document ->
             val senderName = document.getString("name") ?: "Unknown User"
             val senderPhone = document.getString("phoneNumber") ?: "Unknown Number"
             val fcmUrl = "https://fcm.googleapis.com/v1/projects/bands-d1bc1/messages:send"
             val jsonBody = JSONObject().apply {
                 put("message", JSONObject().apply {
                     put("topic", "chatGroup_$chatId")
                     put("notification", JSONObject().apply {
                         put("title", "New message from $senderName")
                         val senderDisplayName = senderName.ifBlank { senderPhone }
                         put("body", "$senderDisplayName : $message")
                     })
                     put("data", JSONObject().apply {
                         put("senderId", senderId)
                     })
                 })
             }
             val requestBody = jsonBody.toString()
             val request = object : StringRequest(Method.POST, fcmUrl, Response.Listener {
                 Log.d("volley Notification", "Message sent successfully")
             }, Response.ErrorListener {
                 Log.d("volley Notification", "Message send failed: ${it.message}")
             }) {
                 override fun getBody(): ByteArray {
                     return requestBody.toByteArray()
                 }
                 override fun getHeaders(): MutableMap<String, String> {
                     return hashMapOf(
                         "Authorization" to "Bearer ${getAccessToken()}",
                         "Content-type" to "application/json"
                     )
                 }
             }
             val queue = Volley.newRequestQueue(context)
             queue.add(request)
         }.addOnFailureListener { e ->
             Log.e("postNotificationToUsers", "Failed to retrieve sender info", e)
         }
     }
    fun getAccessToken():String{
        val inputStream=context.resources.openRawResource(R.raw.bands_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        googleCreds.refreshIfExpired()
        return googleCreds.refreshAccessToken().tokenValue

    }

    fun releaseMessages() {
        _chatMessages.value = emptyList()
        currentChatListener?.remove()
        lastVisibleMessage = null
    }

    fun handleException(exception: Exception? = null, customMessage: String? = null) {
        Log.e("Bandsapp", "exception", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isNullOrEmpty()) errorMsg else customMessage
        eventMutableState.value = Event(message)
        inProgress.value = false
    }

    fun uploadStatus(uri: Uri) {
        uploadImage(uri) {
            createStatus(it.toString())
        }
    }

    fun createStatus(imageUrl: String?) {
        val newStatus = Status(
            ChatUser(
                userData.value?.userId,
                userData.value?.name,
                userData.value?.imageUrl,
                userData.value?.phoneNumber,
            ),
            imageUrl,
            System.currentTimeMillis()
        )
        db.collection(STATUS).document().set(newStatus)
    }

    fun loadStatuses() {
//        val statusShowTime = 24L * 60 * 60 * 1000
        inProgressStatus.value = true
        db.collection(CHATS)
            .where(
                Filter.or(
                    Filter.equalTo("user1.userId", userData.value?.userId),
                    Filter.equalTo("user2.userId", userData.value?.userId)
                )
            ).addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)
                    Log.d("loadStatuses error",error.toString())
                    inProgressStatus.value = false
                    return@addSnapshotListener
                }
                if (value != null) {
                    val currentConnection = arrayListOf(userData.value?.userId)
                    val chats = value.toObjects<ChatData>()
                    chats.forEach { chat ->
                        if (chat.user1.userId == userData.value?.userId) {
                            currentConnection.add(chat.user2.userId)
                        } else {
                            currentConnection.add(chat.user1.userId)
                        }
                        Log.d("current",currentConnection.toString())
                        db.collection(STATUS)
                            //.whereGreaterThan("timeStamp", timeFrame)
                            .whereIn("user.userId", currentConnection)
                            .addSnapshotListener { value, error ->
                                if (error != null) {
                                    handleException(error)
                                    Log.d("statusShowTime loadStatuses value",error.toString())
                                    inProgressStatus.value = false
                                    return@addSnapshotListener
                                }
                                if (value != null) {
                                    val currentTime = System.currentTimeMillis()
                                    val statusesToDelete = mutableListOf<DocumentReference>()
                                    val imagesToDelete = mutableListOf<String>()
                                    for (document in value.documents) {
                                        val status = document.toObject(Status::class.java)
                                        if (status != null && status.timeStamp != null && (currentTime - status.timeStamp) >  24L * 60 * 60 * 1000) {
                                            statusesToDelete.add(document.reference)
                                            status.imageUrl?.let { imagesToDelete.add(it) }
                                        }
                                    }
                                    if (statusesToDelete.isNotEmpty()) {
                                        for (i in statusesToDelete.indices) {
                                            val statusRef = statusesToDelete[i]
                                            if (i < imagesToDelete.size) {
                                                deleteImageFromFirebaseStorage(imagesToDelete[i])
                                            }
                                            statusRef.delete()
                                                .addOnSuccessListener {
                                                    Log.d("Delete Status", "Status document ${statusRef.id} deleted successfully.")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("Delete Status", "Error deleting status document ${statusRef.id}", e)
                                                }
                                        }
                                    }
                                    //status.value = value.toObjects()
                                    _status.update { value.toObjects<Status>() }
                                    Log.d("statusShowTime","$value")
                                    inProgressStatus.value = false
                                }
                            }
                    }
                } else {
                    inProgressStatus.value = false
                }
            }
    }
    private fun deleteImageFromFirebaseStorage(url: String) {
        val imageRef: StorageReference = storage.getReferenceFromUrl(url)
        imageRef.delete()
            .addOnSuccessListener {
                Log.d("Delete Image", "Image deleted successfully from Firebase Storage.")
            }
            .addOnFailureListener { exception ->
                Log.w("Delete Image", "Error deleting image: ", exception)
            }
    }

    fun removeStatus(userId: String,index: Int) {
        val userStatuses = _status.value.filter { it.user.userId == userId }
        if (index in userStatuses.indices) {
            val selectedStatus = userStatuses[index]
            deleteStatusFromFirebase(selectedStatus)
            _status.value = _status.value.toMutableList().apply {
                //removeAt(index)
                remove(selectedStatus)
            }
        }
    }

    private fun deleteStatusFromFirebase(selectedStatus: Status) {
        val statusCollection = db.collection(STATUS)
        statusCollection.whereEqualTo("imageUrl", selectedStatus.imageUrl)
            .whereEqualTo("user.userId", selectedStatus.user.userId)
            .whereEqualTo("timeStamp", selectedStatus.timeStamp)
            .get().addOnSuccessListener { documents ->
                for (doc in documents) {
                    statusCollection.document(doc.id).delete().addOnSuccessListener {
                        Log.d("BandsViewModel", "Status successfully deleted!")
                        selectedStatus.imageUrl?.let {
                            deleteImageFromFirebaseStorage(it)
                        }
                    }.addOnFailureListener {
                        handleException(it)
                        Log.w("BandsViewModel", "Error deleting status", it)
                    }
                }
            }.addOnFailureListener {
                handleException(it)
                Log.w("BandsViewModel", "Error finding status to delete", it)
            }

    }

    private val verificationCallBacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                otpInProgress.value = false
                handleException(e)
                Log.e("PhoneAuth", "Verification Failed: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@BandsViewModel.verificationId.value = verificationId
                resentToken = token
                otpInProgress.value = false
                otpSent.value = true
                Log.d("PhoneAuth", "Code sent: $verificationId")
            }

        }

    fun verifyOtp(otp: String) {
        Log.d("PhoneAuthOtp", "Verification ID: ${verificationId.value}, OTP: $otp")
        if (verificationId.value.isEmpty() || otp.isEmpty()) {
            handleException(exception = null,customMessage = "Verification ID or OTP is empty")
            Log.d("Verification ID or OTP is empty","otp empty or verifi value empty")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId.value, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val phoneNumber = user?.phoneNumber
                Log.d("PhoneAuth", "SignIn success!")
                checkUserExists(phoneNumber)
            } else {
                handleException(task.exception)
                Log.e("PhoneAuth", "SignIn failed: ${task.exception?.message}")
            }
        }

    }

    private fun checkUserExists(phoneNumber: String?) {
        val userRef = FirebaseFirestore.getInstance().collection(USER_NODE)
        userRef.whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { document ->
                if (document.isEmpty) {
                    createOrUpdateProfile(phoneNumber = phoneNumber)
                } else {
                    val userId = document.documents.first().id
                    getUserData(userId)
                }
            }.addOnFailureListener { e ->
                handleException(e)
            }

    }


    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(verificationCallBacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        otpInProgress.value = true
    }

    fun resendVerificationCode(phoneNumber: String, activity: Activity) {
        if (resentToken == null) {
            handleException(customMessage = "No token available for resending code")
            return
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(verificationCallBacks)
            .setForceResendingToken(resentToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
        otpInProgress.value = true
    }
    fun updateAvailability(availability: Availability) {
        val uid = auth.currentUser?.uid ?: return
        val update = mapOf("availability" to availability)

        db.collection(USER_NODE).document(uid).update(update)
            .addOnSuccessListener {
                userData.value?.availability = availability
                Log.d("BandsViewModel", "User availability updated to $availability")
            }
            .addOnFailureListener {
                handleException(it, customMessage = "Failed to update availability")
            }
    }

}
