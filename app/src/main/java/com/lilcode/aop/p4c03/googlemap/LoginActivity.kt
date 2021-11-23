package com.lilcode.aop.p4c03.googlemap

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.et_id
import kotlinx.android.synthetic.main.activity_login.et_pw

class LoginActivity : AppCompatActivity() {

    private var firebaseAuth : FirebaseAuth? = null
    var gel=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val registerIntent = Intent(this, RegisterActivity::class.java)
        val mainIntent = Intent(this, MainActivity::class.java)

        firebaseAuth = FirebaseAuth.getInstance()

        bt_login.setOnClickListener(){
            if(et_id.getText().toString().equals("") || et_id.getText().toString() == null){
                startActivity(mainIntent)
            }
            loginEmail()
            startActivity(mainIntent)
        }

        bt_goregister.setOnClickListener(){
            startActivity(registerIntent)
        }
    }
    private fun loginEmail(){
        firebaseAuth!!.signInWithEmailAndPassword(et_id.text.toString(),et_pw.text.toString()).addOnCompleteListener(this){
            if(it.isSuccessful){
                val user =firebaseAuth?.currentUser
                Toast.makeText(this,"Login success", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Login fail", Toast.LENGTH_SHORT).show()
            }
        }
    }
}