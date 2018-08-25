package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class MainPageActivity : AppCompatActivity() {

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        userId = intent.getStringExtra("userId")
        getName(userId)

        val goToTalkRoomButton = findViewById(R.id.goToTalkRoomButton) as Button
        goToTalkRoomButton.setOnClickListener {
            // goToTalkRoom(roomId)
            goToTalkRoom(0) // for test
        }

        val goToImageDebugButton = findViewById(R.id.goToImageDebugButton) as Button
        goToImageDebugButton.setOnClickListener {
            goToImageDebugMode()
        }
    }

    private fun getName(idStr:String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
            userProfileService.getUserById(idStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "get id succeeded", Toast.LENGTH_SHORT).show()
                        println("get id succeeded: $it")
                        (findViewById(R.id.resultText) as TextView).text = getString(R.string.result, it.name)
                    }, {
                        Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                        println("get id failed: $it")
                    })
        }

    private fun goToTalkRoom(roomId: Long) {
        intent(userId, roomId)
    }

    private fun goToImageDebugMode() {
        intentImage(userId)
    }

    private fun intent(userId: String, roomId: Long) {
        var intent= Intent(this, TalkActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }

    private fun intentImage(userId: String) {
        var intent = Intent(this, ImageDebugActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}