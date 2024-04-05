package com.example.courseassistantapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.login_button)
        val registerButton = findViewById<TextView>(R.id.register_button)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgot_password)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            loginUser(username, password)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(username: String, password: String) {
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        redirectToProfile(user.email)
                    } else {
                        Toast.makeText(this, "Kullanıcı bulunamadı.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Giriş sırasında bir hata oluştu."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun redirectToProfile(email: String?) {
        if (email != null) {
            val intent = when {
                email.endsWith("@std.yildiz.edu.tr") -> {
                    Intent(this, StudentProfileActivity::class.java)
                }
                email.endsWith("@yildiz.edu.tr") -> {
                    Intent(this, InstructorProfileActivity::class.java)
                }
                else -> {
                    Intent(this, AdminProfileActivity::class.java)
                }
            }
            startActivity(intent)
            finish()
        }
    }
}
