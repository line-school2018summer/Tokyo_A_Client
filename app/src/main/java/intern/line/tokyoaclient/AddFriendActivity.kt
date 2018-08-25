package intern.line.tokyoaclient

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import android.widget.ArrayAdapter
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

        searchFriendButton.setOnClickListener {
            searchFriend()
        }
        searchFriendResultList.setOnItemClickListener { adapterView, view, position, id ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            addFriend(userId, friendId)
            //Toast.makeText(this, "clicked: $name", Toast.LENGTH_LONG).show()
        }
    }

    private fun searchFriend() {
        var nameStr = searchNameText.text.toString()
        adapter = UserListAdapter(this, ArrayList())
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


    data class ViewHolder(val nameTextView: TextView, val idTextView: TextView)

    class UserListAdapter(context: Context, users: List<UserProfile>) : ArrayAdapter<UserProfile>(context, 0, users) {
        private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var view = convertView
            var holder: ViewHolder

            if (view == null) {
                view = layoutInflater.inflate(R.layout.friend_list_item, parent, false)
                holder = ViewHolder(
                        (view.findViewById(R.id.nameTextView) as TextView),
                        (view.findViewById(R.id.idTextView) as TextView)
                )
                view.tag = holder
            } else {
                holder = view.tag as ViewHolder
            }

            val user = getItem(position) as UserProfile
            holder.nameTextView.text = user.name
            holder.idTextView.text = user.id
            return view!!
        }
    }
}
