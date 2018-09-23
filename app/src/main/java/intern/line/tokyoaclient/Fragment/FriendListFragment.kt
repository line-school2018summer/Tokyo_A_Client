package intern.line.tokyoaclient.Fragment


import android.app.Activity.RESULT_OK
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
import intern.line.tokyoaclient.*
import intern.line.tokyoaclient.Adapter.NameComparator
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImage
import intern.line.tokyoaclient.HttpConnection.*
import intern.line.tokyoaclient.HttpConnection.model.Room
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.LocalDataBase.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList


class FriendListFragment : Fragment() {
    private lateinit var userId: String
    private lateinit var friendData: ArrayList<UserProfileWithImageUrl>
    private var friendAdapter: UserListAdapterWithImage? = null
    private lateinit var groupData: ArrayList<UserProfileWithImageUrl>
    private var groupAdapter: UserListAdapterWithImage? = null
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper
    private lateinit var sdb: SQLiteDatabase
    private lateinit var selfInfoHelper: SelfInfoDBHelper

    private val CREATE_NEW_GROUP = 1
    private val REQUEST_ADD_FRIEND = 1 // request code

    override fun onCreate(savedInstanceState: Bundle?) { // 最初の1回だけ呼ばれる
        super.onCreate(savedInstanceState)

        // localDBのセットアップ
        if (USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(context)
                roomHelper = RoomDBHelper(context)
                selfInfoHelper = SelfInfoDBHelper(context)
            } catch (e: SQLiteException) {
                debugLog(context, "helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.writableDatabase
                rdb = roomHelper.writableDatabase
                sdb = selfInfoHelper.writableDatabase
                Toast.makeText(context, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(context, "readable error: ${e.toString()}")
            }
        } else {
            context!!.deleteDatabase(FriendDBHelper(context).databaseName)
            context!!.deleteDatabase(RoomDBHelper(context).databaseName)
            context!!.deleteDatabase(TalkDBHelper(context).databaseName)
            Toast.makeText(context, "deleted database", Toast.LENGTH_SHORT).show()
        }

        // MyPagerAdapterで設定しておいたargumentsを取得
        userId = arguments!!.getString("userId")

        // フレンドの取得
        friendData = ArrayList()
        friendAdapter = UserListAdapterWithImage(context!!, friendData)
        groupData = ArrayList()
        groupAdapter = UserListAdapterWithImage(context!!, groupData)
        if (USE_LOCAL_DB) {
            getFriendByLocalDB()
            getGroupByLocalDB()
        }
    }

    private lateinit var v: View
    private lateinit var friendList: ListView
    private lateinit var groupList: ListView
    private lateinit var addFriendButton: Button
    private lateinit var createGroupButton: Button
    private lateinit var userIconImageView: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_friend_list, container, false)

        addFriendButton = v.findViewById(R.id.addFriendButton) as Button
        createGroupButton = v.findViewById(R.id.createGroupButton) as Button
        friendList = v.findViewById(R.id.friendList) as ListView
        groupList = v.findViewById(R.id.groupList) as ListView
        userIconImageView = v.findViewById(R.id.icon) as ImageView

        friendList.adapter = friendAdapter
        groupList.adapter = groupAdapter

        addFriendButton.setOnClickListener {
            goToAddFriend(userId)
        }
        createGroupButton.setOnClickListener {
            goToCreateGroup(userId)
        }
        friendList.setOnItemClickListener { _, view, _, _ ->
            val friendId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
            val roomId: String = (num1 + num2).toString()
            addMemberToRoomAfterCreateRoom(userId, friendId, roomId)
            goToTalk(roomId, view.findViewById<TextView>(R.id.nameTextView).text.toString(), false)
        }
        groupList.setOnItemClickListener { _, view, _, _ ->
            val roomId = view.findViewById<TextView>(R.id.idTextView).text.toString()
            goToTalk(roomId, view.findViewById<TextView>(R.id.nameTextView).text.toString(), true)
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        getOwnNameAndIcon(userId)
        getFriend(userId)
        getGroup(userId)
    }

