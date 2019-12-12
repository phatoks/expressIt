package com.resocoder.expressIt.fragment


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI

import com.resocoder.expressIt.R
import com.resocoder.expressIt.SignInActivity
import com.resocoder.expressIt.glide.GlideApp
import com.resocoder.expressIt.util.FirestoreUtil
import com.resocoder.expressIt.util.StorageUtil
import kotlinx.android.synthetic.main.fragment_my_account.*
import kotlinx.android.synthetic.main.fragment_my_account.view.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.newTask
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.support.v4.toast
import java.io.ByteArrayOutputStream


class MyAccountFragment : Fragment() {

    private val RC_SELECT_IMAGE = 2  //requestcode
    private lateinit var selectedImageBytes: ByteArray //keep track of selected image
    private var pictureJustChanged = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_my_account, container, false)
        //action for clicking profile picture
        view.apply {
            imageView_profile_picture.setOnClickListener {
                val intent = Intent().apply {
                    type = "image/*"
                    action = Intent.ACTION_GET_CONTENT
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                }
                startActivityForResult(Intent.createChooser(intent, "Select Image"), RC_SELECT_IMAGE)
            }


            //action for clicking save button, check to see if there has been any change to the picture, name or bio before saving
            btn_save.setOnClickListener {
                if (::selectedImageBytes.isInitialized)
                    StorageUtil.uploadProfilePhoto(selectedImageBytes) { imagePath ->
                        FirestoreUtil.updateCurrentUser(editText_name.text.toString(),
                                editText_bio.text.toString(), imagePath)
                    }
                else
                    FirestoreUtil.updateCurrentUser(editText_name.text.toString(),
                            editText_bio.text.toString(), null)
                toast("Saving")
            }
            //action for clicking signout
            btn_sign_out.setOnClickListener {
                AuthUI.getInstance()  //firebase Auth
                        .signOut(this@MyAccountFragment.context!!)
                        .addOnCompleteListener {
                            startActivity(intentFor<SignInActivity>().newTask().clearTask()) //when user is signed go to the signactivity
                        }
            }
        }

        return view
    }
    //when image is selected
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SELECT_IMAGE && resultCode == Activity.RESULT_OK &&
                data != null && data.data != null) {
            val selectedImagePath = data.data
            val selectedImageBmp = MediaStore.Images.Media
                    .getBitmap(activity?.contentResolver, selectedImagePath)

            val outputStream = ByteArrayOutputStream()
            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            selectedImageBytes = outputStream.toByteArray()

            //load pictures from firebase cloud storage
            GlideApp.with(this)
                    .load(selectedImageBytes)
                    .into(imageView_profile_picture)

            pictureJustChanged = true
        }
    }

    override fun onStart() {
        super.onStart()
        FirestoreUtil.getCurrentUser { user -> //load user from firestore
            if (this@MyAccountFragment.isVisible) {
                editText_name.setText(user.name)
                editText_bio.setText(user.bio)
               if (!pictureJustChanged && user.profilePicturePath != null)
                    GlideApp.with(this)
                            .load(StorageUtil.pathToReference(user.profilePicturePath))
                            .placeholder(R.drawable.ic_account_circle_black_24dp)
                            .into(imageView_profile_picture)
            }
        }
    }

}
