package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class instructorCreateCourseFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_instructor_create_course, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val courseIdEditText = rootView.findViewById<EditText>(R.id.course_id)
        val courseNameEditText = rootView.findViewById<EditText>(R.id.course_name)
        val groupCountSpinner = rootView.findViewById<Spinner>(R.id.group_count)
        val groupInputContainer = rootView.findViewById<LinearLayout>(R.id.group_input_container)
        val createCourseButton = rootView.findViewById<Button>(R.id.btn_create_course)
        val startDate = rootView.findViewById<EditText>(R.id.course_start_date)
        val endDate = rootView.findViewById<EditText>(R.id.course_end_date)
        val teachers = ArrayList<String>()


        firestore.collection("instructors").get().addOnSuccessListener { querySnapshot: QuerySnapshot? ->
            querySnapshot?.forEach { documentSnapshot ->
                val teacherName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName")
                if (teacherName != null) {
                    teachers.add(teacherName)
                }
            }
        }


        groupCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                groupInputContainer.removeAllViews()
                firestore.collection("instructors").get().addOnSuccessListener { querySnapshot: QuerySnapshot? ->
                    val teachers = ArrayList<String>()
                    querySnapshot?.forEach { documentSnapshot ->
                        val teacherName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName")
                        if (teacherName != null) {
                            teachers.add(teacherName)
                        }
                    }
                    createGroupInputs(groupCountSpinner.selectedItemPosition + 1, groupInputContainer, teachers)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        createCourseButton.setOnClickListener {
            val courseId = courseIdEditText.text.toString()
            val courseName = courseNameEditText.text.toString()

            val course = hashMapOf(
                "courseName" to courseName,
                "start_date" to startDate.text.toString(),
                "end_date" to endDate.text.toString(),
            )

            firestore.collection("courses").document(courseId).set(course).addOnSuccessListener {
                Toast.makeText(requireContext(), "Kurs oluşturuldu", Toast.LENGTH_SHORT).show()

                addGroupsToFirestore(courseId, groupInputContainer,startDate.text.toString(),endDate.text.toString(),courseName)
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Kurs oluşturulamadı", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }
    private fun createGroupInputs(groupCount: Int, groupInputContainer: LinearLayout, teachers: ArrayList<String>) {
        for (i in 1..groupCount) {
            val groupLabel = TextView(requireContext())
            groupLabel.text = "$i- Grup = "

            val spinner = Spinner(requireContext())
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teachers)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            groupInputContainer.addView(groupLabel)
            groupInputContainer.addView(spinner)
        }
    }

    private fun getInstructorIdByName(instructorName: String, callback: (String) -> Unit) {
        val firstName = instructorName.split(" ")[0]
        val lastName = instructorName.split(" ")[1]

        firestore.collection("instructors")
            .whereEqualTo("firstName", firstName)
            .whereEqualTo("lastName", lastName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    callback(documentSnapshot.id)
                } else {
                    callback("")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Öğretmen bulunamadı", Toast.LENGTH_SHORT).show()
                callback("")
            }
    }

    private fun addGroupsToFirestore(courseId: String, groupInputContainer: LinearLayout,start_date:String,end_date:String,courseName:String) {
        for (i in 0 until groupInputContainer.childCount step 2) {
            val instructorName = (groupInputContainer.getChildAt(i + 1) as Spinner).selectedItem.toString()


            getInstructorIdByName(instructorName) { instructorId ->
                if (instructorId.isNotEmpty()) {
                    val group = hashMapOf(
                        "course_id" to courseId,
                        "courseName" to courseName,
                        "instructor_id" to instructorId,
                        "number_of_students" to 0,
                        "start_date" to start_date,
                        "end_date" to end_date
                    )

                    firestore.collection("course_groups").document(courseId + "-" + (i / 2 + 1)).set(group)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Grup oluşturuldu", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Grup oluşturulamadı", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Öğretmen bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}
