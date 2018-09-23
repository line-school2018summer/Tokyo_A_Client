package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImage
import intern.line.tokyoaclient.HttpConnection.friendService
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList


class MemberListActivity : AppCompatActivity() {

    private var userId: String = "default"
    private var roomId: String = "room0"
    private lateinit var memberList: ListView
    private var adapter: UserListAdapterWithImage? = null
    private lateinit var data: ArrayList<UserProfileWithImageUrl>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        memberList = (findViewById(R.id.memberList) as ListView)
        data = ArrayList()
        adapter = UserListAdapterWithImage(this, data)
        memberList.setAdapter(adapter)

        userId = intent.getStringExtra("userId")
        roomId = intent.getStringExtra("roomId")
        val roomMemberIds = intent.getStringArrayExtra("roomMemberIds")
        val roomMemberNames = intent.getStringArrayExtra("roomMemberNames")
        val roomMemberIcons = intent.getStringArrayExtra("roomMemberIcons")
        for(i in 0..roomMemberIds.size-1) {
            adapter?.add(UserProfileWithImageUrl(
                    id = roomMemberIds[i],
                    name =roomMemberNames[i],
                    pathToFile = roomMemberIcons[i]
            ))
        }
        Collections.sort(data, NameComparator())
        adapter?.notifyDataSetChanged()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            finish()
        }
        return super.onKeyDown(keyCode, event)
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
                }, {
                    debugLog(this, "get friend list failed: $it")
                })
    }

    private fun getFriendName(friendId: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getIcon(it.id, it.name)
                }, {
                    debugLog(this, "get name failed: $it")
                })
    }

    private fun getIcon(idStr: String, nameStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.pathToFile != "") {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, it.pathToFile))
                    } else {
                        adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    }
                    Collections.sort(data, NameComparator())
                }, {
                    adapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    Collections.sort(data, NameComparator())
                    debugLog(this, "get image url failed: $it")
                })
    }
}