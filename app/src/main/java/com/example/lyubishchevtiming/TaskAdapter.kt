package com.example.lyubishchevtiming

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lyubishchevtiming.model.Task

class TaskAdapter(
    private val mContext: Context,
    private var tasks: List<Task>
) : BaseAdapter() {

    private var task: Task? = null

    fun setTasks(tasks: List<Task>) {
        this.tasks = tasks
        notifyDataSetChanged()
    }

    override fun getCount(): Int = tasks.size + 1

    override fun getItemId(position: Int): Long = 0L

    override fun getItem(position: Int): Any? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // 最后一格：添加按钮
        if (position == tasks.size) {
            val addView = LayoutInflater.from(mContext).inflate(R.layout.grid_button_add, null)
            val addButton = addView.findViewById<Button>(R.id.add_button)
            addButton.setOnClickListener {
                val intent = Intent(mContext, AddEditTaskActivity::class.java)
                mContext.startActivity(intent)
            }
            return addView
        }

        task = tasks[position]
        val view = convertView?.takeIf { it.findViewById<TextView?>(R.id.task_name_summary) != null }
            ?: LayoutInflater.from(mContext).inflate(R.layout.grid_item, null)

        val taskImageView = view.findViewById<ImageView>(R.id.task_image)
        val taskLetter = view.findViewById<TextView>(R.id.task_letter)
        val taskName = view.findViewById<TextView>(R.id.task_name_summary)

        setImageViewColor(taskImageView, task)

        task?.name?.takeIf { it.isNotEmpty() }?.let {
            taskLetter.text = it.first().toString()
        }
        taskName.text = task?.name.orEmpty()
        return view
    }

    private fun setImageViewColor(taskImageView: ImageView, task: Task?) {
        val drawable = taskImageView.background ?: return
        val colorRes = when (task?.color) {
            "red" -> R.color.red
            "glaucous" -> R.color.glaucous
            "yellow" -> R.color.yellow
            "green" -> R.color.green
            "orange" -> R.color.orange
            "peach" -> R.color.peach
            "lavender" -> R.color.lavender
            "blue" -> R.color.blue
            else -> R.color.colorPrimaryLight
        }
        drawable.setColorFilter(
            ContextCompat.getColor(mContext, colorRes),
            PorterDuff.Mode.SRC_ATOP
        )
    }
}
