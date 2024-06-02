package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot

class studentCoursesFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_student_courses, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        recyclerView = rootView.findViewById(R.id.recycler_view_courses)
        recyclerView.layoutManager = LinearLayoutManager(context)
        courseAdapter = CourseAdapter(emptyList(), ::onCourseClick)
        recyclerView.adapter = courseAdapter

        fetchStudentCourses()

        return rootView
    }

    private fun fetchStudentCourses() {
        firestore.collection("course_groups")
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                val courses = mutableListOf<Course>()
                val tasks = mutableListOf<Task<DocumentSnapshot>>()

                querySnapshot?.forEach { documentSnapshot ->
                    val courseId = documentSnapshot.id
                    val courseName = documentSnapshot.getString("courseName") ?: ""
                    val startDate = documentSnapshot.getString("start_date") ?: ""
                    val endDate = documentSnapshot.getString("end_date") ?: ""
                    val instructorId = documentSnapshot.getString("instructor_id") ?: ""

                    val task = firestore.collection("instructors").document(instructorId).get().addOnSuccessListener { instructorSnapshot ->
                        val instructorName = instructorSnapshot.getString("firstName") + " " + instructorSnapshot.getString("lastName")
                        val course = Course(courseId, courseName, startDate, endDate, instructorName)
                        courses.add(course)
                    }
                    tasks.add(task)
                }

                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    courseAdapter.updateCourses(courses)
                }

                if (querySnapshot?.isEmpty == true) {
                    Toast.makeText(context, "Ders kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching courses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onCourseClick(courseId: String) {
        val selectedCourse = courseAdapter.courses.find { it.id == courseId }
        if (selectedCourse != null) {
            val courseInfo = "Ders Adı: ${selectedCourse.name}\nBaşlangıç Tarihi: ${selectedCourse.startDate}\nBitiş Tarihi: ${selectedCourse.endDate}\nÖğretmen: ${selectedCourse.instructorName}"
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(selectedCourse.name)
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
