package com.resocoder.expressIt.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.resocoder.expressIt.model.*
import com.resocoder.expressIt.recyclerview.item.ImageMessageItem
import com.resocoder.expressIt.recyclerview.item.PersonItem
import com.resocoder.expressIt.recyclerview.item.TextMessageItem
import com.xwray.groupie.kotlinandroidextensions.Item


object FirestoreUtil {
    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() } //lazy initialization, values from firebasfirestore.getInstance() will be set in firestoreInstance only when needed
        //hold user information
    private val currentUserDocRef: DocumentReference
        get() = firestoreInstance.document("users/${FirebaseAuth.getInstance().currentUser?.uid
                ?: throw NullPointerException("UID is null.")}")

    private val chatChannelsCollectionRef = firestoreInstance.collection("chatChannels") //stores individual chat channel and all messages

    //user initialization function
    fun initCurrentUserIfFirstTime(onComplete: () -> Unit) {
        currentUserDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                val newUser = User(FirebaseAuth.getInstance().currentUser?.displayName ?: "",
                        "", null, mutableListOf())
                currentUserDocRef.set(newUser).addOnSuccessListener { //set document in firestore to contain new user
                    onComplete()
                }
            }
            else
                onComplete() //complete here if the document snapshot already exists
        }
    }

    //updating user profile
    fun updateCurrentUser(name: String = "", bio: String = "", profilePicturePath: String? = null) {
        val userFieldMap = mutableMapOf<String, Any>()
        if (name.isNotBlank()) userFieldMap["name"] = name
        if (bio.isNotBlank()) userFieldMap["bio"] = bio
        if (profilePicturePath != null)
            userFieldMap["profilePicturePath"] = profilePicturePath
        currentUserDocRef.update(userFieldMap)
    }

    fun getCurrentUser(onComplete: (User) -> Unit) {
        currentUserDocRef.get()
                .addOnSuccessListener {
                    onComplete(it.toObject(User::class.java)!!)
                }
    }

    //listener for all the people in the user collection

    fun addUsersListener(context: Context, onListen: (List<Item>) -> Unit): ListenerRegistration {
        return firestoreInstance.collection("users")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (firebaseFirestoreException != null) {
                        Log.e("FIRESTORE", "Users listener error.", firebaseFirestoreException)
                        return@addSnapshotListener
                    }
                    //create mutable list of items if there is no exceptions
                    val items = mutableListOf<Item>()
                    querySnapshot!!.documents.forEach { //get user from the document snapshot
                        if (it.id != FirebaseAuth.getInstance().currentUser?.uid)  //display users apart from the currently logged in user
                            items.add(PersonItem(it.toObject(User::class.java)!!, it.id, context))
                    }
                    onListen(items)
                }
    }
    //accept user registration
    fun removeListener(registration: ListenerRegistration) = registration.remove()

    fun getOrCreateChatChannel(otherUserId: String,
                               onComplete: (channelId: String) -> Unit) {
        currentUserDocRef.collection("engagedChatChannels") //remember each user's chat channel

                //use otherUserid to get other user's current channel
                .document(otherUserId).get().addOnSuccessListener {
                    if (it.exists()) {
                        onComplete(it["channelId"] as String)
                        return@addOnSuccessListener
                    }


                    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

                    //create chat channel if it doesn't exist
                    val newChannel = chatChannelsCollectionRef.document()
                    newChannel.set(ChatChannel(mutableListOf(currentUserId, otherUserId)))

                    //save the new chat channel for current user
                    currentUserDocRef
                            .collection("engagedChatChannels")
                            .document(otherUserId)
                            .set(mapOf("channelId" to newChannel.id))

                    //save the other user channel id to their engaged chat channels
                    firestoreInstance.collection("users").document(otherUserId)
                            .collection("engagedChatChannels")
                            .document(currentUserId)
                            .set(mapOf("channelId" to newChannel.id))

                    onComplete(newChannel.id)
                }
    }

    //listener for all the messages in the chat channel
    fun addChatMessagesListener(channelId: String, context: Context,
                                onListen: (List<Item>) -> Unit): ListenerRegistration {
        return chatChannelsCollectionRef.document(channelId).collection("messages")
                .orderBy("time")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (firebaseFirestoreException != null) {
                        Log.e("FIRESTORE", "ChatMessagesListener error.", firebaseFirestoreException)
                        return@addSnapshotListener
                    }

                    //create mutable list of items of the messages if there is no exceptions

                    val items = mutableListOf<Item>()
                    querySnapshot!!.documents.forEach {
                        if (it["type"] == MessageType.TEXT)
                            items.add(TextMessageItem(it.toObject(TextMessage::class.java)!!, context)) //add text messages
                        else
                            items.add(ImageMessageItem(it.toObject(ImageMessage::class.java)!!, context)) //add image messages
                        return@forEach
                    }
                    onListen(items)
                }
    }

    fun sendMessage(message: Message, channelId: String) {
        chatChannelsCollectionRef.document(channelId)
                .collection("messages")
                .add(message)
    }

    //region FCM
    fun getFCMRegistrationTokens(onComplete: (tokens: MutableList<String>) -> Unit) {
        currentUserDocRef.get().addOnSuccessListener {
            val user = it.toObject(User::class.java)!!
            onComplete(user.registrationTokens)
        }
    }

    fun setFCMRegistrationTokens(registrationTokens: MutableList<String>) {
        currentUserDocRef.update(mapOf("registrationTokens" to registrationTokens))
    }
    //endregion FCM
}