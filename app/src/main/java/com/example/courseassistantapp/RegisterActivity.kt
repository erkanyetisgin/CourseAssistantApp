package com.example.courseassistantapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val firstNameEditText = findViewById<EditText>(R.id.first_name)
        val lastNameEditText = findViewById<EditText>(R.id.last_name)
        val studentIdEditText = findViewById<EditText>(R.id.student_id)
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val dobEditText = findViewById<EditText>(R.id.dob)
        val educationSpinner = findViewById<Spinner>(R.id.education_spinner)
        val registerButton = findViewById<Button>(R.id.register_button)
        val phone_number = findViewById<EditText>(R.id.phone_number)

        val educationLevels = arrayOf("Eğitim Durumu",  "Lisans","Önlisans", "Yüksek Lisans", "Doktora")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, educationLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        educationSpinner.adapter = adapter

        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val studentId = studentIdEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val dob = dobEditText.text.toString()
            val phone = phone_number.text.toString()

            val selectedEducation = educationLevels[educationSpinner.selectedItemPosition]

            registerUser(firstName, lastName, studentId, email, password, dob, selectedEducation, phone)
        }

    }

    private fun registerUser(firstName: String, lastName: String, studentId: String, email: String, password: String, dob: String, education: String, phone: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && email.contains("@std.yildiz.edu.tr") ){
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "studentId" to studentId,
                            "email" to email,
                            "date" to dob,
                            "education" to education,
                            "phone" to phone

                        )
                        db.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    baseContext, "Kullanıcı başarıyla kaydedildi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    baseContext, "Kullanıcı kaydedilirken bir hata oluştu: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                else if (task.isSuccessful && email.contains("@yildiz.edu.tr") ){
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "date" to dob,
                            "phone" to phone

                        )
                        db.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    baseContext, "Kullanıcı başarıyla kaydedildi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    baseContext, "Kullanıcı kaydedilirken bir hata oluştu: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                else if (email.contains("@std.yildiz.edu.tr") || email.contains("@yildiz.edu.tr") == false){
                    Toast.makeText(
                        baseContext, "Lütfen geçerli bir Yıldız Teknik Üniversitesi e-posta adresi giriniz.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else {
                    Toast.makeText(
                        baseContext, "Kullanıcı oluşturulurken bir hata oluştu: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
