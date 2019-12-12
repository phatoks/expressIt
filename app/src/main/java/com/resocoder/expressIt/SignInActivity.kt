package com.resocoder.expressIt

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.iid.FirebaseInstanceId
import com.resocoder.expressIt.service.MyFirebaseInstanceIDService
import com.resocoder.expressIt.util.FirestoreUtil
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.jetbrains.anko.clearTask
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask

class SignInActivity : AppCompatActivity() {

    //request code
    private val RC_SIGN_IN = 1

    //create and define sign in method, in our case only the email sign method is enabled because of simplicity
    private val signInProviders =
            listOf(AuthUI.IdpConfig.EmailBuilder()
                    .setAllowNewAccounts(true) //allow creation of new account
                    .setRequireName(true)  //name is required when signing up for a new account
                    .build())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //action for when the sign in/sign up button is clicked
        account_sign_in.setOnClickListener {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(signInProviders)
                    .setLogo(R.mipmap.ic_yarnlogo)
                    .build()
            startActivityForResult(intent, RC_SIGN_IN) //intialize the sign activity and retrieve the result
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                val progressDialog = indeterminateProgressDialog("Setting up your account") //using the indeterminate Process Dialog, display this message if the credentials is correct
                FirestoreUtil.initCurrentUserIfFirstTime {
                    startActivity(intentFor<MainActivity>().newTask().clearTask()) //flags newTask() and clearTask() used so that we cannot go back to the sign in activity after logging in

                    val registrationToken = FirebaseInstanceId.getInstance().token
                    MyFirebaseInstanceIDService.addTokenToFirestore(registrationToken)

                    progressDialog.dismiss()
                }
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                if (response == null) return

                //if the response gotten from the sign is null then return the error

                when (response.error?.errorCode) {
                    ErrorCodes.NO_NETWORK ->
                            longSnackbar(constraint_layout, "No network") //
                    ErrorCodes.UNKNOWN_ERROR ->
                        longSnackbar(constraint_layout, "Unknown error")
                }
            }
        }
    }
}
