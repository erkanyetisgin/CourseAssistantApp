package com.example.courseassistantapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class StudentViewActiveCoursesFragment : Fragment(), OnMapReadyCallback {

    private lateinit var spinnerCourses: Spinner
    private lateinit var btnCheckIn: Button
    private lateinit var attendanceStatus: TextView
    private lateinit var studentName: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var activeCoursesListener: ListenerRegistration

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_view_active_courses, container, false)

        spinnerCourses = view.findViewById(R.id.spinner_courses)
        btnCheckIn = view.findViewById(R.id.btn_check_in)
        attendanceStatus = view.findViewById(R.id.attendance_status)
        mapView = view.findViewById(R.id.map_view)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        MapsInitializer.initialize(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fetchStudentInfo()
        setupCoursesListener()

        btnCheckIn.setOnClickListener {
            checkInAttendance()
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        MapsInitializer.initialize(requireContext())
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        val defaultLocation = LatLng(40.7128, -74.0060)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        spinnerCourses.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCourse = parent.getItemAtPosition(position).toString()
                updateMapForSelectedCourse(selectedCourse)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                if (parent.count > 0) {
                    val selectedCourse = parent.getItemAtPosition(0).toString()
                    updateMapForSelectedCourse(selectedCourse)
                } else {
                    googleMap?.clear()
                }
            }
        }
    }

    private fun fetchStudentInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("students").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        studentName = "$firstName $lastName"
                    } else {
                        Log.d("Firestore", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting student info: ", exception)
                }
        }
    }

    private fun setupCoursesListener() {
        val db = FirebaseFirestore.getInstance()

        activeCoursesListener = db.collection("active_courses")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirebaseError", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val activeCourses = mutableListOf<String>()
                for (doc in snapshots!!) {
                    val courseName = doc.getString("courseName")
                    if (courseName != null) {
                        activeCourses.add(courseName)
                    }
                }
                setupCoursesSpinner(activeCourses)
            }
    }

    private fun setupCoursesSpinner(courses: List<String>) {
        if (googleMap == null) {
            return
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCourses.adapter = adapter

        if (courses.isNotEmpty()) {
            spinnerCourses.setSelection(0)
            updateMapForSelectedCourse(courses[0])
        } else {
            spinnerCourses.setSelection(-1)
            googleMap?.clear()
            attendanceStatus.text = ""
        }
    }

    private fun checkInAttendance() {
        val selectedCourse = spinnerCourses.selectedItem?.toString() ?: return
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        if (currentUser != null) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000
                fastestInterval = 500
                numUpdates = 1
            }


            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userId = currentUser.uid

                        db.collection("attendance_results")
                            .whereEqualTo("courseName", selectedCourse)
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    db.collection("active_courses").document(selectedCourse).get()
                                        .addOnSuccessListener { document ->
                                            if (document != null && document.exists()) {
                                                val teacherLatitude = document.getDouble("latitude") ?: 0.0
                                                val teacherLongitude = document.getDouble("longitude") ?: 0.0

                                                if (isWithinRadius(location.latitude, location.longitude, teacherLatitude, teacherLongitude, 2.0)) {
                                                    val attendanceData = hashMapOf(
                                                        "userId" to userId,
                                                        "studentName" to studentName,
                                                        "courseName" to selectedCourse,
                                                        "latitude" to location.latitude,
                                                        "longitude" to location.longitude,
                                                        "timestamp" to Timestamp.now()
                                                    )

                                                    db.collection("attendance_results")
                                                        .add(attendanceData)
                                                        .addOnSuccessListener {
                                                            Log.d("FirestoreSuccess", "Katılım başarılı: $attendanceData")
                                                            attendanceStatus.text = "Katılım başarılı"
                                                            attendanceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w("FirestoreError", "Error adding document", e)
                                                            attendanceStatus.text = "Katılım başarısız"
                                                            attendanceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                                                        }
                                                } else {
                                                    Toast.makeText(requireContext(), "Yakın değilsiniz", Toast.LENGTH_SHORT).show()
                                                    attendanceStatus.text = "Katılım başarısız"
                                                    attendanceStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                                                }
                                            } else {
                                                Log.d("Firestore", "Öğretim yeri bulunamadı")
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.w("Firestore", "Öğretim yeri alınamadı: ", exception)
                                        }
                                } else {

                                    Toast.makeText(requireContext(), "Bu derse daha önce katıldınız", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.w("Firestore", "Error checking previous attendance: ", exception)
                            }
                    } else {
                        Toast.makeText(requireContext(), "Konum alınamadı", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "Kullanıcı giriş yapmamış", Toast.LENGTH_SHORT).show()
        }
    }


    private fun isWithinRadius(lat1: Double, lon1: Double, lat2: Double, lon2: Double, radius: Double): Boolean {
        val earthRadius = 6371000

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val distance = earthRadius * c

        return distance <= radius + 5.0
    }

    private fun updateMapForSelectedCourse(courseName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("active_courses").document(courseName).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0

                    Log.d("Firestore", "Ders Konumu: $latitude, $longitude")

                    val courseLocation = LatLng(latitude, longitude)
                    googleMap?.apply {
                        clear()
                        addMarker(MarkerOptions().position(courseLocation).title(courseName))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(courseLocation, 15f))
                    }
                } else {
                    Log.d("Firestore", "Ders konumu bulunamadı")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Konum aranırken hata: ", exception)
            }
    }



    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        activeCoursesListener.remove()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
