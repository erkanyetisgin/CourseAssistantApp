package com.example.courseassistantapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileWriter
import java.io.IOException

data class AttendanceResult(
    val studentName: String,
    val latitude: Double,
    val longitude: Double,
    val course: String
)

class AttendanceResultsAdapter(
    private val attendanceResults: List<AttendanceResult>
) : RecyclerView.Adapter<AttendanceResultsAdapter.AttendanceResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_result, parent, false)
        return AttendanceResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceResultViewHolder, position: Int) {
        val attendanceResult = attendanceResults[position]
        holder.tvStudentName.text = attendanceResult.studentName
        holder.tvLatitude.text = attendanceResult.latitude.toString()
        holder.tvLongitude.text = attendanceResult.longitude.toString()
        holder.tvCourse.text = attendanceResult.course
    }

    override fun getItemCount(): Int = attendanceResults.size

    class AttendanceResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentName: TextView = view.findViewById(R.id.tv_student_name)
        val tvLatitude: TextView = view.findViewById(R.id.tv_latitude)
        val tvLongitude: TextView = view.findViewById(R.id.tv_longitude)
        val tvCourse: TextView = view.findViewById(R.id.tv_course)
    }
}

class InstructorStartAttendanceFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var spinnerCourses: Spinner
    private lateinit var btnStartAttendance: Button
    private lateinit var btnStopAttendance: Button
    private lateinit var tvAttendanceResults: TextView
    private lateinit var recyclerViewAttendanceResults: RecyclerView
    private lateinit var btnExportResults: Button

    private val attendanceResults = mutableListOf<AttendanceResult>()
    private lateinit var attendanceResultsAdapter: AttendanceResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instructor_start_attendance, container, false)

        spinnerCourses = view.findViewById(R.id.spinner_courses)
        btnStartAttendance = view.findViewById(R.id.btn_start_attendance)
        btnStopAttendance = view.findViewById(R.id.btn_stop_attendance)
        tvAttendanceResults = view.findViewById(R.id.tv_attendance_results)
        recyclerViewAttendanceResults = view.findViewById(R.id.recycler_view_attendance_results)
        btnExportResults = view.findViewById(R.id.btn_export_results)

        setupRecyclerView()
        setupLocationClient()
        fetchCoursesFromFirebase()

        btnStartAttendance.setOnClickListener {
            startAttendance()
        }

        btnStopAttendance.setOnClickListener {
            stopAttendance()
        }

        btnExportResults.setOnClickListener {
            exportResultsToCsv()
        }

        return view
    }

    private fun fetchCoursesFromFirebase() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val instructorId = currentUser?.uid

        if (instructorId != null) {
            db.collection("course_groups")
                .whereEqualTo("instructor_id", instructorId)
                .get()
                .addOnSuccessListener { result ->
                    val courses = mutableListOf<String>()
                    for (document in result) {
                        val courseName = document.getString("courseName")
                        if (courseName != null) {
                            courses.add(courseName)
                        }
                    }
                    setupCoursesSpinner(courses)
                }
                .addOnFailureListener { exception ->
                    Log.w("FirebaseError", "Kursa ait dersler alınırken hata oluştu: ", exception)
                    Toast.makeText(requireContext(), "Hata: Kursa ait dersler alınırken hata oluştu", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Kullanıcı kimliği hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCoursesSpinner(courses: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourses.adapter = adapter

        listenToAttendanceChanges()
    }

    private fun setupRecyclerView() {
        attendanceResultsAdapter = AttendanceResultsAdapter(attendanceResults)
        recyclerViewAttendanceResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAttendanceResults.adapter = attendanceResultsAdapter
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private fun startAttendance() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val selectedCourse = spinnerCourses.selectedItem.toString()
                        Log.d("StartAttendance", "Konum: ${location.latitude}, ${location.longitude}")
                        val db = FirebaseFirestore.getInstance()
                        val courseData = hashMapOf(
                            "courseName" to selectedCourse,
                            "instructor_id" to FirebaseAuth.getInstance().currentUser?.uid,
                            "start_time" to Timestamp.now(),
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "active" to true
                        )
                        db.collection("active_courses")
                            .document(selectedCourse)
                            .set(courseData)
                            .addOnSuccessListener {
                                Log.d("FirebaseSuccess", "Attendance started for $selectedCourse")
                                btnStartAttendance.visibility = View.GONE
                                btnStopAttendance.visibility = View.VISIBLE
                                tvAttendanceResults.visibility = View.VISIBLE
                                recyclerViewAttendanceResults.visibility = View.VISIBLE
                                btnExportResults.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), "$selectedCourse başladı", Toast.LENGTH_SHORT).show()
                                fetchAttendanceResults(selectedCourse)
                            }
                            .addOnFailureListener { e ->
                                Log.w("FirebaseError", "Error adding document", e)
                            }
                    } else {
                        Log.w("LocationError", "Location is null")
                        Toast.makeText(requireContext(), "Konum bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }



    private fun stopAttendance() {
        val selectedCourse = spinnerCourses.selectedItem.toString()
        val db = FirebaseFirestore.getInstance()

        db.collection("active_courses")
            .document(selectedCourse)
            .delete()
            .addOnSuccessListener {
                // Delete attendance results for this course
                deleteAttendanceResults(selectedCourse)

                btnStartAttendance.visibility = View.VISIBLE
                btnStopAttendance.visibility = View.GONE
                tvAttendanceResults.visibility = View.GONE
                recyclerViewAttendanceResults.visibility = View.GONE
                btnExportResults.visibility = View.GONE

                Toast.makeText(requireContext(), "$selectedCourse kapatıldı", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseError", "Koleksiyon silinirken hata oluştu", e)
            }
    }

    private fun deleteAttendanceResults(courseName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("attendance_results")
            .whereEqualTo("courseName", courseName)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    db.collection("attendance_results").document(document.id).delete()
                        .addOnSuccessListener {
                            Log.d("FirestoreSuccess", "Attendance result deleted: ${document.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FirestoreError", "Error deleting attendance result", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseError", "Error getting attendance results: ", e)
            }
    }

    private fun fetchAttendanceResults(courseName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("attendance_results")
            .whereEqualTo("courseName", courseName)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirebaseError", "listen:error", e)
                    return@addSnapshotListener
                }

                attendanceResults.clear()
                for (document in snapshots!!) {
                    val studentName = document.getString("studentName") ?: "Unknown"
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val course = document.getString("courseName") ?: "Unknown"

                    val attendanceResult = AttendanceResult(studentName, latitude, longitude, course)
                    attendanceResults.add(attendanceResult)
                }
                attendanceResultsAdapter.notifyDataSetChanged()
                tvAttendanceResults.text = "$courseName katılım sonuçları"
                tvAttendanceResults.visibility = View.VISIBLE
                recyclerViewAttendanceResults.visibility = View.VISIBLE
                btnExportResults.visibility = View.VISIBLE
            }
    }

    private fun listenToAttendanceChanges() {
        val selectedCourse = spinnerCourses.selectedItem.toString()
        val db = FirebaseFirestore.getInstance()

        db.collection("active_courses")
            .document(selectedCourse)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirebaseError", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val isActive = snapshot.getBoolean("active") ?: false
                    if (isActive) {
                        btnStartAttendance.visibility = View.GONE
                        btnStopAttendance.visibility = View.VISIBLE
                    } else {
                        btnStartAttendance.visibility = View.VISIBLE
                        btnStopAttendance.visibility = View.GONE
                    }
                } else {
                    btnStartAttendance.visibility = View.VISIBLE
                    btnStopAttendance.visibility = View.GONE
                }
            }
    }

    private fun exportResultsToCsv() {
        val fileName = "attendance_results.csv"
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, fileName)
        try {
            val writer = FileWriter(file)
            writer.append("Student Name,Latitude,Longitude,Course\n")
            for (result in attendanceResults) {
                writer.append("${result.studentName},${result.latitude},${result.longitude},${result.course}\n")
            }
            writer.flush()
            writer.close()
            Toast.makeText(requireContext(), "Sonuçlar $fileName dosyasına kaydedildi", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("FileError", "Error writing CSV file", e)
            Toast.makeText(requireContext(), "Sonuçlar dosyaya kaydedilirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
