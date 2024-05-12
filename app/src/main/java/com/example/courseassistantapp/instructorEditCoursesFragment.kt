package com.example.courseassistantapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath


class instructorEditCoursesFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var courseIdTextView: TextView
    private lateinit var courseNameTextView: TextView
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private lateinit var instructorNameTextView: TextView
    private lateinit var numStudentsTextView: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val rootView = inflater.inflate(R.layout.fragment_instructor_edit_courses, container, false)

        courseIdTextView = rootView.findViewById<TextView>(R.id.course_id)
        courseNameTextView = rootView.findViewById<TextView>(R.id.course_name)
        startDateTextView = rootView.findViewById<TextView>(R.id.start_date)
        endDateTextView = rootView.findViewById<TextView>(R.id.end_date)
        instructorNameTextView = rootView.findViewById<TextView>(R.id.instructor_name)
        numStudentsTextView = rootView.findViewById<TextView>(R.id.number_of_students)
        val viewStudentsButton = rootView.findViewById<Button>(R.id.view_students)
        val addStudentButton = rootView.findViewById<Button>(R.id.button_add_student)
        val deleteStudentButton = rootView.findViewById<Button>(R.id.button_delete_student)
        val updateCourseButton = rootView.findViewById<Button>(R.id.button_update_group)
        val deleteGroupButton = rootView.findViewById<Button>(R.id.button_delete_group)

        fetchCourseInfo()



        viewStudentsButton.setOnClickListener {
            val groupId = courseIdTextView.text.toString().substringAfter("ID: ").trim()

            firestore.collection("group_students")
                .whereEqualTo("group_id", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(requireContext(), "Bu gruba kayıtlı öğrenci bulunmamaktadır.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val studentList = mutableListOf<String>()
                    for (document in documents) {
                        val studentId = document.getString("student_id")
                        if (studentId != null) {
                            firestore.collection("students")
                                .document(studentId)
                                .get()
                                .addOnSuccessListener { studentDocument ->
                                    val fullName = "${studentDocument.getString("firstName")} ${studentDocument.getString("lastName")} (${studentDocument.getString("studentId")})"
                                    studentList.add(fullName)

                                    if (studentList.size == documents.size()) {
                                        showStudentListDialog(studentList, "view")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                }
        }



        addStudentButton.setOnClickListener {
            firestore.collection("students")
                .get()
                .addOnSuccessListener { documents ->
                    val studentList = mutableListOf<String>()

                    for (document in documents) {
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")
                        val studentId = document.getString("studentId")

                        if (firstName != null && lastName != null && studentId != null) {
                            studentList.add("$firstName $lastName ($studentId)")
                        }
                    }

                    showStudentListDialog(studentList,"add")
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                }
        }

        deleteStudentButton.setOnClickListener {
            val groupId = courseIdTextView.text.toString().substringAfter("ID: ").trim()

            firestore.collection("group_students")
                .whereEqualTo("group_id", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(requireContext(), "Bu gruba kayıtlı öğrenci bulunmamaktadır.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val studentList = mutableListOf<String>()
                    for (document in documents) {
                        val studentId = document.getString("student_id")
                        if (studentId != null) {
                            firestore.collection("students")
                                .document(studentId)
                                .get()
                                .addOnSuccessListener { studentDocument ->
                                    val fullName = "${studentDocument.getString("firstName")} ${studentDocument.getString("lastName")} (${studentDocument.getString("studentId")})"
                                    studentList.add(fullName)

                                    if (studentList.size == documents.size()) {
                                        showStudentListDialog(studentList, "delete")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                }

        }

        updateCourseButton.setOnClickListener {
            val courseId = courseIdTextView.text.toString().substringAfter("ID: ").trim()
            val groupName = courseNameTextView.text.toString().substringAfter("Ders Adı: ").trim()
            val startDate = startDateTextView.text.toString().substringAfter("Başlangıç Tarihi: ").trim()
            val endDate = endDateTextView.text.toString().substringAfter("Bitiş Tarihi: ").trim()

            val dialogView = LinearLayout(requireContext())
            dialogView.orientation = LinearLayout.VERTICAL

            val editTextGroupName = EditText(requireContext())
            editTextGroupName.hint = "Ders Adı"
            editTextGroupName.setText(groupName)
            dialogView.addView(editTextGroupName)

            val editTextStartDate = EditText(requireContext())
            editTextStartDate.hint = "Başlangıç Tarihi"
            editTextStartDate.setText(startDate)
            dialogView.addView(editTextStartDate)

            val editTextEndDate = EditText(requireContext())
            editTextEndDate.hint = "Bitiş Tarihi"
            editTextEndDate.setText(endDate)
            dialogView.addView(editTextEndDate)

            AlertDialog.Builder(requireContext())
                .setTitle("Kursu Güncelle")
                .setView(dialogView)
                .setPositiveButton("Güncelle") { dialog, _ ->
                    val newGroupName = editTextGroupName.text.toString()
                    val newStartDate = editTextStartDate.text.toString()
                    val newEndDate = editTextEndDate.text.toString()

                    val courseGroupRef = firestore.collection("course_groups").document(courseId)
                    courseGroupRef.update("courseName", newGroupName)
                        .addOnSuccessListener {
                            courseGroupRef.update("start_date", newStartDate)
                                .addOnSuccessListener {
                                    courseGroupRef.update("end_date", newEndDate)
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Kurs bilgileri güncellendi", Toast.LENGTH_SHORT).show()
                                            fetchCourseInfo()
                                        }
                                        .addOnFailureListener { exception ->
                                            Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("İptal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        deleteGroupButton.setOnClickListener {
            val groupId = courseIdTextView.text.toString().substringAfter("ID: ").trim()

            AlertDialog.Builder(requireContext())
                .setTitle("Grubu Sil")
                .setMessage("Kurs grubunu silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet") { dialog, _ ->
                    firestore.collection("course_groups").document(groupId)
                        .delete()
                        .addOnSuccessListener {
                            firestore.collection("group_students")
                                .whereEqualTo("group_id", groupId)
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        document.reference.delete()
                                    }
                                    Toast.makeText(requireContext(), "Kurs grubu ve ilişkili öğrenci kayıtları silindi", Toast.LENGTH_SHORT).show()
                                    requireActivity().supportFragmentManager.popBackStack()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Hayır") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }



        return rootView
    }

    private fun fetchCourseInfo() {
        val instructorId = firebaseAuth.currentUser?.uid
        if (instructorId != null) {
            firestore.collection("course_groups")
                .whereEqualTo("instructor_id", instructorId)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val courseGroup = documents.documents[0]
                        val courseId = courseGroup.id
                        val groupName = courseGroup.getString("courseName")
                        val numStudents = courseGroup.getLong("number_of_students")
                        val startDate = courseGroup.getString("start_date")
                        val endDate = courseGroup.getString("end_date")

                        if (instructorId != null) {
                            firestore.collection("instructors")
                                .document(instructorId)
                                .get()
                                .addOnSuccessListener { document ->
                                    val instructorName = document.getString("firstName") + " " + document.getString("lastName")
                                    instructorNameTextView.text = "Öğretmen Adı: $instructorName"
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                }
                        }

                        courseIdTextView.text = "ID: $courseId"
                        courseNameTextView.text = "Ders Adı: $groupName"
                        startDateTextView.text = "Başlangıç Tarihi: $startDate"
                        endDateTextView.text = "Bitiş Tarihi: $endDate"
                        numStudentsTextView.text = "Öğrenci Sayısı: $numStudents"
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun showStudentListDialog(studentList: List<String>, action: String) {
        val studentArray = studentList.toTypedArray()

        val builder = AlertDialog.Builder(requireContext())
        if (action == "view") {
            builder.setTitle("Öğrenciler")
        } else if (action == "add") {
            builder.setTitle("Öğrenci Ekle")
        } else if (action == "delete") {
            builder.setTitle("Öğrenci Sil")
        }
        builder.setItems(studentArray) { dialog, which ->
            val selectedStudent = studentArray[which]
            if (action == "add") {
                addStudentToGroup(selectedStudent)
            } else if (action == "delete") {
                deleteStudentFromGroup(selectedStudent)
            }
        }
        builder.setNegativeButton("İptal") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()

    }

    private fun addStudentToGroup(selectedStudent: String) {
        val groupId = courseIdTextView.text.toString().substringAfter("ID: ").trim()

        val fullName = selectedStudent.substringBefore("(").trim()
        val studentId = selectedStudent.substringAfter("(").substringBefore(")").trim()

        firestore.collection("students")
            .whereEqualTo("firstName", fullName.substringBefore(" "))
            .whereEqualTo("lastName", fullName.substringAfter(" "))
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val studentDocument = documents.documents[0]
                    val studentId = studentDocument.id

                    firestore.collection("group_students")
                        .whereEqualTo("student_id", studentId)
                        .whereEqualTo("group_id", groupId)
                        .get()
                        .addOnSuccessListener { groupStudentDocs ->
                            if (groupStudentDocs.isEmpty) {
                                val groupStudent = hashMapOf(
                                    "student_id" to studentId,
                                    "group_id" to groupId
                                )

                                firestore.collection("group_students")
                                    .add(groupStudent)
                                    .addOnSuccessListener { documentReference ->

                                        firestore.collection("course_groups")
                                            .document(groupId)
                                            .get()
                                            .addOnSuccessListener { documentSnapshot ->
                                                val currentNumStudents = documentSnapshot.getLong("number_of_students") ?: 0
                                                val updatedNumStudents = currentNumStudents + 1

                                                firestore.collection("course_groups")
                                                    .document(groupId)
                                                    .update("number_of_students", updatedNumStudents)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(requireContext(), "Öğrenci eklendi", Toast.LENGTH_SHORT).show()
                                                        fetchCourseInfo()
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(requireContext(), "Öğrenci zaten gruba ekli!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Öğrenci bulunamadı!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
            }

    }


    private fun deleteStudentFromGroup(selectedStudent: String) {
        val groupId = courseIdTextView.text.toString().substringAfter("ID: ").trim()

        val fullName = selectedStudent.substringBefore("(").trim()
        val studentId = selectedStudent.substringAfter("(").substringBefore(")").trim()

        firestore.collection("students")
            .whereEqualTo("firstName", fullName.substringBefore(" "))
            .whereEqualTo("lastName", fullName.substringAfter(" "))
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val studentDocument = documents.documents[0]
                    val studentId = studentDocument.id

                    firestore.collection("group_students")
                        .whereEqualTo("student_id", studentId)
                        .whereEqualTo("group_id", groupId)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val documentId = documents.documents[0].id
                                firestore.collection("group_students")
                                    .document(documentId)
                                    .delete()
                                    .addOnSuccessListener {
                                        firestore.collection("course_groups")
                                            .document(groupId)
                                            .get()
                                            .addOnSuccessListener { documentSnapshot ->
                                                val currentNumStudents =
                                                    documentSnapshot.getLong("number_of_students") ?: 0
                                                val updatedNumStudents = currentNumStudents - 1
                                                firestore.collection("course_groups")
                                                    .document(groupId)
                                                    .update("number_of_students", updatedNumStudents)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(requireContext(), "Öğrenci silindi", Toast.LENGTH_SHORT).show()
                                                        fetchCourseInfo()
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(requireContext(), "Öğrenci bulunamadı!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Öğrenci bulunamadı!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Hata: $exception", Toast.LENGTH_SHORT).show()
            }
    }


}