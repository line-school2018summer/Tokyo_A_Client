package intern.line.tokyoaclient

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImageSelection
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import intern.line.tokyoaclient.LocalDataBase.FriendLocalDBService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList


class CreateGroupActivity : AppCompatActivity() {

    private var userId: String = "default"
    private lateinit var friendList: ListView
    private lateinit var counter: TextView
    private var count = 0
    private lateinit var selectedUsers: ArrayList<UserProfileWithImageUrl>
    private var adapter: UserListAdapterWithImageSelection? = null
    private lateinit var data: ArrayList<UserProfileWithImageUrl>
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var helper: FriendDBHelper

    private val REQUEST_CREATE_GROUP = 1 // request code


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        friendList = (findViewById(R.id.friendList) as ListView)
        counter = (findViewById(R.id.counter) as TextView)
        count = 0
        selectedUsers = ArrayList()
        data = ArrayList()
        adapter = UserListAdapterWithImageSelection(this, data)
        friendList.setAdapter(adapter)

        userId = intent.getStringExtra("userId")

        if (USE_LOCAL_DB) {
            try {
                helper = FriendDBHelper(this)
            } catch (e: SQLiteException) {
                Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }

            try {
                fdb = helper.readableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                Toast.makeText(this, "readable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("readable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(FriendDBHelper(this).databaseName)
        }

        if (USE_LOCAL_DB) {
            getFriendByLocalDB()
        } else {
            getFriend(userId)
        }

        val createButton = findViewById(R.id.createButton) as Button
        createButton.setOnClickListener {
            createGroup(selectedUsers)
        }

        friendList.setOnItemClickListener { _, view, position, _ ->
            val check = view.findViewById<CheckedTextView>(R.id.nameTextView)
            if (check.isChecked) {
                selectedUsers.remove(data[position])
                count--;
            } else {
                count++;
                selectedUsers.add(data[position])
            }
            counter.text = count.toString()
            check.toggle()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getFriendByLocalDB() {
        adapter?.clear() // 空にする
        FriendLocalDBService().getAllFriend(fdb, this) {
            adapter?.add(UserProfileWithImageUrl(
                    id = it.getString(0),
                    name = it.getString(1),
                    pathToFile = it.getString(2)
            ))
        }
        adapter?.notifyDataSetChanged()
    }

    private fun getFriend(userId: String) {
        adapter?.clear()
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for (s in it) {
                        getFriendName(s.friendId)
                    }
                }, {
                    debugLog(this, "get friend list failed: $it")
                })
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getIcon(it.id, it.name)
                }, {
                    debugLog(this, "get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.pathToFile != "") {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, it.pathToFile))
                    } else {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    }
                    Collections.sort(data, NameComparator())
                }, {
                    adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparator())
                    debugLog(this, "get image url failed: $it")
                })
    }

    private fun createGroup(users: ArrayList<UserProfileWithImageUrl>) {
        val intent = Intent(this, ConfigureGroupActivity::class.java)
        intent.putExtra("userId", userId)
        Collections.sort(users, NameComparator())
        intent.putExtra("userIds", users.map{it -> it.id}.toTypedArray())
        intent.putExtra("userNames", users.map{it -> it.name}.toTypedArray())
        intent.putExtra("userIcons", users.map{it -> it.pathToFile}.toTypedArray())
        startActivityForResult(intent, REQUEST_CREATE_GROUP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CREATE_GROUP && resultCode == RESULT_OK) {
            finish()
        }
    }
}