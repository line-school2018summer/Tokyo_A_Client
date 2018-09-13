package intern.line.tokyoaclient


import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.userProfileService
import kotlinx.android.synthetic.main.fragment_setting.*
import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
// private const val ARG_PARAM1 = "param1"

private lateinit var friendList: ListView
private lateinit var addFriendButton: Button
private lateinit var userIconImageView: ImageView
private var adapter: UserListAdapterWithImage? = null
private lateinit var data: ArrayList<UserProfileWithImageUrl>

private lateinit var userId: String

class FriendListFragment : Fragment() {
    private lateinit var v: View
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var helper: FriendDBHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_friend_list, container, false)
        //MyPagerAdapterで設定しておいたargumentsを取得
        userId = arguments!!.getString("userId")
        addFriendButton = v.findViewById(R.id.addFriendButton) as Button
        friendList = v.findViewById(R.id.friendList) as ListView
        userIconImageView = v.findViewById(R.id.icon) as ImageView

        data = ArrayList()
        adapter = UserListAdapterWithImage(context!!, data)
        friendList.setAdapter(adapter)

        if(USE_LOCAL_DB) {
            try {
                helper = FriendDBHelper(context)
            } catch (e: SQLiteException) {
                Toast.makeText(context, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }

            try {
                fdb = helper.readableDatabase
                Toast.makeText(context, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                Toast.makeText(context, "readable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("readable error: ${e.toString()}")
            }
        } else {
            context!!.deleteDatabase(FriendDBHelper(context).databaseName)
        }

        getOwnName(userId)
        if(USE_LOCAL_DB) {
            getFriendByLocalDB()
        } else {
            getFriend(userId)
        }

        addFriendButton.setOnClickListener {
            goToAddFriend(userId)
        }
        friendList.setOnItemClickListener { adapterView, view, position, id ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
            val roomId: Int = num1 + num2
            goToTalk(roomId, view.findViewById<TextView>(R.id.nameTextView).text.toString())
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

    private fun getFriendByLocalDB() {
        adapter?.clear() // 空にする
        val sqlstr = "select * from friends"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            getFriendNameByLocalDB(cursor.getString(0))
        }
        adapter?.notifyDataSetChanged()
    }

    private fun getOwnName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    (v.findViewById(R.id.ownNameText) as TextView).text = it.name
                    getOwnIcon(idStr)
                }, {
                    Toast.makeText(context, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getOwnIcon(idStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                    println("get image url succeeded: $it")
                    if(it.pathToFile != "") {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.pathToFile).into(userIconImageView)
                    } else {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(userIconImageView)
                    }
                }, {
                    Toast.makeText(context, "get image url failed: $it", Toast.LENGTH_LONG).show()
                    println("get image url failed: $it")
                })
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    getIcon(it.id, it.name)
                }, {
                    Toast.makeText(context, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                    println("get image url succeeded: $it")
                    if (it.pathToFile != "") {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, it.pathToFile))
                    } else {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    }
                    Collections.sort(data, NameComparator())
                }, {
                    adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparator())
                    Toast.makeText(context, "get image url failed: $it", Toast.LENGTH_LONG).show()
                    println("get image url failed: $it")
                })
    }

    private fun getFriendNameByLocalDB(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        println("get friend from localDB")
        val sqlstr = "select * from friend_data where id = '$friendId'"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            adapter?.add(UserProfileWithImageUrl(
                    id = cursor.getString(0), // id
                    name = cursor.getString(1), // name
                    pathToFile = cursor.getString(2)
            )) // created_atとupdated_atはdefault値．今後created_atとupdated_atが重要になってきたら変更しないといけない
        }
    }

    private fun goToAddFriend(userId: String) {
        val intent = Intent(context, AddFriendActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun goToTalk(roomId: Int, name: String) {
        val intent = Intent(context, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }

}
