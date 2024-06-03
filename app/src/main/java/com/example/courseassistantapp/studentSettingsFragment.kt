package com.example.courseassistantapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class studentSettingsFragment : Fragment() {
    private lateinit var currentUser: FirebaseUser
    companion object {
        private const val GALLERY_REQUEST_CODE = 1001
        private const val CAMERA_REQUEST_CODE = 1002
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser = FirebaseAuth.getInstance().currentUser!!
        val emailButton = view.findViewById<Button>(R.id.btn_change_email)
        val passwordButton = view.findViewById<Button>(R.id.btn_change_password)
        val imageButton = view.findViewById<Button>(R.id.btn_change_image)
        val deleteButton = view.findViewById<Button>(R.id.btn_delete_account)
        val emailEditText = view.findViewById<EditText>(R.id.new_email)
        val passwordEditText = view.findViewById<EditText>(R.id.new_password)
        val profile_picture = view.findViewById<ImageView>(R.id.profile_picture)
        val personalInfoButton = view.findViewById<Button>(R.id.btn_update_personal_info)

        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${currentUser.uid}")
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            val imageURL = uri.toString()
            Glide.with(this)
                .load(imageURL)
                .into(profile_picture)
        }.addOnFailureListener { exception ->
            profile_picture.setImageResource(R.drawable.admin)
        }

        imageButton.setOnClickListener {
            val imageDialog = AlertDialog.Builder(requireContext())
            imageDialog.setTitle("Profil Resmi")
            imageDialog.setMessage("Profil resminizi güncellemek için bir kaynak seçin")

            imageDialog.setPositiveButton("Galeri") { dialog, _ ->
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                dialog.dismiss()
            }

            imageDialog.setNegativeButton("Kamera") { dialog, _ ->
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                dialog.dismiss()
            }

            imageDialog.setNeutralButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }

            imageDialog.show()
        }

        personalInfoButton.setOnClickListener {
            val fragment = studentUpdateInfoFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()


        }

        emailButton.setOnClickListener {
            val newEmail = emailEditText.text.toString()
            val user = FirebaseAuth.getInstance().currentUser

            val passwordDialog = buildPasswordDialog { reenteredPassword ->
                if (Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    val credential = EmailAuthProvider.getCredential(user?.email!!, reenteredPassword)
                    user.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updateEmail(newEmail)?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Authentication'da email güncellendi
                                    val db = FirebaseFirestore.getInstance()
                                    val userId = user.uid

                                    // Firestore'da kullanıcı belgesini güncelle
                                    db.collection("students").document(userId)
                                        .update("email", newEmail)
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "E-posta adresi başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(requireContext(), "Firestore güncellemesi başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), reauthTask.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Geçersiz e-posta adresi", Toast.LENGTH_SHORT).show()
                }
            }

            passwordDialog.show()
        }

        passwordButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            val user = FirebaseAuth.getInstance().currentUser

            val passwordDialog = buildPasswordDialog {
                val reenteredPassword = it
                if (newPassword.length >= 6) {
                    val credential = EmailAuthProvider.getCredential(user?.email!!, reenteredPassword)
                    user.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPassword)?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(), "Şifre başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), reauthTask.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Geçersiz şifre", Toast.LENGTH_SHORT).show()
                }
            }

            passwordDialog.show()
        }

        deleteButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            val passwordDialog = buildPasswordDialog {
                val reenteredPassword = it
                val credential = EmailAuthProvider.getCredential(user?.email!!, reenteredPassword)
                user.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Hesabınız başarıyla silindi", Toast.LENGTH_SHORT).show()
                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            } else {
                                Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), reauthTask.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            passwordDialog.show()
        }


    }

    private fun buildPasswordDialog(callback: (String) -> Unit): AlertDialog {
        val newPasswordDialog = AlertDialog.Builder(requireContext())
        newPasswordDialog.setTitle(" Hesap Doğrulama")
        newPasswordDialog.setMessage("İşlemi gerçekleştirebilmek için mevcut şifrenizi girin")

        val verifyPassword = EditText(requireContext())
        verifyPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        newPasswordDialog.setView(verifyPassword)

        newPasswordDialog.setPositiveButton("Tamam") { dialog, _ ->
            val reenteredPassword = verifyPassword.text.toString()
            callback(reenteredPassword)
        }

        newPasswordDialog.setNegativeButton("İptal") { dialog, _ ->
            dialog.dismiss()
        }

        return newPasswordDialog.create()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    val imageUri = data?.data
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference
                    val imageRef = storageRef.child("images/${currentUser.uid}")
                    imageRef.putFile(imageUri!!).addOnSuccessListener {
                        Toast.makeText(context, "Resim başarıyla yüklendi", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(context, "Resim yüklenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference
                    val imageRef = storageRef.child("images/${currentUser.uid}")
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val imageData = baos.toByteArray()
                    imageRef.putBytes(imageData).addOnSuccessListener {
                        Toast.makeText(context, "Resim başarıyla yüklendi", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        Toast.makeText(context, "Resim yüklenirken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateEmailInDatabase(userId: String, newEmail: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("students").child(userId)
        val updates = hashMapOf<String, Any>(
            "email" to newEmail
        )
        databaseReference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "E-posta adresi veritabanında başarıyla güncellendi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "E-posta adresi veritabanında güncellenirken bir hata oluştu: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
