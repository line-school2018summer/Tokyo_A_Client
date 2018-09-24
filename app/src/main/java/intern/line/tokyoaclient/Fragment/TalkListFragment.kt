package intern.line.tokyoaclient.Fragment


import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import intern.line.tokyoaclient.*
import intern.line.tokyoaclient.Adapter.RoomAdapterWithImage
import intern.line.tokyoaclient.Adapter.RoomComparator
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.RoomWithImageUrlAndLatestTalk
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.HttpConnection.talkService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.*
import intern.line.tokyoaclient.R
import intern.line.tokyoaclient.TalkActivity
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class TalkListFragment : Fragment() {
    private lateinit var userId: String
    private lateinit var data: ArrayList<RoomWithImageUrlAndLatestTalk>
    private var adapter: RoomAdapterWithImage? = null
    private var alive = true
    private var first = true
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper
    private lateinit var tdb: SQLiteDatabase
    private lateinit var talkHelper: TalkDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(context)
                roomHelper = RoomDBHelper(context)
                talkHelper = TalkDBHelper(context)
            } catch (e: SQLiteException) {
                debugLog(context, "helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.writableDatabase
                rdb = roomHelper.writableDatabase
                tdb = talkHelper.writableDatabase
                // debugLog(context, "accessed to database")
            } catch (e: SQLiteException) {
                debugLog(context, "writable error: ${e.toString()}")
            }
        } else {
            context!!.deleteDatabase(FriendDBHelper(context).databaseName)
            context!!.deleteDatabase(RoomDBHelper(context).databaseName)
            context!!.deleteDatabase(TalkDBHelper(context).databaseName)
        }

        // MyPagerAdapterで設定しておいたargumentsを取得
        userId = arguments!!.getString("userId")
        data = ArrayList()
        adapter = RoomAdapterWithImage(context!!, data)
        if(USE_LOCAL_DB) {
            getRoomByLocalDB()
            Collections.sort(data, RoomComparator())
        }
    }

    private lateinit var v: View
    private lateinit var roomList: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_talk_list, container, false)

        roomList = v.findViewById(R.id.roomListView) as ListView

        roomList.setAdapter(adapter)

        roomList.setOnItemClickListener { _, _, position, _ ->
            val roomId: String = data[position].roomId
            goToTalk(roomId, data[position].roomName, data[position].isGroup)
        }

        return v
    }

    private lateinit var timer: TimerTask

    override fun onResume() { // Fragment間の遷移のときは，呼ばれない！！
        super.onResume()
        alive = true
        first = true
        updateRoomByLocalDB()
        timer = Timer().schedule(0, 300000, { getRoom(userId) })
    }

    override fun onPause() {
        super.onPause()
        alive = false
        timer.cancel()
    }

    private fun getRoomByLocalDB() {
        adapter?.clear() // 空にする
        RoomLocalDBService().getAllRoom(rdb, context) {
            println("**************************")
            println("room number: ${it.count}")
            println("add room; roomId = ${it.getString(0)}\n")
            adapter?.add(RoomWithImageUrlAndLatestTalk(
                    it.getString(0), // roomId
                    it.getString(1), // roomName
                    it.getString(2), // pathToFile
                    it.getString(6), // latestTalk
                    Timestamp.valueOf(it.getString(7)), // latestTalkTime
                    it.getLong(5), // sinceTalkId
                    Timestamp.valueOf(it.getString(3)), // createdAt
                    if(it.getString(4).equals("1")) true else false // isGroup
            ))
        }
        adapter?.notifyDataSetChanged()
    }

    private fun updateRoomByLocalDB() {
        RoomLocalDBService().getAllRoom(rdb, context) { cursor ->
            val allUpdated = data.find{ it.roomId.equals(cursor.getString(0)) && it.roomName.equals(cursor.getString(1)) && it.pathToFile.equals(cursor.getString(2))} // 変更なし
            val exists = data.find{ it.roomId.equals(cursor.getString(0)) } // 存在はする
            if(exists != null && allUpdated == null) {
                exists.roomName = cursor.getString(1)
                exists.pathToFile = cursor.getString(2)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    // userが所属するroomを取得
    private fun getRoom(userId: String) {
        roomService.getRoomsByUserId(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    print("room number: " + it.size)
                    for(r in it) {
                        var target = searchDataWithRoomId(r.roomId)
                        if(target == null) { // ルームがまだ存在していない場合
                            if(USE_LOCAL_DB)
                                getLatestTalkWithLongPolling(r.roomId, getSinceTalkIdByLocalDB(r.roomId))
                            else
                                getLatestTalkWithLongPolling(r.roomId, -1)
                        } else {
                            if(first)
                                getLatestTalkWithLongPolling(r.roomId, getSinceTalkIdByData(r.roomId))
                        }
                    }
                    if(first)
                        first = false
                }, {
                    debugLog(context, "get room failed: $it")
                })
    }

    private fun getSinceTalkIdByData(roomId: String): Long {
        val target = data.find { it.roomId == roomId }
        if(target != null)
            return target.sinceTalkId
        else
            return -1
    }

    private fun getSinceTalkIdByLocalDB(roomId: String): Long {
        val sqlstr = "select * from rooms where room_id = '${roomId}'"
        val cursor = rdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        try {
            cursor.moveToNext()
            return cursor.getLong(5)
        } catch(e: CursorIndexOutOfBoundsException) {
            return -1
        }
    }

    private fun getLatestTalkWithLongPolling(roomId: String, sinceTalkId: Long) {
        talkService.getTalk(roomId, sinceTalkId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (!it.isEmpty()) { // 新着メッセージがある場合
                        var target = searchDataWithRoomId(roomId)
                        if(target != null) { // ルームがすでに存在している場合
                            target.latestTalk = it.last().text
                            target.latestTalkTime = it.last().createdAt
                            target.sinceTalkId = it.last().talkId
                            Collections.sort(data, RoomComparator())
                            adapter?.notifyDataSetChanged()

                            if(USE_LOCAL_DB)
                                updateRoomLocalDB(roomId, it.last().talkId, it.last().text, it.last().createdAt)
                        } else { // ルームがまだ存在していない場合，新規追加
                            getRoomNameAndIcon(RoomWithImageUrlAndLatestTalk(
                                    roomId,
                                    "hoge",
                                    "default.jpg",
                                    it.last().text,
                                    it.last().createdAt,
                                    it.last().talkId
                            ),  it.last().talkId)
                        }
                        if(USE_LOCAL_DB)
                            updateTalkLocalDB(it)
                        if(alive)
                            getLatestTalkWithLongPolling(roomId, it.last().talkId)
                    } else { // 新着メッセージがない場合
                        var target = searchDataWithRoomId(roomId)
                        if(target == null) { // ルームがまだ存在していない場合，新規追加
                            getRoomNameAndIcon(RoomWithImageUrlAndLatestTalk(
                                    roomId,
                                    "hoge",
                                    "default.jpg",
                                    "",
                                    Timestamp(0L)
                            ))
                        }
                        if(alive)
                            getLatestTalkWithLongPolling(roomId, sinceTalkId)
                    }
                }, {
                    // debugLog(context, "get talk failed: $it")
                    if(alive)
                        getLatestTalkWithLongPolling(roomId, sinceTalkId)
                })
    }

    // （ローカルに存在する）ルームの検索
    private fun searchDataWithRoomId(roomId: String): RoomWithImageUrlAndLatestTalk? {
        val target = data.find { it.roomId.equals(roomId) }
        return target
    }

    private fun getRoomNameAndIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, sinceTalkId: Long = -1) {
        roomService.getRoomByRoomId(roomWithImageUrlAndLatestTalk.roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.isGroup) { // グループ
                        roomWithImageUrlAndLatestTalk.isGroup = true
                        roomWithImageUrlAndLatestTalk.createdAt = it.createdAt
                        roomWithImageUrlAndLatestTalk.roomName = it.roomName
                        getRoomIcon(roomWithImageUrlAndLatestTalk, sinceTalkId)
                    } else { // 個人チャット
                        roomWithImageUrlAndLatestTalk.isGroup = false
                        getMembers(roomWithImageUrlAndLatestTalk, sinceTalkId)
                    }
                }, {
                    debugLog(context, "get room failed: $it")
                })
    }

    private fun getRoomIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, sinceTalkId: Long) {
        imageService.getImageUrlById(roomWithImageUrlAndLatestTalk.roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    roomWithImageUrlAndLatestTalk.pathToFile = it.pathToFile
                    val target = searchDataWithRoomId(roomWithImageUrlAndLatestTalk.roomId)
                    if(target == null) {
                        print("(1) add room; roomId = ${roomWithImageUrlAndLatestTalk.roomId}\n")
                        adapter?.add(roomWithImageUrlAndLatestTalk)
                        Collections.sort(data, RoomComparator())
                        adapter?.notifyDataSetChanged()
                    }

                    if(USE_LOCAL_DB) {
                        RoomLocalDBService().addRoom(roomWithImageUrlAndLatestTalk.roomId,
                                roomWithImageUrlAndLatestTalk.roomName,
                                "default.jpg",
                                it.createdAt,
                                roomWithImageUrlAndLatestTalk.isGroup,
                                sinceTalkId,
                                roomWithImageUrlAndLatestTalk.latestTalk,
                                roomWithImageUrlAndLatestTalk.latestTalkTime,
                                rdb, context)
                        debugLog(context, "added room to localDB: roomid is ${roomWithImageUrlAndLatestTalk.roomId}")
                    }
                }, {
                    debugLog(context, "get image url failed: $it")
                })
    }

    private fun updateRoomLocalDB(roomId: String,
                                  sinceTalkId: Long,
                                  latestTalk: String,
                                  latestTalkTime: Timestamp) {
        RoomLocalDBService().updateSinceTalkId(roomId, sinceTalkId, latestTalk, latestTalkTime, rdb, context)
    }

    private fun updateTalkLocalDB(talks: List<Talk>) {
        for(t in talks) {
            TalkLocalDBService().addTalk(t, tdb, context)
        }
    }

    private fun addMembersToLocalDB(roomId: String, userIds: ArrayList<String>) {
        RoomLocalDBService().addRoomMembers(roomId, userIds, rdb, context)
    }

    private fun getMembers(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, sinceTalkId: Long) {
        roomService.getRoomMembersByRoomId(roomWithImageUrlAndLatestTalk.roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val friendId = it.find { !it.uid.equals(userId) }?.uid
                    if(friendId != null) {
                        getFriendNameAndIcon(roomWithImageUrlAndLatestTalk, friendId, sinceTalkId)
                    } else { // 自分とフレンドになってしまった場合（普通ありえない）
                    }
                }, {
                    debugLog(context, "get room members failed: $it")
                })
    }

    private fun getFriendNameAndIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, friendId: String, sinceTalkId: Long) {
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    roomWithImageUrlAndLatestTalk.roomName = it.name
                    getFriendIcon(roomWithImageUrlAndLatestTalk, friendId, sinceTalkId)
                }, {
                    debugLog(context, "get name failed: $it")
                })
    }

    private fun getFriendIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, friendId: String, sinceTalkId: Long) {
        imageService.getImageUrlById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    roomWithImageUrlAndLatestTalk.pathToFile = it.pathToFile
                    val target = searchDataWithRoomId(roomWithImageUrlAndLatestTalk.roomId)
                        if(target == null) {
                            print("(2) add room; roomId = ${roomWithImageUrlAndLatestTalk.roomId}\n")
                            adapter?.add(roomWithImageUrlAndLatestTalk)
                            Collections.sort(data, RoomComparator())
                            adapter?.notifyDataSetChanged()
                        }

                    if(USE_LOCAL_DB) {
                        RoomLocalDBService().addRoom(roomWithImageUrlAndLatestTalk.roomId,
                                roomWithImageUrlAndLatestTalk.roomName,
                                roomWithImageUrlAndLatestTalk.pathToFile,
                                roomWithImageUrlAndLatestTalk.createdAt,
                                false,
                                sinceTalkId,
                                roomWithImageUrlAndLatestTalk.latestTalk,
                                roomWithImageUrlAndLatestTalk.latestTalkTime,
                                rdb, context)
                        addMembersToLocalDB(roomWithImageUrlAndLatestTalk.roomId, arrayListOf(userId, friendId))
                    }
                }, {
                    debugLog(context, "get image url failed: $it")
                })
    }

    private fun goToTalk(roomId: String, name: String, isGroup: Boolean) {
        val intent = Intent(context, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId)
        intent.putExtra("isGroup", isGroup)
        startActivity(intent)
    }
}
