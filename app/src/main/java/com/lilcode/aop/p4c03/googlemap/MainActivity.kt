package com.lilcode.aop.p4c03.googlemap

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lilcode.aop.p4c03.googlemap.activity.AddActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        main_addmark.setOnClickListener {
            val intent = Intent(this, MapSearchActivity::class.java)
            startActivity(intent)
        }
    }
}