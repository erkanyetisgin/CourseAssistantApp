package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class studentRegisterCoursesFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseListTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_student_register_courses, container, false)


        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        courseListTextView = rootView.findViewById(R.id.text_course_list)

        fetchStudentCourses()



        return rootView
    }

    private fun fetchStudentCourses() {
        val studentId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("group_students")
            .whereEqualTo("student_id", studentId)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                val courseList = StringBuilder()
                querySnapshot?.forEach { documentSnapshot ->
                    val courseId = documentSnapshot.getString("course_id") ?: ""
                    if (courseId.isNotEmpty()) {
                        firestore.collection("course_groups").document(courseId).get().addOnSuccessListener { courseSnapshot ->
                            if (courseSnapshot.exists()) {
                                val courseName = courseSnapshot.getString("courseName") ?: ""
                                val startDate = courseSnapshot.getString("start_date") ?: ""
                                val endDate = courseSnapshot.getString("end_date") ?: ""
                                val instructorId = courseSnapshot.getString("instructor_id") ?: ""
                                firestore.collection("instructors").document(instructorId).get().addOnSuccessListener { instructorSnapshot ->
                                    val instructorName = instructorSnapshot.getString("firstName") + " " + instructorSnapshot.getString("lastName")
                                    courseList.append("Course ID: $courseId\n")
                                    courseList.append("Ders Adı: $courseName\n")
                                    courseList.append("Başlangıç Tarihi: $startDate\n")
                                    courseList.append("Bitiş Tarihi: $endDate\n")
                                    courseList.append("Öğretmen: $instructorName\n\n")

                                    courseListTextView.text = courseList.toString()
                                    if (courseList.isEmpty()) {
                                        courseListTextView.text = "Öğrencinin kayıtlı olduğu ders bulunamadı."
                                    }
                                }
                            }
                        }.addOnFailureListener { e ->
                            courseListTextView.text = "Error fetching course details: ${e.message}"
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                courseListTextView.text = "Error fetching courses: ${e.message}"
            }
    }




}