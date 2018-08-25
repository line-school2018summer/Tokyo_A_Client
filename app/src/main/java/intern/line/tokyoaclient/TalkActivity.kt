package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_talk_page.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.HttpConnection.talkService
import java.util.*
import kotlin.concurrent.schedule


class TalkActivity : AppCompatActivity() {

    private var userId: String = "default"
    private var roomId: Long = -1
    private var sinceTalkId: Long = -1
    private lateinit var talkList: ListView
    private lateinit var adapter: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_page)
        talkList = (findViewById(R.id.talkList) as ListView)
        var data: ArrayList<String> = ArrayList<String>()
        adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data)
        talkList.setAdapter(adapter)

        userId = intent.getStringExtra("userId")
        roomId = intent.getStringExtra("roomId").toLong()

        getMessage()

        //ボタンをゲットしておく
        val sendButton = findViewById(R.id.sendButton) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        sendButton.setOnClickListener {
            sendMessage()
            getAllMessages()
        }

        Timer().schedule(0, 1000, { getMessage() })
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
                        Toast.makeText(this, "send talk succeeded", Toast.LENGTH_SHORT).show()
                        println("send talk succeeded: $input")

                        getMessage()
                        inputText.editableText.clear() // 入力内容をリセットする
                        talkList.setSelection(adapter.count)
                    }, {
                        Toast.makeText(this, "send talk failed: $it", Toast.LENGTH_SHORT).show()
                        println("send talk failed: $it")
                    })
        }
    }

    private fun getAllMessages() {
        talkService.getAllTalk()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
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
                        println("it[0].talkId: ${it[0].talkId}")
                        sinceTalkId = it.last().talkId
                        adapter.addAll(it.map { talk -> talk.text })
                    }

                    println("sinceTalkId: $sinceTalkId")
                    talkList.setSelection(adapter.count)
                }, {
                    Toast.makeText(this, "get talk failed: $it", Toast.LENGTH_SHORT).show()
                    println("get talk failed: $it")
                })
    }

    /* no use for now on
    private fun intent(uid:String) {
        var intent= Intent(this, MainPageActivity::class.java)
        intent.putExtra("userId", uid)
        startActivity(intent)
    }
    */
}
