package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.facebook.stetho.Stetho
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import intern.line.tokyoaclient.HttpConnection.*
import kotlinx.android.synthetic.main.activity_main.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class MainActivity : AppCompatActivity() {
    //firabaseauthオブジェクトとログインユーザーオブジェクトのインスタンスを作っておく
    private var mAuth = FirebaseAuth.getInstance()
    private var currentUser: FirebaseUser? = null

    private lateinit var nameStr: String
    private lateinit var passwordStr: String
    private lateinit var mailStr: String

    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Stetho.initializeWithDefaults(this);
        setContentView(R.layout.activity_main)

        //ボタンをゲットしておく
        val signUpButton = findViewById(R.id.signup) as Button
        val signInButton = findViewById(R.id.signin) as Button
        val goButton = findViewById(R.id.goButton) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        signUpButton.setOnClickListener {
            signUp()
        }
        signInButton.setOnClickListener {
            signIn()
        }
        goButton.setOnClickListener {
            goTest()
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
                            createAccount(userId, nameStr)
                            intent(userId)
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
                        intent(userId)
                    } else {
                        //Sign in Error
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun createAccount(idStr: String, nameStr: String) {
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

    private fun intent(uid: String) {
        var intent = Intent(this, FriendListActivity::class.java)
        intent.putExtra("userId", uid)
        startActivity(intent)
    }

    private fun goTest() {
        var intent = Intent(this, AddFriendActivity::class.java)
        //intent.putExtra("userId", uid)
        startActivity(intent)
    }
}


