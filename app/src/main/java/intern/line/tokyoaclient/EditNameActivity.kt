package intern.line.tokyoaclient

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.LocalDataBase.SelfInfoDBHelper
import intern.line.tokyoaclient.LocalDataBase.SelfInfoLocalDBService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class EditNameActivity : AppCompatActivity() {

    private lateinit var userName: String
    private lateinit var userId: String
    // localDB
    private lateinit var sdb: SQLiteDatabase
    private lateinit var selfInfoHelper: SelfInfoDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_name)

        userName = intent.getStringExtra("userName")
        userId = intent.getStringExtra("userId")

        if (USE_LOCAL_DB) {
            try {
                selfInfoHelper = SelfInfoDBHelper(this)
            } catch (e: SQLiteException) {
                debugLog(this, "helper error: ${e.toString()}")
            }

            try {
                sdb = selfInfoHelper.writableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(this, "writable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(SelfInfoDBHelper(this).databaseName)
        }

        //ボタンをゲットしておく
        val applyButton = findViewById(R.id.applyButton) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        applyButton.setOnClickListener {
            changeName(userId, (findViewById(R.id.nameText) as TextView).text.toString())
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun changeName(userId: String, newName: String){
        userProfileService.modifyUser(userId, newName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "modify name succeeded", Toast.LENGTH_SHORT).show()
                    if(USE_LOCAL_DB)
                        updateNameLocalDB()
                    finish()
                }, {
                    debugLog(this, "modify name failed: $it")
                })
    }

    private fun updateNameLocalDB() {
        userProfileService.getUserById(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    SelfInfoLocalDBService().updateNameInfo(userId, it.name, sdb)
                }, {
                    debugLog(this, "get name failed: $it")
                })
    }
}