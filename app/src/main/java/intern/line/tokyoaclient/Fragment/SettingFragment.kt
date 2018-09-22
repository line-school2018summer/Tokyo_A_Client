package intern.line.tokyoaclient.Fragment


import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.*
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.SelfInfoDBHelper
import intern.line.tokyoaclient.LocalDataBase.SelfInfoLocalDBService
import kotlinx.android.synthetic.main.fragment_setting.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class SettingFragment : Fragment() {
    private lateinit var userId: String
    // localDB
    private lateinit var sdb: SQLiteDatabase
    private lateinit var selfInfoHelper: SelfInfoDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments!!.getString("userId")

        if (USE_LOCAL_DB) {
            try {
                selfInfoHelper = SelfInfoDBHelper(context)
            } catch (e: SQLiteException) {
                debugLog(context, "helper error: ${e.toString()}")
            }

            try {
                sdb = selfInfoHelper.readableDatabase
                Toast.makeText(context, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(context, "writable error: ${e.toString()}")
            }
        } else {
            context?.deleteDatabase(SelfInfoDBHelper(context).databaseName)
        }
    }

    private lateinit var v: View
    private lateinit var editNameButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_setting, container, false)
        editNameButton = v.findViewById(R.id.editNameButton) as Button
        editNameButton.setOnClickListener {
            goNameEdit()
        }

        editIcon = v.findViewById(R.id.editIcon) as ImageView
        editIcon.setOnClickListener {
            goIconEdit()
        }

        logoutButton = v.findViewById(R.id.logoutButton) as Button
        logoutButton.setOnClickListener {
            logout()
        }

        return v
    }

    private lateinit var userName: String
    private lateinit var editIcon: ImageView

    override fun onResume() {
        super.onResume()
        if(USE_LOCAL_DB) {
            getNameAndIconByLocalDB()
        } else {
            getOwnName(userId)
            getIcon(userId)
        }
    }

    private fun getOwnName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    (v.findViewById(R.id.userName) as TextView).text = it.name
                    userName = it.name
                }, {
                    debugLog(context, "get name failed: $it")
                })
    }

    private fun getIcon(idStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.pathToFile != "") {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.pathToFile).into(editIcon)
                    } else {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(editIcon)
                    }
                }, {
                    Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(editIcon)
                    debugLog(context, "get image url failed: $it")
                })
    }

    private fun getNameAndIconByLocalDB() {
        SelfInfoLocalDBService().getInfo(sdb) {
            it.moveToNext()
            (v.findViewById(R.id.userName) as TextView).text = it.getString(1)
            userName = it.getString(1)
            Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.getString(2)).into(editIcon)
        }
    }

    private fun goNameEdit() {
        var intent = Intent(context, EditNameActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("userName", userName)
        startActivity(intent)
    }

    private fun goIconEdit() {
        var intent = Intent(context, EditIconActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun logout() {
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
