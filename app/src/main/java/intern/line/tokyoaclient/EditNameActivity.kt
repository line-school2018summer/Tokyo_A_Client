package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.userProfileService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class EditNameActivity : AppCompatActivity() {

    private lateinit var userName: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_name)

        userName = intent.getStringExtra("userName")
        userId = intent.getStringExtra("userId")

        //ボタンをゲットしておく
        val applyButton = findViewById(R.id.applyButton) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        applyButton.setOnClickListener {
            changeName(userId, (findViewById(R.id.nameText) as TextView).text.toString())
        }
    }

    private fun changeName(userId: String, newName: String){
        userProfileService.modifyUser(userId, newName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "modify name succeeded", Toast.LENGTH_SHORT).show()
                    finish()
                }, {
                    Toast.makeText(this, "modify name failed: $it", Toast.LENGTH_LONG).show()
                    println("modify name failed: $it")
                })
    }
}