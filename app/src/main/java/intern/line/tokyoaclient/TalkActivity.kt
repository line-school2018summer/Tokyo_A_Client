package intern.line.tokyoaclient

import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import intern.line.tokyoaclient.Adapter.TalkAdapter
import intern.line.tokyoaclient.Adapter.TimeComparator
import kotlinx.android.synthetic.main.activity_talk_page.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.HttpConnection.model.TalkWithImageUrl
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.HttpConnection.service.UserProfileService
import intern.line.tokyoaclient.HttpConnection.talkService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.*
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList


class TalkActivity : AppCompatActivity() {

    private var userId: String = "default"
    private var roomId: String = "room0"
    private var isGroup: Boolean = false
    private var sinceTalkId: Long = -1
    private lateinit var talkList: ListView
    private var adapter: TalkAdapter? = null
    private lateinit var data: ArrayList<TalkWithImageUrl>
    private var alive = true

    private var REQUEST_ADD_MEMBER = 1001

    // キーボード表示を制御するためのオブジェクト
    private lateinit var inputMethodManager: InputMethodManager
    // 背景のレイアウト
    private lateinit var mainLayout: ConstraintLayout
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper
    private lateinit var tdb: SQLiteDatabase
    private lateinit var talkHelper: TalkDBHelper
    private lateinit var sdb: SQLiteDatabase
    private lateinit var selfInfoHelper: SelfInfoDBHelper
    private var roomMemberList: ArrayList<UserProfileWithImageUrl> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_page)

        if (USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(this)
                roomHelper = RoomDBHelper(this)
                talkHelper = TalkDBHelper(this)
                selfInfoHelper = SelfInfoDBHelper(this)
            } catch (e: SQLiteException) {
                Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.writableDatabase
                rdb = roomHelper.writableDatabase
                tdb = talkHelper.writableDatabase
                sdb = selfInfoHelper.readableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                Toast.makeText(this, "writable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("writable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(FriendDBHelper(this).databaseName)
            this.deleteDatabase(RoomDBHelper(this).databaseName)
            this.deleteDatabase(TalkDBHelper(this).databaseName)
            this.deleteDatabase(SelfInfoDBHelper(this).databaseName)
        }

        (findViewById(R.id.roomName) as TextView).text = intent.getStringExtra("roomName")
        userId = intent.getStringExtra("userId")
        roomId = intent.getStringExtra("roomId")
        isGroup = intent.getBooleanExtra("isGroup", false)
        debugLog(this, "isGroup: $isGroup")

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mainLayout = findViewById(R.id.main_layout) as ConstraintLayout

        if(USE_LOCAL_DB) { // 使うユーザの情報を確保しておく
            RoomLocalDBService().getRoomMembers(roomId, rdb, this) {
                while(it.moveToNext()) {
                    val uid = it.getString(1)
                    FriendLocalDBService().getFriend(it.getString(1), fdb, this) {
                        try {
                            it.moveToNext()
                            roomMemberList.add(UserProfileWithImageUrl(it.getString(0), it.getString(1), it.getString(2)))
                        } catch (e: CursorIndexOutOfBoundsException) {
                            // フレンドで見つからなかったらそれは自分
                            SelfInfoLocalDBService().getInfo(sdb) {
                                try {
                                    it.moveToNext()
                                    roomMemberList.add(UserProfileWithImageUrl(it.getString(0), it.getString(1), it.getString(2)))
                                } catch (e: CursorIndexOutOfBoundsException) { // 自分でもなかったらフレンドではない，知らない人
                                    getUserInfo(uid)
                                }
                            }
                        }
                    }
                }
            }
            getMember()
        } else { // TODO
            getMember()
        }

        //ボタンをゲットしておく
        val sendButton = findViewById(R.id.sendButton) as Button
        val addMemberButton = findViewById(R.id.addMemberButton) as Button
        if(isGroup)
            addMemberButton.visibility = View.VISIBLE
        else
            addMemberButton.visibility = View.INVISIBLE

        //それぞれのボタンが押されたときにメソッドを呼び出す
        sendButton.setOnClickListener {
            sendMessage()
        }

        addMemberButton.setOnClickListener {
            goAddMember()
        }

        if(USE_LOCAL_DB) {
            RoomLocalDBService().getRoomMembers(roomId, rdb, this) {
                while (it.moveToNext())
                    println("Room member: ${it.getString(1)}")
            }
        }

        talkList = (findViewById(R.id.talkList) as ListView)
        data = ArrayList()
        adapter = TalkAdapter(this, data)
        if(USE_LOCAL_DB) // 履歴から追加
            getMessageByLocalDB(roomId)
        talkList.setAdapter(adapter)
        println("Room open sinceTalkId: $sinceTalkId")

        getMessageWithLongPolling()

        talkList.setOnItemClickListener { _, _, _, _ ->
            focusOnBackground()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        focusOnBackground()
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            alive = false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun focusOnBackground() {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(mainLayout.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        // 背景にフォーカスを移す
        mainLayout.requestFocus()
    }

    private fun getMember() {
        roomService.getRoomMembersByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for(rm in it) {
                        val target = roomMemberList.find{ it.id.equals(rm.uid) }
                        if(target == null)
                            getUserInfo(rm.uid)
                    }
                }, {
                    debugLog(this, "get room member failed: $it")
                })
    }

    private fun sendMessage() {
        if (inputText.text.toString() == "") {
            // do nothing
        } else {
            val input: String = inputText.text.toString()
            talkService.addTalk(userId, roomId, input)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        // getMessage()
                        inputText.editableText.clear() // 入力内容をリセットする
                        adapter?.notifyDataSetChanged()
                        // talkList.setSelection(adapter!!.count) // 最下部にfocusする
                    }, {
                        debugLog(this, "send talk failed: $it")
                    })
        }
    }

    private fun getMessageByLocalDB(roomId: String) {
        TalkLocalDBService().getTalkByRoomId(roomId, tdb, this) {
            val member = findMemberFromList(roomMemberList, it.getString(1))
            if(member != null) {
                adapter?.add(TalkWithImageUrl(
                        it.getLong(0),
                        member.name,
                        it.getString(2), // roomId
                        it.getString(3),
                        it.getLong(4),
                        member.pathToFile,
                        Timestamp.valueOf(it.getString(5)),
                        Timestamp.valueOf(it.getString(6))))
                Collections.sort(data, TimeComparator())
                adapter?.notifyDataSetChanged()
                if(sinceTalkId < it.getLong(0))
                    sinceTalkId = it.getLong(0)
            } else {
                println("something wrong in find member from list")
            }
        }
    }

    private fun findMemberFromList(data: ArrayList<UserProfileWithImageUrl>, userId: String): UserProfileWithImageUrl? {
        return data.find { it.id.equals(userId) }
    }

    private fun getMessageWithLongPolling() {
        talkService.getTalk(roomId, sinceTalkId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    println("get talk succeeded: $it")

                    if (!it.isEmpty()) {
                        sinceTalkId = it.last().talkId
                        if(USE_LOCAL_DB)
                            RoomLocalDBService().updateSinceTalkId(it.last(), rdb, this) // 最新トークを更新
                        for(t in it) {
                            if(USE_LOCAL_DB) {
                                addMessageUsingMemberList(t)
                                addMessageToLocalDB(t) // トークを更新
                            } else {
                                addMessageUsingMemberList(t)
                                // getSendersName(t)
                            }
                        }
                    }

                    println("sinceTalkId: $sinceTalkId")
                    adapter?.notifyDataSetChanged()
                    if(alive)
                        getMessageWithLongPolling()
                }, {
                    Toast.makeText(this, "get talk failed: $it", Toast.LENGTH_SHORT).show()
                    println("get talk failed: $it")
                    if(alive)
                        getMessageWithLongPolling()
                })
    }

    private fun addMessageToLocalDB(talk: Talk) {
        TalkLocalDBService().addTalk(talk, tdb, this)
    }

    private fun addMessageUsingMemberList(talk: Talk) {
        val member = findMemberFromList(roomMemberList, talk.senderId)
        if (member != null) {
            adapter?.add(TalkWithImageUrl(
                    talk.talkId,
                    member.name,
                    talk.roomId,
                    talk.text,
                    talk.numRead,
                    member.pathToFile,
                    talk.createdAt,
                    talk.updatedAt))
        } else {
            adapter?.add(TalkWithImageUrl(
                    talk.talkId,
                    "unknown",
                    talk.roomId,
                    talk.text,
                    talk.numRead,
                    "default.jpg",
                    talk.createdAt,
                    talk.updatedAt))
        }
        Collections.sort(data, TimeComparator())
        adapter?.notifyDataSetChanged()
    }

    private fun getSendersName(talk: Talk) {
        userProfileService.getUserById(talk.senderId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getImageUrl(talk, it.name)
                }, {
                    debugLog(this, "get name failed: $it")
                })
    }

    private fun getImageUrl(talk: Talk, name: String) {
        imageService.getImageUrlById(talk.senderId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    adapter?.add(TalkWithImageUrl(talk.talkId, name, talk.roomId, talk.text, talk.numRead, it.pathToFile, talk.createdAt, talk.updatedAt))
                    Collections.sort(data, TimeComparator())
                    talkList.setSelection(adapter!!.count)
                }, {
                    debugLog(this, "get image url failed: $it")
                })
    }

    private fun getUserInfo(uid: String) {
        userProfileService.getUserById(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    getImageUrl(uid, it.name)
                }, {
                    debugLog(this, "get user failed: $it")
                })
    }

    private fun getImageUrl(uid: String, name: String) {
        imageService.getImageUrlById(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    roomMemberList.add(UserProfileWithImageUrl(uid, name, it.pathToFile))
                }, {
                    debugLog(this, "get image url failed: $it")
                })
    }

    private fun goAddMember() {
        val intent = Intent(this, AddMemberActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId)
        startActivityForResult(intent, REQUEST_ADD_MEMBER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_ADD_MEMBER && resultCode == RESULT_OK) {
            if(data != null) {
                Toast.makeText(this, data.getStringExtra("addMemberMessage"), Toast.LENGTH_SHORT).show()
                var addMemberIds = data.getStringArrayListExtra("addMemberIds")
                if(USE_LOCAL_DB) {
                    for (m in addMemberIds) {
                        FriendLocalDBService().getFriend(m, fdb, this) {
                            while (it.moveToNext())
                                roomMemberList.add(UserProfileWithImageUrl(it.getString(0), it.getString(1), it.getString(2)))
                        }
                    }
                }
            }
        }
    }
}
