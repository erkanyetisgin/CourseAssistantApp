package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import android.widget.ArrayAdapter
import android.widget.Toast

class studentSendReport : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_student_send_report, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val scopeEditText = rootView.findViewById<EditText>(R.id.edit_scope)
        val courseNameSpinner = rootView.findViewById<Spinner>(R.id.spinner_course_name)
        val recipientSpinner = rootView.findViewById<Spinner>(R.id.spinner_recipient)
        val subjectEditText = rootView.findViewById<EditText>(R.id.edit_subject)
        val bodyEditText = rootView.findViewById<EditText>(R.id.edit_body)
        val sendButton = rootView.findViewById<Button>(R.id.button_send)

        setupSpinners(courseNameSpinner, recipientSpinner)

        sendButton.setOnClickListener {
            val scope = scopeEditText.text.toString()
            val courseName = courseNameSpinner.selectedItem.toString()
            val subject = subjectEditText.text.toString()
            val body = bodyEditText.text.toString()
            val selectedInstructorName = recipientSpinner.selectedItem.toString()

            firestore.collection("instructors").get().addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                var instructorId: String? = null
                querySnapshot?.forEach { documentSnapshot ->
                    val instructorName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName")
                    if (instructorName == selectedInstructorName) {
                        instructorId = documentSnapshot.id
                    }
                }

                if (instructorId != null) {
                    val report = hashMapOf(
                        "scope" to scope,
                        "courseName" to courseName,
                        "instructor_id" to instructorId,
                        "subject" to subject,
                        "body" to body,
                        "student_id" to firebaseAuth.currentUser?.uid
                    )

                    firestore.collection("reports").add(report)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(requireContext(), "Rapor başarıyla gönderildi", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Rapor gönderilirken bir hata oluştu", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Seçili öğretmen bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return rootView
    }

    private fun setupSpinners(courseNameSpinner: Spinner, recipientSpinner: Spinner) {
        val courseNames = ArrayList<String>()
        val recipients = ArrayList<String>()

        firestore.collection("courses").get().addOnSuccessListener { querySnapshot: QuerySnapshot? ->
            querySnapshot?.forEach { documentSnapshot ->
                val courseName = documentSnapshot.getString("courseName")
                if (courseName != null) {
                    courseNames.add(courseName)
                }
            }
            val courseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseNames)
            courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            courseNameSpinner.adapter = courseAdapter
        }

        firestore.collection("instructors").get().addOnSuccessListener { querySnapshot: QuerySnapshot? ->
            querySnapshot?.forEach { documentSnapshot ->
                val instructorName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName")
                if (instructorName != null) {
                    recipients.add(instructorName)
                }
            }
            val recipientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, recipients)
            recipientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            recipientSpinner.adapter = recipientAdapter
        }
    }
}
