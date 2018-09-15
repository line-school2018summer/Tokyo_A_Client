package intern.line.tokyoaclient

import android.app.Activity
import android.content.ContentValues
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
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
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
    private lateinit var helper: FriendDBHelper

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
                helper = FriendDBHelper(this)
            } catch (e: SQLiteException) {
                Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }


            try {
                fdb = helper.writableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                Toast.makeText(this, "writable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("writable error: ${e.toString()}")
            }
        }

        getFriend(userId)

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        alreadyFriendList.setOnItemClickListener { _, view, _, _ ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val friendName = view.findViewById<TextView>(R.id.nameTextView).text.toString()
            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
            val roomId: Int = num1 + num2
            goToTalk(roomId, friendName)
        }
        searchFriendResultList.setOnItemClickListener { _, view, position, _ ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val friendName = view.findViewById<TextView>(R.id.nameTextView).text.toString()
            val pathToFile = newFriendData[position].pathToFile
            if(USE_LOCAL_DB) {
                addFriendToLocalDB(friendId, friendName, pathToFile) // 本来ならaddFriendの中でDBへの書き込みの成功が確認できてからlocalDBに追加するべき
            }
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

    private fun getFriend(userId: String) {
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    friendIdData.addAll(it.map{friend ->  friend.friendId})
                    // Toast.makeText(context, "get friend list succeeded", Toast.LENGTH_SHORT).show()
                    // println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(this, "get friend list failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
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
                        if(u.id in friendIdData) {
                            getFriendName(u.id, alreadyFriendData, alreadyFriendAdapter)
                        } else {
                            getFriendName(u.id, newFriendData, newFriendAdapter)
                        }
                    }
                    // Toast.makeText(this, "search friend succeeded", Toast.LENGTH_SHORT).show()
                    // println("search friend succeeded: $it")
                }, {
                    Toast.makeText(this, "search friend failed: $it", Toast.LENGTH_LONG).show()
                    println("search friend failed: $it")
                })
    }

    private fun getFriendName(friendId: String, data: ArrayList<UserProfileWithImageUrl>, adapter: UserListAdapterWithImage?) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(this, "get name succeeded", Toast.LENGTH_SHORT).show()
                    // println("get name succeeded: $it")
                    getIcon(it.id, it.name, data, adapter)
                }, {
                    Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String, data: ArrayList<UserProfileWithImageUrl>, adapter: UserListAdapterWithImage?) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(this, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
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

    private fun addFriend(userId: String, friendId: String, friendName: String) {
        friendService.addFriend(userId, friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "$friendName をフレンドに追加しました！", Toast.LENGTH_SHORT).show()
                    println("add friend succeeded")
                    // フレンド画面へのintent
                    var intent = Intent()
                    intent.putExtra("newFriendId", friendId)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }, {
                    Toast.makeText(this, "add friend failed: $it", Toast.LENGTH_LONG).show()
                    println("add friend failed: $it")
                })
    }

    private fun addFriendToLocalDB(friendId: String, friendName: String, pathToFile: String) {
        var value: ContentValues
        var res: Long
        println("add friend to localDB")

        // フレンドのidの追加
        value = ContentValues().also {
            it.put("friend_id", friendId)
        }
        res = fdb.insert("friends", null, value)
        if(res < 0) {
            // error
            Toast.makeText(this, "error in INSERT", Toast.LENGTH_SHORT).show()
            return
        }

        value = ContentValues().also {
            it.put("id", friendId)
            it.put("name", friendName)
            it.put("path_to_file", pathToFile)
        }
        res = fdb.insert("friend_data", null, value)
        if(res < 0) {
            // error
            Toast.makeText(this, "error in INSERT", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun goToTalk(roomId: Int, name: String) {
        val intent = Intent(this, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
        finish()
    }
}
