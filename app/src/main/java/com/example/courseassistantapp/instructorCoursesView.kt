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

class instructorCoursesView : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseListTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_instructor_courses_view, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        courseListTextView = rootView.findViewById(R.id.text_course_list)

        fetchInstructorCourses()

        return rootView
    }

    private fun fetchInstructorCourses() {
        val instructorId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("course_groups")
            .whereEqualTo("instructor_id", instructorId)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                val courseList = StringBuilder()
                querySnapshot?.forEach { documentSnapshot ->
                    val courseId = documentSnapshot.id
                    val courseName = documentSnapshot.getString("courseName") ?: ""
                    val startDate = documentSnapshot.getString("start_date") ?: ""
                    val endDate = documentSnapshot.getString("end_date") ?: ""
                    courseList.append("ID: $courseId\n")
                    courseList.append("Ders Adı: $courseName\n")
                    courseList.append("Başlangıç Tarihi: $startDate\n")
                    courseList.append("Bitiş Tarihi: $endDate\n\n")
                }
                courseListTextView.text = courseList.toString()
                if (courseList.isEmpty()) {
                    courseListTextView.text = "Öğretmenin kayıtlı olduğu ders bulunamadı."
                }

                courseListTextView.setOnClickListener {
                    navigateToEditCourse(querySnapshot?.documents?.get(0)?.id ?: "")
                }

            }
            .addOnFailureListener { e ->
                courseListTextView.text = "Error fetching courses: ${e.message}"
            }


    }

    private fun navigateToEditCourse(courseId: String) {
        val fragment = instructorEditCoursesFragment()
        val bundle = Bundle()
        bundle.putString("courseId", courseId)
        fragment.arguments = bundle
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}