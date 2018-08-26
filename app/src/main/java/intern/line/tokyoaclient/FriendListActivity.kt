package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.*
import rx.android.schedulers.AndroidSchedulers
import intern.line.tokyoaclient.HttpConnection.friendService
import rx.schedulers.Schedulers
import java.util.UUID


class FriendListActivity : AppCompatActivity() {

    private lateinit var friendList: ListView
    private lateinit var addFriendButton: Button
    private var adapter: UserListAdapter? = null

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)

        userId = intent.getStringExtra("userId")
        addFriendButton = findViewById(R.id.addFriendButton) as Button
        friendList = findViewById(R.id.friendList) as ListView

        adapter = UserListAdapter(this, ArrayList())
        friendList.setAdapter(adapter)

        getOwnName(userId)
        getFriend(userId)

        addFriendButton.setOnClickListener {
            goToAddFriend(userId)
        }
        friendList.setOnItemClickListener { adapterView, view, position, id ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
            val roomId: Int = num1+num2
            goToTalk(roomId)
        }
    }

    private fun getFriend(userId: String) {
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for (s in it) {
                        getFriendName(s.friendId)
                    }
                    Toast.makeText(this, "get friend list succeeded", Toast.LENGTH_SHORT).show()
                    println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(this, "get friend list failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
                })
    }

    private fun getOwnName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    (findViewById(R.id.ownNameText) as TextView).text = "Hello, ${it.name}!"
                }, {
                    Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    adapter?.addAll(it)
                }, {
                    Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun goToAddFriend(userId: String) {
        val intent = Intent(this, AddFriendActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun goToTalk(roomId: Int) {
        val intent = Intent(this, TalkActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }
}
