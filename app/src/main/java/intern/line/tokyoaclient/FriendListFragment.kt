package intern.line.tokyoaclient


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
// private const val ARG_PARAM1 = "param1"

private lateinit var friendList: ListView
private lateinit var addFriendButton: Button
private var adapter: UserListAdapter? = null

private lateinit var userId: String

class FriendListFragment : Fragment() {
    private lateinit var v: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_friend_list, container, false)
        //MyPagerAdapterで設定しておいたargumentsを取得
        userId = arguments!!.getString("userId")
        addFriendButton = v.findViewById(R.id.addFriendButton) as Button
        friendList = v.findViewById(R.id.friendList) as ListView

        adapter = UserListAdapter(context!!, ArrayList())
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
            val roomId: Int = num1 + num2
            goToTalk(roomId)
        }
        return v
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
                    Toast.makeText(context, "get friend list succeeded", Toast.LENGTH_SHORT).show()
                    println("get friend list succeeded: $it")
                }, {
                    Toast.makeText(context, "get friend list failed: $it", Toast.LENGTH_LONG).show()
                    println("get friend list failed: $it")
                })
    }

    private fun getOwnName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    (v.findViewById(R.id.ownNameText) as TextView).text = "Hello, ${it.name}!"
                }, {
                    Toast.makeText(context, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    adapter?.addAll(it)
                }, {
                    Toast.makeText(context, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun goToAddFriend(userId: String) {
        val intent = Intent(context, AddFriendActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun goToTalk(roomId: Int) {
        val intent = Intent(context, TalkActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }

}
