package com.example.lyubishchevtiming

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.lyubishchevtiming.databinding.ActivityMainBinding
import com.example.lyubishchevtiming.service.TimeTrackingService
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 不论结果都不阻断应用 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.elevation = 0f
        requestNotificationPermissionIfNeeded()

        val adapter = ViewPagerAdapter(
            this,
            supportFragmentManager,
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
        binding.viewpager.adapter = adapter

        if (intent.hasExtra("tab")) {
            Log.d(TAG, "onCreate: tab")
            binding.viewpager.setCurrentItem(1, true)
        }

        binding.tabs.setupWithViewPager(binding.viewpager)
        isTrackingServiceRunning(applicationContext)
    }

    /**
     * Android 13 (API 33) 起需要运行时申请 POST_NOTIFICATIONS 权限，
     * 以便番茄钟/超日节律休息提醒能够弹出通知。
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * 检查计时前台服务是否仍在运行（用于诊断与展示当前是否有活动会话）。
     */
    private fun isTrackingServiceRunning(context: android.content.Context): Boolean {
        val am = context.getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        val services = am.getRunningServices(Int.MAX_VALUE)
        services.forEach { service ->
            if (TimeTrackingService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
