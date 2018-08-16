package intern.line.tokyoaclient

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import intern.line.tokyoaclient.HttpConnection.*
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity() {
    //firabaseauthオブジェクトとログインユーザーオブジェクトのインスタンスを作っておく
    var mAuth = FirebaseAuth.getInstance()
    var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ボタンをゲットしておく
        val signupButton = findViewById(R.id.signup) as Button
        val signinButton = findViewById(R.id.signin) as Button
        val testButton = findViewById(R.id.test) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        signupButton.setOnClickListener {
            signUp()
        }
        signinButton.setOnClickListener {
            signIn()
        }
        testButton.setOnClickListener {
            intent()
        }

    }


    private fun signUp(){
        //テキスト欄から入力内容を取得
        val emailStr = mailText.text.toString()
        val passwordStr = passwordText.text.toString()
        //ユーザー登録関数を呼び出す
        mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        //Registration OK
                        Toast.makeText(this, "succeeded", Toast.LENGTH_LONG).show()
                        currentUser = FirebaseAuth.getInstance().currentUser    //ユーザーインスタンスで現在のユーザーを取得するメソッド
                        var userId = currentUser?.email.toString()
                        (findViewById(R.id.resultText) as TextView).text = getString(R.string.result, userId)
                        //テキストビューに挨拶テキストを代入。そのまま"Hello, ${userId}!"とリテラルでいれるとエラー
                        //src/res/values/string.xmlファイル内でストリングを用意しておかないとダメらしい

                    } else {
                        //Registration error
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun signIn(){   //signUp()と同様
        val emailStr = mailText.text.toString()
        val passStr = passwordText.text.toString()

        mAuth.signInWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        //Sign in OK
                        Toast.makeText(this, "succeeded", Toast.LENGTH_LONG).show()
                        currentUser = FirebaseAuth.getInstance().currentUser
                        var userId = currentUser?.email
                        (findViewById(R.id.resultText) as TextView).text = getString(R.string.result, userId)
                    } else {
                        //Sign in Error
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
    }

    public fun intent() {
        var intent: Intent = Intent(this, DebugActivity::class.java)
        intent.putExtra("TestNum", 1)
        startActivity(intent)
    }
}

