package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class studentCoursesFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseListTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_student_courses, container, false)


        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        courseListTextView = rootView.findViewById(R.id.text_course_list)

        fetchStudentCourses()



        return rootView
    }

    private fun fetchStudentCourses() {
        firestore.collection("course_groups")
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                val courseList = StringBuilder()
                querySnapshot?.forEach { documentSnapshot ->
                    val courseId = documentSnapshot.id
                    val courseName = documentSnapshot.getString("courseName") ?: ""
                    val startDate = documentSnapshot.getString("start_date") ?: ""
                    val endDate = documentSnapshot.getString("end_date") ?: ""
                    val instructorId = documentSnapshot.getString("instructor_id") ?: ""
                    firestore.collection("instructors").document(instructorId).get().addOnSuccessListener { instructorSnapshot ->
                        val instructorName = instructorSnapshot.getString("firstName") + " " + instructorSnapshot.getString("lastName")
                        courseList.append("Course ID: $courseId\n")
                        courseList.append("Ders Adı: $courseName\n")
                        courseList.append("Başlangıç Tarihi: $startDate\n")
                        courseList.append("Bitiş Tarihi: $endDate\n")
                        courseList.append("Öğretmen: $instructorName\n\n")
                        courseListTextView.text = courseList.toString()

                        val courseInfo = "Ders Adı: $courseName\nBaşlangıç Tarihi: $startDate\nBitiş Tarihi: $endDate\nÖğretmen: $instructorName"
                        courseListTextView.setOnClickListener {
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle(courseName)
                            builder.setCancelable(false)
                            builder.setPositiveButton("Evet") { dialog, which ->
                                registerToCourse(courseId)
                            }
                            builder.setNegativeButton("Hayır") { dialog, which ->
                                dialog.dismiss()
                            }
                            builder.setMessage("Bu derse kaydınızı yapmak istiyor musunuz?\n\n$courseInfo")
                            val alertDialog = builder.create()
                            alertDialog.show()
                        }
                    }
                }
                if (querySnapshot?.isEmpty == true) {
                    courseListTextView.text = "Ders kaydı bulunamadı."
                }
            }
            .addOnFailureListener { e ->
                courseListTextView.text = "Error fetching courses: ${e.message}"
            }
    }

    private fun registerToCourse(courseId: String) {
        val studentId = firebaseAuth.currentUser?.uid ?: return
        val data = hashMapOf(
            "student_id" to studentId,
            "course_id" to courseId
        )
        firestore.collection("group_students").add(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Ders kaydı başarılı", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ders kaydı başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





}