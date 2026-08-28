package com.example.lyubishchevtiming

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ViewPagerAdapter(
    private val mContext: Context,
    fm: FragmentManager,
    behavior: Int
) : FragmentPagerAdapter(fm, behavior) {

    override fun getItem(position: Int): Fragment {
        return if (position == 0) TaskFragment() else SummaryFragment()
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence {
        return if (position == 0) {
            mContext.getString(R.string.tab_task)
        } else {
            mContext.getString(R.string.tab_summary)
        }
    }
}
