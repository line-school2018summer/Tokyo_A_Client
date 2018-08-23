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


class FriendListActivity : AppCompatActivity() {

    private lateinit var friendList: ListView
    private lateinit var adapter: ArrayAdapter<List<Friend>>

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)

        userId = intent.getStringExtra("userId")

        friendList = (findViewById(R.id.friendListview)) as ListView

        getName(userId)
        getFriend(userId)


    }

    private fun getFriend(uid: String){
        friendService.getFriendById(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    adapter.add(it)
                    friendList.setAdapter(adapter)
                    Toast.makeText(this, "get id succeeded", Toast.LENGTH_LONG).show()
                    println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
                })
    }

    private fun getName(idStr:String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "get id succeeded", Toast.LENGTH_SHORT).show()
                    println("get id succeeded: $it")
                    (findViewById(R.id.ownNameText) as TextView).text =it.name
                }, {
                    Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                    println("get id failed: $it")
                })
    }
}

