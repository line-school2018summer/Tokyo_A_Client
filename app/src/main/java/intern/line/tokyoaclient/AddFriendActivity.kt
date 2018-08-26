package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import android.widget.TextView
import intern.line.tokyoaclient.HttpConnection.friendService


class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchFriendResultList: ListView
    private lateinit var searchFriendButton: Button
    private lateinit var searchNameText: EditText
    private var adapter: UserListAdapter? = null

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

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        searchFriendResultList.setOnItemClickListener { adapterView, view, position, id ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            addFriend(userId, friendId)
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
}
