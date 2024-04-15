package com.example.courseassistantapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val email=findViewById<EditText>(R.id.email)
        val resetButton=findViewById<Button>(R.id.password_reset_button)

        resetButton.setOnClickListener {
            val emailText=email.text.toString()
            FirebaseAuth.getInstance().sendPasswordResetEmail(emailText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this,"Şifre sıfırlama maili gönderildi",Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this,"Hata: ${task.exception?.message}",Toast.LENGTH_SHORT).show()
                    }
                }

        }



    }
}