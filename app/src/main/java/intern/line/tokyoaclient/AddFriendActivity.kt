package intern.line.tokyoaclient

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import android.widget.TextView
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper


class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchFriendResultList: ListView
    private lateinit var searchFriendButton: Button
    private lateinit var searchNameText: EditText
    private var adapter: UserListAdapter? = null
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var helper: FriendDBHelper

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        userId = intent.getStringExtra("userId")
        searchFriendButton = (findViewById(R.id.searchFriendButton)) as Button
        searchFriendResultList = (findViewById(R.id.searchFriendResultList)) as ListView
        searchNameText = (findViewById(R.id.searchNameText)) as EditText

        adapter = UserListAdapter(this, ArrayList())
        searchFriendResultList.setAdapter(adapter)

        try {
            helper = FriendDBHelper(this)
        } catch(e: SQLiteException) {
            Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
            println("helper error: ${e.toString()}")
        }


        try {
            fdb = helper.writableDatabase
            // fdb = helper.readableDatabase
            Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
        } catch(e: SQLiteException) {
            Toast.makeText(this, "writable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
            println("writable error: ${e.toString()}")
        }

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        searchFriendResultList.setOnItemClickListener { adapterView, view, position, id ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val friendName = view.findViewById<TextView>(R.id.nameTextView).text.toString()
            addFriend(userId, friendId)
            addFriendToLocalDB(friendId, friendName) // 本来ならaddFriendの中でDBへの書き込みの成功が確認できてからlocalDBに追加するべき
        }
    }

    private fun searchFriend() {
        var nameStr = searchNameText.text.toString()
        userProfileService.getUserByLikelyName(nameStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    adapter?.addAll(it)
                    Toast.makeText(this, "search friend succeeded", Toast.LENGTH_SHORT).show()
                    println("search friend succeeded: $it")
                }, {
                    Toast.makeText(this, "search friend failed: $it", Toast.LENGTH_LONG).show()
                    println("search friend failed: $it")
                })
    }

    private fun addFriend(userId: String, FriendId: String) {
        friendService.addFriend(userId, FriendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "add friend succeeded", Toast.LENGTH_SHORT).show()
                    println("search friend succeeded")
                }, {
                    Toast.makeText(this, "add friend failed: $it", Toast.LENGTH_LONG).show()
                    println("search friend failed: $it")
                })
    }

    private fun addFriendToLocalDB(friendId: String, friendName: String) {
        var value: ContentValues
        var res: Long

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
        }
        res = fdb.insert("friend_name", null, value)
        if(res < 0) {
            // error
            Toast.makeText(this, "error in INSERT", Toast.LENGTH_SHORT).show()
            return
        }
    }
}
