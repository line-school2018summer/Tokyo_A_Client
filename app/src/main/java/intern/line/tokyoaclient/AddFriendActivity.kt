package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.TextView


class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchFriendResultList: ListView
    private lateinit var searchFriendButton: Button
    private lateinit var searchNameText: EditText
    private var adapter: ArrayAdapter<UserProfile>? = null

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        userId = intent.getStringExtra("userId")
        searchFriendButton = findViewById(R.id.searchFriendButton) as Button
        searchFriendResultList = (findViewById(R.id.searchFriendResultList)) as ListView
        searchNameText = (findViewById(R.id.searchNameText)) as EditText

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        searchFriendResultList.setOnItemClickListener { adapterView, view, position, id ->
            val itemTextView: TextView = view.findViewById(android.R.id.text1)
            val msg = itemTextView.text.toString()
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

    }

    private fun searchFriend() {
        var nameStr = searchNameText.text.toString()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<UserProfile>())
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
        searchFriendResultList.setAdapter(adapter)
    }

}