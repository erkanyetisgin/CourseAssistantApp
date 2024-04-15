package com.example.courseassistantapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class adminViewFragment : Fragment() {
    private lateinit var db: FirebaseFirestore
    private lateinit var userType: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_delete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        val listView = view.findViewById<ListView>(R.id.user_list_view)

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val users = mutableListOf<String>()
                for (document in result) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val studentId  = document.getString("studentId") ?: ""
                    val email = document.getString("email") ?: ""
                    val date = document.getString("date") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val fullName = "$firstName $lastName"
                    if (userType == "student" && email.endsWith("@std.yildiz.edu.tr")) {
                        users.add("Öğrenci Adı: $fullName\nÖğrenci Numarası: $studentId\nEmail: $email\nDoğum Tarihi: $date\nTelefon: $phone")
                    } else if (userType == "instructor" && email.endsWith("@yildiz.edu.tr")) {
                        users.add("Eğitmen Adı: $fullName\nEmail: $email\nTelefon: $phone\nDoğum Tarihi: $date")
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, users)
                listView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Kullanıcılar yüklenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun setUserType(type: String) {
        userType = type
    }
}
