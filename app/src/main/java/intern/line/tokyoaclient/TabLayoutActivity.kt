package intern.line.tokyoaclient

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import intern.line.tokyoaclient.Adapter.MyPagerAdapter
import kotlinx.android.synthetic.main.activity_tab_layout.*
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.view.LayoutInflater



class TabLayoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_layout)
        val userId = intent.getStringExtra("userId")
        val fragmentAdapter = MyPagerAdapter(supportFragmentManager, userId)
        viewpager_main.adapter = fragmentAdapter
        tabs_main.setupWithViewPager(viewpager_main)

        val inflater = LayoutInflater.from(this)
        val tab1 = tabs_main.getTabAt(0)
        val tab2 = tabs_main.getTabAt(1)
        val tab3 = tabs_main.getTabAt(2)
        tab1?.setIcon(R.drawable.tab_1_selecter)
        tab2?.setIcon(R.drawable.tab_2_selecter)
        tab3?.setIcon(R.drawable.tab_3_selecter)
        val tab1View = inflater.inflate(R.layout.tab_1_layout, null)
        tab1?.setCustomView(tab1View)
        val tab2View = inflater.inflate(R.layout.tab_2_layout, null)
        tab2?.setCustomView(tab2View)
        val tab3View = inflater.inflate(R.layout.tab_3_layout, null)
        tab3?.setCustomView(tab3View)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
        }
        return true
    }
}