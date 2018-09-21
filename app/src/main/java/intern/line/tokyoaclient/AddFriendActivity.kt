package intern.line.tokyoaclient

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import android.widget.TextView
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImage
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import intern.line.tokyoaclient.LocalDataBase.FriendLocalDBService
import intern.line.tokyoaclient.LocalDataBase.RoomDBHelper
import intern.line.tokyoaclient.LocalDataBase.RoomLocalDBService
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList


class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchFriendResultList: ListView
    private lateinit var alreadyFriendList: ListView
    private lateinit var searchFriendButton: Button
    private lateinit var searchNameText: EditText
    private var alreadyFriendAdapter: UserListAdapterWithImage? = null
    private var newFriendAdapter: UserListAdapterWithImage? = null
    private lateinit var alreadyFriendData: ArrayList<UserProfileWithImageUrl>
    private lateinit var newFriendData: ArrayList<UserProfileWithImageUrl>
    private lateinit var friendIdData: ArrayList<String>
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        userId = intent.getStringExtra("userId")
        searchFriendButton = (findViewById(R.id.searchFriendButton)) as Button
        alreadyFriendList = (findViewById(R.id.alreadyFriendList)) as ListView
        searchFriendResultList = (findViewById(R.id.searchFriendResultList)) as ListView
        searchNameText = (findViewById(R.id.searchNameText)) as EditText

        alreadyFriendData = ArrayList()
        alreadyFriendAdapter = UserListAdapterWithImage(this, alreadyFriendData)
        alreadyFriendList.setAdapter(alreadyFriendAdapter)
        newFriendData = ArrayList()
        newFriendAdapter = UserListAdapterWithImage(this, newFriendData)
        searchFriendResultList.setAdapter(newFriendAdapter)

        friendIdData = ArrayList() // すでに友達

        if(USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(this)
                roomHelper = RoomDBHelper(this)
            } catch (e: SQLiteException) {
                debugLog(this, "helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.writableDatabase
                rdb = roomHelper.writableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(this, "writable error: ${e.toString()}")
            }
        }

        if(USE_LOCAL_DB)
            getFriendByLocalDB()
        else
            getFriend(userId)

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        alreadyFriendList.setOnItemClickListener { _, view, _, _ ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val friendName = view.findViewById<TextView>(R.id.nameTextView).text.toString()
            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
            val roomId: String = (num1 + num2).toString()
            addMemberToRoomAfterCreateRoom(userId, friendId, roomId)
            goToTalk(roomId, friendName)
        }
        searchFriendResultList.setOnItemClickListener { _, view, position, _ ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val friendName = view.findViewById<TextView>(R.id.nameTextView).text.toString()
            val pathToFile = newFriendData[position].pathToFile
            if(USE_LOCAL_DB)
                addFriendToLocalDB(friendId, friendName, pathToFile) // 本来ならaddFriendの中でDBへの書き込みの成功が確認できてからlocalDBに追加するべき
            addFriend(userId, friendId, friendName)
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
        FriendLocalDBService().getAllFriend(fdb, this) {
            friendIdData.add(it.getString(0))
        }
    }

    private fun getFriend(userId: String) {
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    friendIdData.addAll(it.map{friend ->  friend.friendId})
                }, {
                    debugLog(this, "get friend list failed: $it")
                })
    }

    private fun searchFriend() {
        alreadyFriendAdapter?.clear() // 空にする
        newFriendAdapter?.clear() // 空にする
        var nameStr = searchNameText.text.toString()
        userProfileService.getUserByLikelyName(nameStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for (u in it) {
                        if(u.id in friendIdData) { // すでにフレンドに登録されていればalreadyFriendDataに，登録されていなければnewFriendDataに追加
                            getFriendName(u.id, alreadyFriendData, alreadyFriendAdapter)
                        } else {
                            getFriendName(u.id, newFriendData, newFriendAdapter)
                        }
                    }
                }, {
                    debugLog(this, "search friend failed: $it")
                })
    }

    private fun getFriendName(friendId: String, data: ArrayList<UserProfileWithImageUrl>, adapter: UserListAdapterWithImage?) {
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getIcon(it.id, it.name, data, adapter)
                }, {
                    debugLog(this, "get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String, data: ArrayList<UserProfileWithImageUrl>, adapter: UserListAdapterWithImage?) {
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
                    debugLog(this, "get image url failed: $it")
                    adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparator())
                })
    }

    private fun addFriendToLocalDB(friendId: String, friendName: String, pathToFile: String) {
        FriendLocalDBService().addFriend(friendId, friendName, pathToFile, fdb, this)
    }

    private fun addFriend(userId: String, friendId: String, friendName: String) {
        friendService.addFriend(userId, friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "$friendName をフレンドに追加しました！", Toast.LENGTH_SHORT).show()
                    // フレンド画面へのintent
                    var intent = Intent()
                    intent.putExtra("newFriendId", friendId)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }, {
                    debugLog(this, "add friend failed: $it")
                })
    }

    private fun addMemberToRoomAfterCreateRoom(userId: String, friendId: String, roomId: String) {
        // ルームの登録
        roomService.addRoom(roomId, "default_room")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for(u in arrayListOf(userId, friendId)) {
                        addMemberToRoom(u, roomId)
                    }
                    if(USE_LOCAL_DB)
                        addGroupToLocalDB(userId, friendId, roomId, "default_room")
                }, {
                    debugLog(this, "add room failed: $it")
                })
    }

    private fun addMemberToRoom(userId: String, roomId: String) {
        // ルームメンバー（自分とフレンド）の登録
        roomService.addRoomMember(roomId, userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    Toast.makeText(this, "add room member failed: $it", Toast.LENGTH_LONG).show()
                    println("add room member failed: $it")
                })
    }

    private fun addGroupToLocalDB(userId: String, friendId: String, roomId: String, roomName: String) {
        // ルームの情報を取得してから，localDBに保存
        roomService.getRoomByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val room = it
                    RoomLocalDBService().getRoomById(roomId, rdb, this) {
                        if(it.count == 0) { // ルームがまだ存在していなければ
                            // ルームのlocalDBへの保存
                            val member = alreadyFriendData.find { it.id.equals(friendId) }
                            if(member != null) {
                                RoomLocalDBService().addRoom(
                                        roomId,
                                        member.name,
                                        member.pathToFile,
                                        room.createdAt,
                                        false,
                                        -1,
                                        "",
                                        Timestamp(0L),
                                        rdb, this)
                            } else {
                                debugLog(this, "something wrong in find member from data")
                            }

                            // ルームメンバーのlocalDBへの保存
                            RoomLocalDBService().addRoomMembers(roomId, arrayListOf(userId, friendId), rdb, this)

                            debugLog(this, "add room!")
                        }
                    }
                }, {
                    debugLog(this, "add room failed: $it")
                })
    }

    private fun goToTalk(roomId: String, name: String) {
        val intent = Intent(this, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId)
        startActivity(intent)
        finish()
    }
}
