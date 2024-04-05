package com.example.courseassistantapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val firstNameEditText = findViewById<EditText>(R.id.first_name)
        val lastNameEditText = findViewById<EditText>(R.id.last_name)
        val studentIdEditText = findViewById<EditText>(R.id.student_id)
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val registerButton = findViewById<Button>(R.id.register_button)

        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val studentId = studentIdEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            registerUser(firstName, lastName, studentId, email, password)
        }
    }

    private fun registerUser(firstName: String, lastName: String, studentId: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Kayıt başarıyla tamamlandı.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Kayıt sırasında bir hata oluştu."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
