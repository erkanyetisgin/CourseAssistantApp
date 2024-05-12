package com.example.courseassistantapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class adminDeleteFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val listView = view.findViewById<ListView>(R.id.user_list_view)

        // users parçalandı.artık students ve instructors olarak ayrı ayrı listelenecek.
        //ilk olarak students listelenecek.
        // students listesi

        if (userType == "students") {
            db.collection("students")
                .get()
                .addOnSuccessListener { result ->
                    val students = mutableListOf<String>()
                    for (document in result) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        students.add(fullName)
                    }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, students)
                    listView.adapter = adapter

                    listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                        val selectedStudent = students[position]
                        showDeleteConfirmationDialog(selectedStudent, "students")
                    }
                }
        }

        // instructors listesi
        else if (userType == "instructors") {
            db.collection("instructors")
                .get()
                .addOnSuccessListener { result ->
                    val instructors = mutableListOf<String>()
                    for (document in result) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        instructors.add(fullName)
                    }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, instructors)
                    listView.adapter = adapter

                    listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                        val selectedInstructor = instructors[position]
                        showDeleteConfirmationDialog(selectedInstructor, "instructors")
                    }
                }
        }


  /*      db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val users = mutableListOf<String>()
                for (document in result) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName"
                    if (userType == "student" && document.getString("email")?.endsWith("@std.yildiz.edu.tr") == true) {
                        users.add(fullName)
                    } else if (userType == "instructor" && document.getString("email")?.endsWith("@yildiz.edu.tr") == true) {
                        users.add(fullName)
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, users)
                listView.adapter = adapter

                listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    val selectedUser = users[position]
                    showDeleteConfirmationDialog(selectedUser)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Kullanıcılar yüklenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
            } */
    }

    private fun showDeleteConfirmationDialog(userName: String, userType: String) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Kullanıcıyı Sil")
        alertDialogBuilder.setMessage("$userName adlı kullanıcıyı silmek istediğinize emin misiniz?")
        alertDialogBuilder.setPositiveButton("Evet") { _, _ ->
            deleteUser(userName, userType)
        }
        alertDialogBuilder.setNegativeButton("Hayır") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteUser(userName: String, userType: String) {
       db.collection(userType   )
            .whereEqualTo("firstName", userName.split(" ")[0])
            .whereEqualTo("lastName", userName.split(" ")[1])
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection(userType).document(document.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Kullanıcı başarıyla silindi.", Toast.LENGTH_SHORT).show()
                            val transaction = requireFragmentManager().beginTransaction()
                            transaction.detach(this).attach(this).commit()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Kullanıcı silinirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Kullanıcı silinirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    fun setUserType(type: String) {
        userType = type
    }
}