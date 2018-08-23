package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.*
import rx.android.schedulers.AndroidSchedulers
import intern.line.tokyoaclient.HttpConnection.model.Friend
import intern.line.tokyoaclient.HttpConnection.friendService
import rx.schedulers.Schedulers
import android.widget.ArrayAdapter


class FriendListActivity : AppCompatActivity() {

    private lateinit var friendList: ListView
    private lateinit var addFriendButton: Button
    private var adapter: ArrayAdapter<Friend>? = null

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)

        userId = intent.getStringExtra("userId")
        addFriendButton = findViewById(R.id.addFriendButton) as Button
        friendList = (findViewById(R.id.friendList)) as ListView


        getName(userId)
        getFriend(userId)

        addFriendButton.setOnClickListener {
            goToAddFriend(userId)
        }


    }

    private fun getFriend(userId: String) {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<Friend>())
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    adapter?.addAll(it)
                    Toast.makeText(this, "get id succeeded", Toast.LENGTH_SHORT).show()
                    println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
                })
        friendList.setAdapter(adapter)
    }

    private fun getName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    (findViewById(R.id.ownNameText) as TextView).text = it.name
                }, {
                    Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun goToAddFriend(userId: String) {
        var intent = Intent(this, AddFriendActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}