    private fun getOwnNameAndIcon(idStr: String) {
        if (USE_LOCAL_DB) {
            var found = false
            SelfInfoLocalDBService().getInfo(sdb) {
                if (it.count != 0) {
                    it.moveToNext()
                    (v.findViewById(R.id.ownNameText) as TextView).text = it.getString(1)
                    val pathToFile = it.getString(2)
                    if (pathToFile != "") {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + pathToFile).into(userIconImageView)
                    } else {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(userIconImageView)
                    }
                    found = true
                }
            }
            if (found)
                return
        }
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    (v.findViewById(R.id.ownNameText) as TextView).text = it.name
                    if (USE_LOCAL_DB)
                        addSelfInfoToLocalDB(it.name)
                }, {
                    debugLog(context, "get name failed: $it")
                })
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.pathToFile != "") {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.pathToFile).into(userIconImageView)
                    } else {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(userIconImageView)
                    }
                    if (USE_LOCAL_DB)
                        addIconInfoToLocalDB(it.pathToFile)
                }, {
                    debugLog(context, "get image url failed: $it")
                })
    }

    private fun addSelfInfoToLocalDB(name: String) {
        SelfInfoLocalDBService().getInfo(sdb) {
            if(!it.moveToNext()) { // まだ登録されていなかったら
                SelfInfoLocalDBService().addInfo(userId, name, "default.jpg", sdb, context)
            }
        }
    }

    private fun addIconInfoToLocalDB(pathToFile: String) {
        SelfInfoLocalDBService().updatePathToFileInfo(userId, pathToFile, sdb)
    }

    private fun getFriendByLocalDB() {
        friendAdapter?.clear() // 空にする
        FriendLocalDBService().getAllFriend(fdb, context) {
            friendAdapter?.add(UserProfileWithImageUrl(
                    id = it.getString(0),
                    name = it.getString(1),
                    pathToFile = it.getString(2)))
            Collections.sort(friendData, NameComparator())

        }
        friendAdapter?.notifyDataSetChanged()
    }

    private fun getGroupByLocalDB() {
        groupAdapter?.clear()
        RoomLocalDBService().getAllRoom(rdb, context) {
            if (it.getInt(4) == 1) { // isGroup
                groupAdapter?.add(UserProfileWithImageUrl(
                        id = it.getString(0),
                        name = it.getString(1),
                        pathToFile = it.getString(2)))
                Collections.sort(groupData, NameComparator())
            }
        }
        groupAdapter?.notifyDataSetChanged()
    }

    private fun getFriend(userId: String) {
        friendService.getFriendById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for (s in it) {
                        getFriendName(s.friendId)
                    }
                }, {
                    debugLog(context, "get friend list failed: $it")
                })
    }

    private fun getFriendName(friendId: String) {
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getFriendIcon(it.id, it.name)
                }, {
                    debugLog(context, "get name failed: $it")
                })
    }

    private fun getFriendIcon(idStr: String, nameStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val pathToFileStr = it.pathToFile
                    val allUpdated = friendData.find{ it.id.equals(idStr) && it.name.equals(nameStr) && it.pathToFile.equals(pathToFileStr)} // 変更なし
                    val exists = friendData.find{ it.id.equals(idStr) } // 存在はする

                    if(exists != null && allUpdated == null) {
                        friendAdapter?.remove(exists)
                    }
                    if(allUpdated == null) {
                        if (pathToFileStr != "") {
                            friendAdapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, pathToFileStr))
                        } else {
                            friendAdapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                        }
                        if(USE_LOCAL_DB) {
                            FriendLocalDBService().addFriend(idStr, nameStr, pathToFileStr, fdb, context)
                            val friendId = idStr
                            val num1: Int = Math.abs(UUID.nameUUIDFromBytes(userId.toByteArray()).hashCode())
                            val num2: Int = Math.abs(UUID.nameUUIDFromBytes(friendId.toByteArray()).hashCode())
                            val roomId: String = (num1 + num2).toString()
                            RoomLocalDBService().updateRoomInfo(roomId, nameStr, pathToFileStr, rdb, context)
                        }
                    }
                    Collections.sort(friendData, NameComparator())
                }, {
                    debugLog(context, "get image url failed: $it")
                    // friendAdapter?.addAll(UserProfileWithImageUrl(idStr, nameStr, "default.jpg"))
                    // Collections.sort(friendData, NameComparator())
                })
    }

    private fun getGroup(idStr: String) {
        roomService.getRoomsByUserId(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for(r in it) {
                        getRoomName(r.roomId)
                    }
                }, {
                    debugLog(context, "get room failed: $it")
                })
    }

    private fun getRoomName(roomId: String) {
        roomService.getRoomByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.isGroup) {
                        getRoomIcon(roomId, it.roomName, it)
                    }
                }, {
                })
    }

    private fun getRoomIcon(roomId: String, roomName: String, room: Room) {
        imageService.getImageUrlById(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val pathToFileStr = it.pathToFile
                    val allUpdated = groupData.find{ it.id.equals(roomId) && it.name.equals(roomName) && it.pathToFile.equals(pathToFileStr)} // 変更なし
                    val exists = groupData.find{ it.id.equals(roomId) } // 存在はする

                    if(exists != null && allUpdated == null) {
                        groupAdapter?.remove(exists)
                    }
                    if(allUpdated == null) {
                        if (pathToFileStr != "") {
                            groupAdapter?.addAll(UserProfileWithImageUrl(roomId, roomName, pathToFileStr))
                        } else {
                            groupAdapter?.addAll(UserProfileWithImageUrl(roomId, roomName, "default.jpg"))
                        }
                        if(USE_LOCAL_DB)
                            RoomLocalDBService().addRoom(roomId, roomName, pathToFileStr, room.createdAt, room.isGroup, -1, "", Timestamp(0L), rdb, context)
                    }
                    Collections.sort(groupData, NameComparator())
                }, {
                    debugLog(context, "get image url failed: $it")
                    // groupAdapter?.addAll(UserProfileWithImageUrl(roomId, roomName, "default.jpg"))
                    Collections.sort(groupData, NameComparator())
                })
    }

    private fun addMemberToRoomAfterCreateRoom(userId: String, friendId: String, roomId: String) {
        // ルームの登録
        roomService.addRoom(roomId, "default_room")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for(u in arrayListOf(userId, friendId)) {
                        addMemberToRoom(u, roomId)
                    }
                    if(USE_LOCAL_DB)
                        addGroupToLocalDB(userId, friendId, roomId, "default_room")
                }, {
                    debugLog(context, "add room failed: $it")
                })
    }

    private fun addMemberToRoom(userId: String, roomId: String) {
        // ルームメンバー（自分とフレンド）の登録
        roomService.addRoomMember(roomId, userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    Toast.makeText(context, "add room member failed: $it", Toast.LENGTH_LONG).show()
                    println("add room member failed: $it")
                })
    }

    private fun addGroupToLocalDB(userId: String, friendId: String, roomId: String, roomName: String) {
        // ルームの情報を取得してから，localDBに保存
        roomService.getRoomByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val room = it
                    RoomLocalDBService().getRoomById(roomId, rdb, context) {
                        if(it.count == 0) { // ルームがまだ存在していなければ
                            // ルームのlocalDBへの保存
                            val member = friendData.find { it.id.equals(friendId) }
                            if(member != null) {
                                RoomLocalDBService().addRoom(
                                        roomId,
                                        member.name,
                                        member.pathToFile,
                                        room.createdAt,
                                        false,
                                        -1,
                                        "",
                                        Timestamp(0L),
                                        rdb, context)
                            } else {
                                debugLog(context, "something wrong in find member from data")
                            }

                            // ルームメンバーのlocalDBへの保存
                            RoomLocalDBService().addRoomMembers(roomId, arrayListOf(userId, friendId), rdb, context)

                            debugLog(context, "add room!")
                        }
                    }
                }, {
                    debugLog(context, "add room failed: $it")
                })
    }

    private fun goToAddFriend(userId: String) {
        val intent = Intent(context, AddFriendActivity::class.java)
        intent.putExtra("userId", userId)
        startActivityForResult(intent, REQUEST_ADD_FRIEND)
    }

    private fun goToTalk(roomId: String, name: String, isGroup: Boolean) {
        val intent = Intent(context, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId)
        intent.putExtra("isGroup", isGroup)
        startActivity(intent)
    }

    private fun goToCreateGroup(userId: String) {
        val intent = Intent(context, CreateGroupActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("mode", CREATE_NEW_GROUP)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_ADD_FRIEND && resultCode == RESULT_OK) {
            if(data != null) {
                getFriendName(data.getStringExtra("newFriendId"))
            }
        }
    }
}
