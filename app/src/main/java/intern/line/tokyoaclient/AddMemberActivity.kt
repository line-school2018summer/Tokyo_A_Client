package intern.line.tokyoaclient

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.NameComparatorSelection
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImageSelection
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrlSelection
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import intern.line.tokyoaclient.LocalDataBase.FriendLocalDBService
import intern.line.tokyoaclient.LocalDataBase.RoomDBHelper
import intern.line.tokyoaclient.LocalDataBase.RoomLocalDBService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList


class AddMemberActivity : AppCompatActivity() {

    private var userId: String = "default"
    private var roomId: String = "room0"
    private lateinit var friendList: ListView
    private lateinit var counter: TextView
    private var count = 0
    private lateinit var selectedUsers: ArrayList<UserProfileWithImageUrlSelection>
    private var adapter: UserListAdapterWithImageSelection? = null
    private lateinit var data: ArrayList<UserProfileWithImageUrlSelection>
    private lateinit var roomMemberIdData: ArrayList<String>
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper


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
        roomId = intent.getStringExtra("roomId")

        if (USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(this)
                roomHelper = RoomDBHelper(this)
            } catch (e: SQLiteException) {
                Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.readableDatabase
                rdb = roomHelper.writableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(this, "readable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(FriendDBHelper(this).databaseName)
        }

        roomMemberIdData = ArrayList()

        if (USE_LOCAL_DB) {
            getRoomMemberByLocalDB()
        } else {
            // getRoomMember() // TODO
        }

        if (USE_LOCAL_DB) {
            getFriendByLocalDB()
        } else {
            getFriend(userId)
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

        val addMemberButton = findViewById(R.id.createButton) as Button
        addMemberButton.text = "招待する"
        addMemberButton.setOnClickListener {
            addMember()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getRoomMemberByLocalDB() {
        RoomLocalDBService().getRoomMembers(roomId, rdb, this) {
            while(it.moveToNext())
                roomMemberIdData.add(it.getString(1))
        }
    }

    private fun getFriendByLocalDB() {
        adapter?.clear() // 空にする
        FriendLocalDBService().getAllFriend(fdb, this) {
            if(it.getString(0) !in roomMemberIdData)
            adapter?.add(UserProfileWithImageUrlSelection(
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
                        adapter?.addAll(UserProfileWithImageUrlSelection(idStr, nameStr, it.pathToFile))
                    } else {
                        adapter?.addAll(UserProfileWithImageUrlSelection(idStr, nameStr, "default.jpg"))
                    }
                    Collections.sort(data, NameComparatorSelection())
                }, {
                    adapter?.addAll(UserProfileWithImageUrlSelection(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparatorSelection())
                    debugLog(this, "get image url failed: $it")
                })
    }

    private fun addMember() {
        for(u in selectedUsers) {
            roomService.addRoomMember(roomId, u.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    debugLog(this, "add member failed: $it")
                })
        }
        if(USE_LOCAL_DB)
            addMemberToLocalDB()

        var addMemberNames = ""
        for(u in selectedUsers) {
            addMemberNames += "と" + u.name
        }
        val intent = Intent()
        setResult(Activity.RESULT_OK, intent)
        var addMember = ArrayList<String>()
        addMember.addAll(selectedUsers.map{ it -> it.id })
        intent.putExtra("addMemberIds", addMember)
        intent.putExtra("addMemberMessage", "${addMemberNames.substring(1)}を追加しました")
        finish()
    }

    private fun addMemberToLocalDB() {
        var addMember = ArrayList<String>()
        addMember.addAll(selectedUsers.map{ it -> it.id })
        RoomLocalDBService().addRoomMembers(roomId, addMember, rdb, this)
    }
}