package intern.line.tokyoaclient

import android.content.Context
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.util.EventLog
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.activity_talk_page.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.HttpConnection.model.TalkWithImageUrl
import intern.line.tokyoaclient.HttpConnection.talkService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class TalkActivity : AppCompatActivity() {

    private var userId: String = "default"
    private var roomId: Long = -1
    private var sinceTalkId: Long = -1
    private lateinit var talkList: ListView
    private var adapter: TalkAdapter? = null
    private lateinit var timer: TimerTask
    private lateinit var data: ArrayList<TalkWithImageUrl>

    // キーボード表示を制御するためのオブジェクト
    private lateinit var inputMethodManager: InputMethodManager
    // 背景のレイアウト
    private lateinit var mainLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_page)

        talkList = (findViewById(R.id.talkList) as ListView)
        data = ArrayList()
        adapter = TalkAdapter(this, data)
        talkList.setAdapter(adapter)

        (findViewById(R.id.roomName) as TextView).text = intent.getStringExtra("roomName")
        userId = intent.getStringExtra("userId")
        roomId = intent.getStringExtra("roomId").toLong()

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mainLayout = findViewById(R.id.main_layout) as ConstraintLayout

        //ボタンをゲットしておく
        val sendButton = findViewById(R.id.sendButton) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        sendButton.setOnClickListener {
            sendMessage()
        }

        talkList.setOnItemClickListener { adapterView, view, position, id ->
            focusOnBackground()
        }

        timer = Timer().schedule(0, 1000, { getMessage() })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        focusOnBackground()
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            timer.cancel()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun focusOnBackground() {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(mainLayout.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        // 背景にフォーカスを移す
        mainLayout.requestFocus()
    }

    private fun sendMessage() {
        if (inputText.text.toString() == "") {
            // do nothing
            // inputText.error = "コメントを入れてください"
        } else {
            val input: String = inputText.text.toString()
            talkService.addTalk(userId, roomId, input)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        // Toast.makeText(this, "send talk succeeded", Toast.LENGTH_SHORT).show()
                        println("send talk succeeded: $input")
                        getMessage()
                        inputText.editableText.clear() // 入力内容をリセットする
                        adapter?.notifyDataSetChanged()
                        // talkList.setSelection(adapter!!.count)
                    }, {
                        // Toast.makeText(this, "send talk failed: $it", Toast.LENGTH_SHORT).show()
                        println("send talk failed: $it")
                    })
        }
    }

    private fun getAllMessages() {
        talkService.getAllTalk()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    sinceTalkId = it.last().talkId
                    println(it.toString())
                }, {

                })
    }

    private fun getMessage() {
        talkService.getTalk(roomId, sinceTalkId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    println("get talk succeeded: $it")

                    if (!it.isEmpty()) {
                        // println("it[0].talkId: ${it[0].talkId}")
                        sinceTalkId = it.last().talkId
                        // adapter.addAll(it.map { talk -> talk.text })
                        for(t in it) {
                            getSendersName(t)
                        }
                    }

                    println("sinceTalkId: $sinceTalkId")
                    adapter?.notifyDataSetChanged()
                }, {
                    Toast.makeText(this, "get talk failed: $it", Toast.LENGTH_SHORT).show()
                    println("get talk failed: $it")
                })
    }

    private fun getSendersName(talk: Talk) {
        userProfileService.getUserById(talk.senderId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    getImageUrl(talk, it.name)
                }, {
                    // Toast.makeText(this, "get name failed: $it", Toast.LENGTH_SHORT).show()
                    println("get name failed: $it")
                })
    }

    private fun getImageUrl(talk: Talk, name: String) {
        imageService.getImageUrlById(talk.senderId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get image url succeeded", Toast.LENGTH_SHORT).show()
                    println("get image url succeeded: $it")
                    adapter?.add(TalkWithImageUrl(talk.talkId, name, talk.roomId, talk.text, talk.numRead, it.pathToFile, talk.createdAt, talk.updatedAt))
                    Collections.sort(data, TimeComparator())
                    talkList.setSelection(adapter!!.count)
                }, {
                    // Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image url failed: $it")
                })
    }
}
