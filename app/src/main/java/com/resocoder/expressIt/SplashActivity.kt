package com.resocoder.expressIt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import org.jetbrains.anko.startActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    //determine the sequence for signing in
        if (FirebaseAuth.getInstance().currentUser == null) //if current user has never signed in before
            startActivity<SignInActivity>() //go to signin activity to sign up for an account
        else
            startActivity<MainActivity>() //else, that is user already has an account, go straight to main activity which contains the people and account fragment
        finish()
    }
}
