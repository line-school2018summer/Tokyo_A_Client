package intern.line.tokyoaclient.Adapter

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import intern.line.tokyoaclient.Fragment.FriendListFragment
import intern.line.tokyoaclient.Fragment.SettingFragment
import intern.line.tokyoaclient.Fragment.TalkListFragment

class MyPagerAdapter(fm: FragmentManager, uid: String) : FragmentPagerAdapter(fm) {
    val bundle = Bundle()
    val userId: String = uid

    override fun getItem(position: Int): Fragment {
        bundle.putString("userId",userId)
        when (position) {
            0 -> {
                val frg = FriendListFragment()
                frg.setArguments(bundle)
                return frg
            }
            1 -> {
                val frg = TalkListFragment()
                frg.setArguments(bundle)
                return frg
            }
            else -> {
                val frg = SettingFragment()
                frg.setArguments(bundle)
                return frg
            }
        }
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Contacts"
            1 -> "Chats"
            else -> "Settings"
        }
    }
}