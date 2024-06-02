package com.example.courseassistantapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(var courses: List<Course>, private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View, private val onClick: (String) -> Unit) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val courseNameTextView: TextView = view.findViewById(R.id.course_name)
        private val courseDatesTextView: TextView = view.findViewById(R.id.course_dates)
        private val courseInstructorTextView: TextView = view.findViewById(R.id.course_instructor)
        private lateinit var courseId: String

        init {
            view.setOnClickListener(this)
        }

        fun bind(course: Course) {
            courseNameTextView.text = course.name
            courseDatesTextView.text = "${course.startDate} - ${course.endDate}"
            courseInstructorTextView.text = course.instructorName
            courseId = course.id
        }

        override fun onClick(v: View) {
            onClick(courseId)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    fun updateCourses(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}
