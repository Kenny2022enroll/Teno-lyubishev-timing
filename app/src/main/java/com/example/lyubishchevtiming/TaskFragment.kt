package com.example.lyubishchevtiming

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.lyubishchevtiming.database.AppDatabase
import com.example.lyubishchevtiming.databinding.FragmentTaskBinding
import com.example.lyubishchevtiming.model.Task
import com.example.lyubishchevtiming.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private var tasks: List<Task> = emptyList()
    private var taskAdapter: TaskAdapter? = null
    private lateinit var db: AppDatabase
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireActivity())

        taskAdapter = TaskAdapter(requireActivity(), tasks)
        binding.gridViewTasks.adapter = taskAdapter

        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }

        binding.gridViewTasks.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val task = tasks.getOrNull(position) ?: return@OnItemClickListener
                val intent = Intent(requireContext(), TaskActivity::class.java)
                intent.putExtra("task", task)
                startActivity(intent)
            }

        // 长按任务 → 删除（软删除）：从活动列表移除，但保留其历史统计
        binding.gridViewTasks.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                val task = tasks.getOrNull(position) ?: return@OnItemLongClickListener false
                confirmDeleteTask(task)
                true
            }

        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.tasks.observe(viewLifecycleOwner, Observer { list ->
            if (list.isNotEmpty()) {
                tasks = list
                taskAdapter = TaskAdapter(requireActivity(), tasks)
                binding.gridViewTasks.adapter = taskAdapter
                showTasksList()
            } else {
                showAddButton()
            }
        })
    }

    private fun confirmDeleteTask(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_task_confirm, task.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    mainViewModel.archiveTask(task.id)
                    Toast.makeText(requireContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddButton() {
        binding.gridViewTasks.visibility = View.GONE
        binding.fabAddTask.visibility = View.VISIBLE
    }

    private fun showTasksList() {
        binding.fabAddTask.visibility = View.VISIBLE
        binding.gridViewTasks.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
