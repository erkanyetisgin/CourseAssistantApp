package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Tasks
import android.util.Log

class instructorCoursesView : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_instructor_courses_view, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        recyclerView = rootView.findViewById(R.id.recycler_view_courses)
        recyclerView.layoutManager = LinearLayoutManager(context)
        courseAdapter = CourseAdapter(emptyList()) { courseId -> navigateToEditCourse(courseId) }
        recyclerView.adapter = courseAdapter

        fetchInstructorCourses()

        return rootView
    }

    private fun fetchInstructorCourses() {
        val instructorId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("course_groups")
            .whereEqualTo("instructor_id", instructorId)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                val courseTasks = querySnapshot?.documents?.map { documentSnapshot ->
                    val courseId = documentSnapshot.id
                    val courseName = documentSnapshot.getString("courseName") ?: "No Course Name"
                    val startDate = documentSnapshot.getString("start_date") ?: "No Start Date"
                    val endDate = documentSnapshot.getString("end_date") ?: "No End Date"
                    val instructorId = documentSnapshot.getString("instructor_id") ?: "No Instructor"

                    firestore.collection("instructors").document(instructorId).get().continueWith { task ->
                        val instructorSnapshot = task.result
                        val instructorName = instructorSnapshot?.getString("firstName") + " " + instructorSnapshot?.getString("lastName")
                        Course(courseId, courseName, startDate, endDate, instructorName)
                    }
                } ?: emptyList()

                Tasks.whenAllSuccess<Course>(courseTasks).addOnSuccessListener { courses ->
                    courseAdapter.updateCourses(courses)
                    Log.d("fetchInstructorCourses", "Courses fetched successfully: ${courses.size} courses")
                }
            }
            .addOnFailureListener { e ->
                Log.e("fetchInstructorCourses", "Error fetching courses", e)
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
