package intern.line.tokyoaclient

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.facebook.stetho.Stetho
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import intern.line.tokyoaclient.Debug.localDBDebugActivity
import intern.line.tokyoaclient.HttpConnection.*
import intern.line.tokyoaclient.LocalDataBase.*
import kotlinx.android.synthetic.main.activity_main.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


public var USE_LOCAL_DB = true

class MainActivity : AppCompatActivity() {
    //firabaseauthオブジェクトとログインユーザーオブジェクトのインスタンスを作っておく
    private var mAuth = FirebaseAuth.getInstance()
    private var currentUser: FirebaseUser? = null

    private lateinit var nameStr: String
    private lateinit var passwordStr: String
    private lateinit var mailStr: String

    private lateinit var userId: String
    // localDB
    private lateinit var sdb: SQLiteDatabase
    private lateinit var selfInfoHelper: SelfInfoDBHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Stetho.initializeWithDefaults(this)
        setContentView(R.layout.activity_main)
        
        // localDBのセットアップ
        if (USE_LOCAL_DB) {
            try {
                selfInfoHelper = SelfInfoDBHelper(this)
            } catch (e: SQLiteException) {
                debugLog(this, "helper error: ${e.toString()}")
            }

            try {
                sdb = selfInfoHelper.readableDatabase
                // Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                debugLog(this, "readable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(FriendDBHelper(this).databaseName)
            this.deleteDatabase(RoomDBHelper(this).databaseName)
            this.deleteDatabase(TalkDBHelper(this).databaseName)
            this.deleteDatabase(SelfInfoDBHelper(this).databaseName)
            Toast.makeText(this, "deleted database", Toast.LENGTH_SHORT).show()
        }

        //ボタンをゲットしておく
        val signUpButton = findViewById(R.id.signup) as Button
        val signInButton = findViewById(R.id.signin) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        signUpButton.setOnClickListener {
            signUp()
        }
        signInButton.setOnClickListener {
            signIn()
        }

        findViewById<Button>(R.id.localDBButton).visibility = View.INVISIBLE
        findViewById<Button>(R.id.noLocalDBButton).visibility = View.INVISIBLE
        findViewById<Button>(R.id.localDBButton).setOnClickListener {
            USE_LOCAL_DB = true
        }
        findViewById<Button>(R.id.noLocalDBButton).setOnClickListener {
            USE_LOCAL_DB = false
        }
    }


    private fun signUp() {
        if (nameText.text.toString() == "") {
            nameText.error = "なにか入力してください"
        } else {
            //テキスト欄から入力内容を取得
            nameStr = nameText.text.toString()
            passwordStr = passwordText.text.toString()
            mailStr = mailText.text.toString()
            //ユーザー登録関数を呼び出す
            mAuth.createUserWithEmailAndPassword(mailStr, passwordStr)
                    .addOnCompleteListener { task: Task<AuthResult> ->
                        if (task.isSuccessful) {
                            //Registration OK
                            Toast.makeText(this, "succeeded", Toast.LENGTH_LONG).show()
                            currentUser = FirebaseAuth.getInstance().currentUser //ユーザーインスタンスで現在のユーザーを取得するメソッド
                            userId = currentUser?.uid.toString()
                            checkIfDataExist(userId, nameStr, true)
                        } else {
                            //Registration error
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
        }
    }

    private fun signIn() {   //signUp()と同様
        nameStr = nameText.text.toString()
        passwordStr = passwordText.text.toString()
        mailStr = mailText.text.toString()

        mAuth.signInWithEmailAndPassword(mailStr, passwordStr)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        //Sign in OK
                        Toast.makeText(this, "succeeded", Toast.LENGTH_LONG).show()
                        currentUser = FirebaseAuth.getInstance().currentUser
                        userId = currentUser?.uid.toString()
                        checkIfDataExist(userId)
                    } else {
                        //Sign in Error
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun createAccount(idStr: String, nameStr: String) {
        addUser(idStr, nameStr)
        addDefaultImage(idStr)
    }

    private fun addUser(idStr: String, nameStr: String) {
        userProfileService.addUser(idStr, nameStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "create succeeded", Toast.LENGTH_LONG).show()
                    println("create succeeded")
                }, {
                    Toast.makeText(this, "create failed: $it", Toast.LENGTH_LONG).show()
                    println("create failed: $it")
                })
    }

    private fun addDefaultImage(idStr: String) {
        // デフォルトアイコンの追加
        imageService.addDefaultImage(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "create image succeeded", Toast.LENGTH_LONG).show()
                    println("create image succeeded")
                }, {
                    Toast.makeText(this, "create image failed: $it", Toast.LENGTH_LONG).show()
                    println("create image failed: $it")
                })
    }

    private fun checkIfDataExist(uid: String, userName: String = "fuga", createAccount: Boolean = false) {
        var okayToContinue = false
        var savedName = "hoge"
        SelfInfoLocalDBService().getInfo(sdb) {
            if(it.count == 0) {
                okayToContinue = true
                if(createAccount)
                    createAccount(uid, userName)
                intent(uid)
            } else {
                it.moveToNext()
                savedName = it.getString(1)
                if(it.getString(0).equals(uid)) {
                    okayToContinue = true
                    if(createAccount)
                    createAccount(uid, userName)
                intent(uid)
                } else {
                    okayToContinue = false
                }
            }
        }
        if(!okayToContinue) {
            var dialog = ConfirmDialog()
            dialog.title = "すでにデータが存在します！"
            dialog.msg = "ユーザ名「$savedName」でログインした履歴があります．新しいユーザでログインする場合，元のデータは全消去されますが，このまま続けますか？"
            dialog.okText = "はい"
            dialog.cancelText = "いいえ"
            dialog.onOkClickListener = DialogInterface.OnClickListener { dialog, id ->
                // println("ok clicked")
                this.deleteDatabase(FriendDBHelper(this).databaseName)
                this.deleteDatabase(RoomDBHelper(this).databaseName)
                this.deleteDatabase(TalkDBHelper(this).databaseName)
                this.deleteDatabase(SelfInfoDBHelper(this).databaseName)
                Toast.makeText(this, "元のデータを削除しました", Toast.LENGTH_SHORT).show()
                okayToContinue = true
                if(createAccount)
                    createAccount(uid, userName)
                intent(uid)
            }
            dialog.onCancelClickListener = DialogInterface.OnClickListener { dialog, id ->
                // println("cancel clicked")
                okayToContinue = false
            }
            // supportFragmentManagerはAppCompatActivity(正確にはFragmentActivity)を継承したアクティビティで使用可
            dialog.show(supportFragmentManager, "tag")
        }
    }

    private fun intent(uid: String) {
        var intent = Intent(this, TabLayoutActivity::class.java)
        intent.putExtra("userId", uid)
        startActivity(intent)
        finish()
    }

    private fun goTest() {
        var intent = Intent(this, TabLayoutActivity::class.java)
        //intent.putExtra("userId", uid)
        startActivity(intent)
    }

    private fun golocalDBTest() {
        var intent = Intent(this, localDBDebugActivity::class.java)
        // intent.putExtra("userId", uid)
        startActivity(intent)
    }
}


