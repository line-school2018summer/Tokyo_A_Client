package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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

        //それぞれのボタンが押されたときにメソッドを呼び出す
        signupButton.setOnClickListener {
            signUp()
        }
        signinButton.setOnClickListener {
            signIn()
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

                                                // GETのテスト
                        // res_getAllUsersは予めグローバル変数として定義して，actions.ktのgetAllUsersで代入したもの
                        // res_getAllUsers_localはローカル変数としてgetAllUsers内で定義したものを返り値として返したもの
                        // 何故かres_getAllUsers_localの方法はうまくいかない．res_getAllUsersも何故か1回目は入らない，
                        var res_getAllUsers_local = getAllUsers().toMutableList()
                        println("res_getAllUsers: " + res_getAllUsers.toString()) // res_getAllUsers: [] (1回目), res_getAllUsers: [UserProfile(id=fjalkcmipizx, name=barbar, created_at=2018-08-14 06:03:16.0, updated_at=2018-01-01 00:00:00.0), ...] (2回目)
                        println("res_getAllUsers_local: " + res_getAllUsers_local.toString()) // res_getAllUsers_local: []

                        getUserById("fjalkcmipizx")
                        println("res_getUserById: " + res_getUserById.toString()) // res_getAllUsersと同様の結果

                        getUsersByName("itohdak")
                        println("res_getUsersByName: " + res_getUsersByName.toString()) // res_getAllUsersと同様の結果

                        getUsersByLikelyName("itohdak")
                        println("res_getUsersByLikelyName: " + res_getUsersByLikelyName.toString()) // res_getAllUsersと同様の結果

                        // POSTのテスト
                        // actions.ktのcreateAccountでonFailureに入って"create failure"と出てしまう
                        // しかし，データベースを見てみるとちゃんと追加されている
                        var uid = currentUser?.uid
                        if(uid != null && userId != null)
                            createAccount(uid.toString(), userId.toString())

                        // PUTのテスト
                        modifyAccount("9xmdsm9fqoij", "hogehoge")

                        // DELETEのテスト
                        deleteAccount("fjalkcmipizx")

                    } else {
                        //Sign in Error
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
    }
}

