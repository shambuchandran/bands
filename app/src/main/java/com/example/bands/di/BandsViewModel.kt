package com.example.bands.di

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val chatMessages = mutableStateOf<List<Message>>(listOf())
    var inProgressChatMessages = mutableStateOf(false)
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
//        uid.let {
//            inProgress.value = true
//            db.collection(USER_NODE).document(uid!!).get().addOnSuccessListener {
//                if (it.exists()) {
//                    db.collection(USER_NODE).document(uid).set(userData).addOnSuccessListener {
//                        inProgress.value = false
//                        getUserData(uid)
//                        updateUserDetailsInChatsAndStatus(userData)
//                        updateAvailability(Availability.ONLINE)
//                    }.addOnFailureListener {
//                        handleException(it, customMessage = "Cannot update user")
//                        Log.d("createOrUpdateProfile on success",it.toString())
//                        inProgress.value = false
//                    }
//                } else {
//                    db.collection(USER_NODE).document(uid).set(userData)
//                    inProgress.value = false
//                    getUserData(uid)
//                    updateAvailability(Availability.ONLINE)
//                }
//            }.addOnFailureListener {
//                handleException(it, "Cannot retrieve User")
//                Log.d("createOrUpdateProfile on fail",it.toString())
//                inProgress.value = false
//            }
//        }
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
        val chatMessage = Message(userData.value?.userId, message, time)
        db.collection(CHATS).document(chatId).collection(MESSAGE).document().set(chatMessage)
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

    fun loadMessages(chatId: String) {
        inProgressChatMessages.value = true
        currentChatListener = db.collection(CHATS).document(chatId).collection(MESSAGE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)
                }
                if (value != null) {
                    chatMessages.value = value.documents.mapNotNull {
                        it.toObject<Message>()
                    }.sortedBy { it.timeStamp }
                    inProgressChatMessages.value = false
                }
            }
        subscribeForNotification(chatId)
    }
    fun subscribeForNotification(chatId:String){
        FirebaseMessaging.getInstance().subscribeToTopic("chatGroup_$chatId").addOnCompleteListener {
            if (it.isSuccessful){
                Log.d("viewmodel"," success chatGroup_$chatId")
            }else{
                Log.d("viewmodel"," failed chatGroup_$chatId")
            }
        }
    }
     fun postNotificationToUsers(chatId: String,senderId:String,message: String){
         if (senderId != userData.value?.userId) {
             Log.d("postNotificationToUsers", "Skipping notification for the sender")
             return
         }
         val fcmUrl = "https://fcm.googleapis.com/v1/projects/bands-d1bc1/messages:send"
         val jsonBody=JSONObject().apply {
             put("message",JSONObject().apply {
                 put("topic", "chatGroup_$chatId")
                 put("notification",JSONObject().apply {
                     put("title","New message from $chatId")
                     put("body","$senderId : $message")
                 })
                 put("data", JSONObject().apply {
                     put("senderId", senderId)
                 })
             })
         }
         val requestBody=jsonBody.toString()
         val request= object :StringRequest(Method.POST,fcmUrl,Response.Listener {
             Log.d("volley","msg send success")
         },Response.ErrorListener {
             Log.d("volley","msg send failed: ${it.message}")
         }){
             override fun getBody(): ByteArray {
                 return requestBody.toByteArray()
             }

             override fun getHeaders(): MutableMap<String, String> {
                val  headers = HashMap<String,String>()
                 headers["Authorization"] = "Bearer ${getAccessToken()}"
                 headers["Content-type"] = "application/json"
                 return headers
             }
         }
         val queue=Volley.newRequestQueue(context)
         queue.add(request)
     }
    fun getAccessToken():String{
        val inputStream=context.resources.openRawResource(R.raw.bands_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        return googleCreds.refreshAccessToken().tokenValue

    }

    fun releaseMessages() {
        chatMessages.value = listOf()
        currentChatListener = null
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
        val statusShowTime = 24L * 60 * 60 * 1000
        Log.d("statusShowTime",statusShowTime.toString())
        val timeFrame = System.currentTimeMillis() - statusShowTime
        Log.d("timeFrame",timeFrame.toString())
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
                                    Log.d("loadStatuses value",error.toString())
                                    inProgressStatus.value = false
                                }
                                if (value != null) {
                                    //status.value = value.toObjects()
                                    _status.update { value.toObjects<Status>() }
                                    Log.d("sts",_status.toString())
                                    inProgressStatus.value = false
                                }
                            }
                    }
                } else {
                    inProgressStatus.value = false
                }
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
