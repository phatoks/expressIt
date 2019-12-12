package com.resocoder.expressIt

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.resocoder.expressIt.model.ImageMessage
import com.resocoder.expressIt.model.TextMessage
import com.resocoder.expressIt.model.User
import com.resocoder.expressIt.util.FirestoreUtil
import com.resocoder.expressIt.util.StorageUtil
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.ByteArrayOutputStream
import java.util.*

private const val RC_SELECT_IMAGE = 2  //request code

class ChatActivity : AppCompatActivity() {

    private lateinit var currentChannelId: String
    private lateinit var currentUser: User
    private lateinit var otherUserId: String

    private lateinit var messagesListenerRegistration: ListenerRegistration
    private var shouldInitRecyclerView = true
    private lateinit var messagesSection: Section

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(AppConstants.USER_NAME)

        FirestoreUtil.getCurrentUser {
            currentUser = it
        }

        otherUserId = intent.getStringExtra(AppConstants.USER_ID)
        FirestoreUtil.getOrCreateChatChannel(otherUserId) { channelId ->
            currentChannelId = channelId

            messagesListenerRegistration =
                    FirestoreUtil.addChatMessagesListener(channelId, this, this::updateRecyclerView)


            //listener for the send button to send text messages after text input
            imageView_send.setOnClickListener {
                val messageToSend =
                        TextMessage(editText_message.text.toString(), Calendar.getInstance().time,
                                FirebaseAuth.getInstance().currentUser!!.uid,
                                otherUserId, currentUser.name)
                editText_message.setText("") //remove message from the edit text after message as being sent
                FirestoreUtil.sendMessage(messageToSend, channelId)  //send text message
            }

            //listener for image floating button to select and send image messages

            fab_send_image.setOnClickListener {
                val intent = Intent().apply {
                    type = "image/*"
                    action = Intent.ACTION_GET_CONTENT
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                }
                startActivityForResult(Intent.createChooser(intent, "Select Image"), RC_SELECT_IMAGE) //start activity to select an image
            }
        }
    }

    //override the activity to select an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SELECT_IMAGE && resultCode == Activity.RESULT_OK &&
                data != null && data.data != null) {
            val selectedImagePath = data.data

            val selectedImageBmp = MediaStore.Images.Media.getBitmap(contentResolver, selectedImagePath)

            //convert the bitmap to a byte array
            val outputStream = ByteArrayOutputStream()

            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val selectedImageBytes = outputStream.toByteArray()  //get actual byte of the umage

            //upload image to cloud storage
            StorageUtil.uploadMessageImage(selectedImageBytes) { imagePath ->
                val messageToSend =
                        ImageMessage(imagePath, Calendar.getInstance().time,
                                FirebaseAuth.getInstance().currentUser!!.uid,
                                otherUserId, currentUser.name)
                FirestoreUtil.sendMessage(messageToSend, currentChannelId)  //send image message
            }
        }
    }

    //updates changes in the message collection in the current chat channels
    private fun updateRecyclerView(messages: List<Item>) {
        fun init() {
            recycler_view_messages.apply {
                layoutManager = LinearLayoutManager(this@ChatActivity)
                adapter = GroupAdapter<ViewHolder>().apply {
                    messagesSection = Section(messages)
                    this.add(messagesSection)
                }
            }
            shouldInitRecyclerView = false
        }

        fun updateItems() = messagesSection.update(messages)

        if (shouldInitRecyclerView)
            init()
        else
            updateItems()

        recycler_view_messages.scrollToPosition(recycler_view_messages.adapter.itemCount - 1)  //scroll message down after it has been sent
    }
}
