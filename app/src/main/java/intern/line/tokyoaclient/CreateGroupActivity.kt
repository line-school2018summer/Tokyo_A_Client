package intern.line.tokyoaclient

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImageSelection
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList


class CreateGroupActivity : AppCompatActivity() {

    private var userId: String = "default"
    private lateinit var friendList: ListView
    private lateinit var counter: TextView
    private var count = 0
    private lateinit var selectedUserId: ArrayList<String>
    private var adapter: UserListAdapterWithImageSelection? = null
    private lateinit var data: ArrayList<UserProfileWithImageUrl>
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var helper: FriendDBHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        friendList = (findViewById(R.id.friendList) as ListView)
        counter = (findViewById(R.id.counter) as TextView)
        count = 0
        selectedUserId = ArrayList()
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
            createGroup(selectedUserId)
        }

        friendList.setOnItemClickListener { _, view, position, _ ->
            val check = view.findViewById<CheckedTextView>(R.id.nameTextView)
            if (check.isChecked) {
                selectedUserId.remove(data[position].id)
                count--;
            } else {
                count++;
                selectedUserId.add(data[position].id)
            }
            counter.text = count.toString()
            check.toggle()
        }
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
                    // Toast.makeText(context, "get friend list succeeded", Toast.LENGTH_SHORT).show()
                    // println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(this, "get friend list failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
                })
    }

    private fun getFriendByLocalDB() {
        adapter?.clear() // 空にする
        val sqlstr = "select * from friends"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            getFriendNameByLocalDB(cursor.getString(0))
        }
        adapter?.notifyDataSetChanged()
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    // println("get name succeeded: $it")
                    getIcon(it.id, it.name)
                }, {
                    Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                    // println("get image url succeeded: $it")
                    if (it.pathToFile != "") {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, it.pathToFile))
                    } else {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    }
                    Collections.sort(data, NameComparator())
                }, {
                    adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparator())
                    Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_LONG).show()
                    println("get image url failed: $it")
                })
    }

    private fun getFriendNameByLocalDB(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        println("get friend from localDB")
        val sqlstr = "select * from friend_data where id = '$friendId'"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            adapter?.add(UserProfileWithImageUrl(
                    id = cursor.getString(0), // id
                    name = cursor.getString(1), // name
                    pathToFile = cursor.getString(2)
            )) // created_atとupdated_atはdefault値．今後created_atとupdated_atが重要になってきたら変更しないといけない
        }
    }

    private fun createGroup(userIds: ArrayList<String>) {

    }
}