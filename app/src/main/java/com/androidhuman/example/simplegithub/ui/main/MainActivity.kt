package com.androidhuman.example.simplegithub.ui.main

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.androidhuman.example.simplegithub.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnActivityMainSearch.requestFocus()
        btnActivityMainSearch.setOnClickListener {
//            startActivity(Intent(this@MainActivity, SearchActivity))
        }
    }
}