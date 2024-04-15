package com.example.courseassistantapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
          redirectToProfile(auth.currentUser?.email)
      }
      else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
     }
    }

    private fun redirectToProfile(email: String?) {
        if (email != null) {
            val intent = when {
                email.endsWith("@std.yildiz.edu.tr") -> {
                    Intent(this, StudentAccountActivity::class.java)
                }
                email.endsWith("@yildiz.edu.tr") -> {
                    Intent(this, InstructorAccountActivity::class.java)
                }
                email.endsWith("@admin.com") -> {
                    Intent(this, AdminAccountActivity::class.java)
                }
                else -> {
                    Intent(this, LoginActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        }
    }
}
