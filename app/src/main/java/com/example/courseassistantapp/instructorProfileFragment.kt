package com.example.courseassistantapp


import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class instructorProfileFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var firebaseRef: DatabaseReference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseRef = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!
        db = FirebaseFirestore.getInstance()

        val nameTextView = view.findViewById<TextView>(R.id.instructor_profile_name)
        val emailTextView = view.findViewById<TextView>(R.id.instructor_profile_email)
        val dobTextView = view.findViewById<TextView>(R.id.instructor_profile_dob)
        val phoneTextView = view.findViewById<TextView>(R.id.instructor_profile_phone_number)
        val instagramButton = view.findViewById<ImageButton>(R.id.instagram)
        val whatsappButton = view.findViewById<ImageButton>(R.id.whatsapp)
        val phoneButton = view.findViewById<ImageButton>(R.id.instructor_phonecall)
        val emailButton = view.findViewById<ImageButton>(R.id.instructor_email)
        val addInstagram = view.findViewById<Button>(R.id.add_instagram)
        val instagramTextView = view.findViewById<TextView>(R.id.instructor_profile_instagram)
        val whatsappTextView = view.findViewById<TextView>(R.id.instructor_profile_whatsapp)
        val profile_picture = view.findViewById<ImageView>(R.id.instructor_profile_image)

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

        val userId = currentUser.uid
        val userRef = db.collection("instructors").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.data
                    val firstname = user?.get("firstName").toString()
                    val lastname = user?.get("lastName").toString()
                    nameTextView.text = "$firstname $lastname".uppercase()
                    emailTextView.text = user?.get("email").toString()
                    dobTextView.text = user?.get("date").toString()
                    phoneTextView.text = user?.get("phone").toString()
                    val instagramUsername: String
                    if (user?.containsKey("instagramUsername") == true) {
                        instagramTextView.text = user["instagramUsername"].toString()
                    } else {
                        instagramTextView.text = "Instagram Kullanıcı eklenmedi"
                    }
                    whatsappTextView.text = user?.get("phone").toString()

                } else {
                    Toast.makeText(context, "Kullanıcı verisi bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Veritabanına erişimde hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        phoneButton.setOnClickListener {
            val phoneNumber = phoneTextView.text.toString()

            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }

            startActivity(intent)
        }

        emailButton.setOnClickListener {
            val email = emailTextView.text.toString()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }

            startActivity(intent)
        }

        addInstagram.setOnClickListener {
            val username = instagramTextView.text.toString()
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Instagram Kullanıcı Adı")
            val input = EditText(requireContext())
            builder.setView(input)
            builder.setPositiveButton("Ekle") { dialog, which ->
                val newUsername = input.text.toString().trim()
                if (newUsername.isNotEmpty()) {
                    val userRef = db.collection("instructors").document(currentUser.uid)
                    userRef.update("instagramUsername", newUsername)
                        .addOnSuccessListener {
                            instagramTextView.text = newUsername
                            Toast.makeText(context, "Instagram kullanıcı adı başarıyla eklendi: $newUsername", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Instagram kullanıcı adını eklerken bir hata oluştu: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Instagram kullanıcı adı boş olamaz", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("İptal") { dialog, which -> dialog.cancel() }
            builder.show()

        }

        instagramButton.setOnClickListener {
            val username = instagramTextView.text.toString()
            if (username.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://instagram.com/$username")
                }
                startActivity(intent)
            } else {
                Toast.makeText(context, "Instagram kullanıcı adı bulunamadı", Toast.LENGTH_SHORT).show()
            }

        }

        whatsappButton.setOnClickListener {
            val phoneNumber = whatsappTextView.text.toString()
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        }


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_instructor_profile, container, false)
    }

}