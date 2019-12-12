package com.resocoder.expressIt

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.resocoder.expressIt.fragment.MyAccountFragment
import com.resocoder.expressIt.fragment.PeopleFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //create the default fragment.Immediately show list of user
        replaceFragment(PeopleFragment())


        //when the people or account button is selected, show the appropraite fragment(people fragment or account fragment)
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_people -> {
                    replaceFragment(PeopleFragment()) //show people fragment
                    true
                }
                R.id.navigation_my_account -> {
                    replaceFragment(MyAccountFragment()) //show my account fragment
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_layout, fragment)
                .commit()
    }
}
