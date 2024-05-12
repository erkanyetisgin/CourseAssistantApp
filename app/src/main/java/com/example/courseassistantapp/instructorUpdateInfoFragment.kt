package com.example.courseassistantapp

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class instructorUpdateInfoFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_instructor_update_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser!!

        val firstNameEditText = view.findViewById<EditText>(R.id.edit_first_name)
        val lastNameEditText = view.findViewById<EditText>(R.id.edit_last_name)
        val phoneEditText = view.findViewById<EditText>(R.id.edit_phone)
        val dobEditText = view.findViewById<EditText>(R.id.edit_dob)
        val updateButton = view.findViewById<Button>(R.id.btn_update_info)

        displayUserInfo(firstNameEditText, lastNameEditText, phoneEditText, dobEditText)

        updateButton.setOnClickListener {
            val newFirstName = firstNameEditText.text.toString()
            val newLastName = lastNameEditText.text.toString()
            val newPhone = phoneEditText.text.toString()
            val newDOB = dobEditText.text.toString()

            updateUserProfile(newFirstName, newLastName, newPhone, newDOB)
        }
    }

    private fun displayUserInfo(
        firstNameEditText: EditText,
        lastNameEditText: EditText,
        phoneEditText: EditText,
        dobEditText: EditText
    ) {
        db.collection("instructors").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName")
                val lastName = document.getString("lastName")
                val phone = document.getString("phone")
                val dob = document.getString("date")


                firstNameEditText.setText(firstName)
                lastNameEditText.setText(lastName)
                phoneEditText.setText(phone)
                dobEditText.setText(dob)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Bilgiler alınırken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfile(
        newFirstName: String,
        newLastName: String,
        newPhone: String,
        newDOB: String
    ) {
        val userRef = db.collection("instructors").document(currentUser.uid)

        if (newFirstName.isNotEmpty()) {
            userRef.update("firstName", newFirstName)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Ad güncellendi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Ad güncellenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (newLastName.isNotEmpty()) {
            userRef.update("lastName", newLastName)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Soyad güncellendi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Soyad güncellenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (newPhone.isNotEmpty()) {
            userRef.update("phone", newPhone)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Telefon numarası güncellendi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Telefon numarası güncellenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        if (newDOB.isNotEmpty()) {
            userRef.update("date", newDOB)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Doğum tarihi güncellendi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Doğum tarihi güncellenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }


    }
}
