package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

data class Report(
    val id: String,
    val senderName: String,
    val instructorId: String,
    val scope: String,
    val subject: String,
    val body: String,
    val courseName: String
)

class instructorViewReports : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_instructor_view_reports, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view_reports)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val instructorId = firebaseAuth.currentUser?.uid
        if (instructorId != null) {
            firestore.collection("reports")
                .whereEqualTo("instructor_id", instructorId)
                .get()
                .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                    val reports = mutableListOf<Report>()
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val studentIds = querySnapshot.documents.map { it.getString("student_id") }.toSet()
                        firestore.collection("students")
                            .whereIn(FieldPath.documentId(), studentIds.toList())
                            .get()
                            .addOnSuccessListener { studentQuerySnapshot ->
                                val studentMap = studentQuerySnapshot.documents.associateBy({ it.id }, { it.getString("email") })

                                for (documentSnapshot in querySnapshot.documents) {
                                    val studentId = documentSnapshot.getString("student_id") ?: ""
                                    val studentEmail = studentMap[studentId] ?: "Unknown Student"
                                    val report = Report(
                                        id = documentSnapshot.id,
                                        senderName = "Gönderen: $studentEmail",
                                        instructorId = documentSnapshot.getString("instructor_id") ?: "",
                                        scope = "Kapsam: ${documentSnapshot.getString("scope")}",
                                        subject = "Konu: ${documentSnapshot.getString("subject")}",
                                        body = "İçerik: ${documentSnapshot.getString("body")}",
                                        courseName = "Ders Adı: ${documentSnapshot.getString("courseName")}"
                                    )
                                    reports.add(report)
                                }

                                if (reports.isNotEmpty()) {
                                    recyclerView.adapter = ReportAdapter(reports)
                                } else {
                                    Toast.makeText(requireContext(), "Rapor bulunamadı", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(requireContext(), "Raporlar alınırken bir hata oluştu: $exception", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Rapor bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Raporlar alınırken bir hata oluştu: $exception", Toast.LENGTH_SHORT).show()
                }
        }


        return rootView
    }


    class ReportAdapter(private val reports: List<Report>) : RecyclerView.Adapter<ReportAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val scopeTextView: TextView = view.findViewById(R.id.text_scope)
            val subjectTextView: TextView = view.findViewById(R.id.text_subject)
            val bodyTextView: TextView = view.findViewById(R.id.text_body)
            val courseNameTextView: TextView = view.findViewById(R.id.text_course_name)
            val senderNameTextView: TextView = view.findViewById(R.id.text_sender_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val report = reports[position]
            holder.scopeTextView.text = report.scope
            holder.subjectTextView.text = report.subject
            holder.bodyTextView.text = report.body
            holder.courseNameTextView.text = report.courseName
            holder.senderNameTextView.text = report.senderName
        }

        override fun getItemCount(): Int {
            return reports.size
        }
    }
}

