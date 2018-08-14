package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.*



class DebugActivity : AppCompatActivity() {

    lateinit var idText: EditText
    lateinit var nameText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        idText = findViewById(R.id.idText) as EditText
        nameText = findViewById(R.id.nameText) as EditText

        //ボタンをゲットしておく
        val getButton = findViewById(R.id.get) as Button
        val postButton = findViewById(R.id.post) as Button
        val putButton = findViewById(R.id.put) as Button
        val deleteButton = findViewById(R.id.delete) as Button

        //それぞれのボタンが押されたときにメソッドを呼び出す
        getButton.setOnClickListener {
            get()
        }
        postButton.setOnClickListener {
            post()
        }
        putButton.setOnClickListener {
            put()
        }
        deleteButton.setOnClickListener {
            delete()
        }
    }

    public fun get() {
        val idStr = idText.text.toString()
        val nameStr = nameText.text.toString()

        // GETのテスト
        // res_getAllUsersは予めグローバル変数として定義して，actions.ktのgetAllUsersで代入したもの
        // res_getAllUsers_localはローカル変数としてgetAllUsers内で定義したものを返り値として返したもの
        // 何故かres_getAllUsers_localの方法はうまくいかない．res_getAllUsersも何故か1回目は入らない，
        var res_getAllUsers_local = getAllUsers().toMutableList()
        println("res_getAllUsers: " + res_getAllUsers.toString()) // res_getAllUsers: [] (1回目), res_getAllUsers: [UserProfile(id=fjalkcmipizx, name=barbar, created_at=2018-08-14 06:03:16.0, updated_at=2018-01-01 00:00:00.0), ...] (2回目)
        println("res_getAllUsers_local: " + res_getAllUsers_local.toString()) // res_getAllUsers_local: []

        getUserById(idStr)
        println("res_getUserById: " + res_getUserById.toString()) // res_getAllUsersと同様の結果
        if(res_getUserById.id != "default")
            Toast.makeText(this, "get id success: $res_getUserById", Toast.LENGTH_LONG).show()
        else
            Toast.makeText(this, "get id failure", Toast.LENGTH_LONG).show()

        getUsersByName(nameStr)
        println("res_getUsersByName: " + res_getUsersByName.toString()) // res_getAllUsersと同様の結果

        getUsersByLikelyName(nameStr)
        println("res_getUsersByLikelyName: " + res_getUsersByLikelyName.toString()) // res_getAllUsersと同様の結果
    }

    public fun post() {
        val idStr = idText.text.toString()
        val nameStr = nameText.text.toString()
        // POSTのテスト
        // actions.ktのcreateAccountでonFailureに入って"create failure"と出てしまう
        // しかし，データベースを見てみるとちゃんと追加されている
        val res = createAccount(idStr.toString(), nameStr.toString())
        if(res) {
            Toast.makeText(this, "create success", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "create failure", Toast.LENGTH_LONG).show()
        }
    }

    public fun put() {
        val idStr = idText.text.toString()
        val nameStr = nameText.text.toString()
        // PUTのテスト
        // こちらもPOSTと同じ．failureと出るがちゃんと更新されている
        val res = modifyAccount(idStr, nameStr)
        if(res) {
            Toast.makeText(this, "modify success", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "modify failure", Toast.LENGTH_LONG).show()
        }
    }

    public fun delete() {
        val idStr = idText.text.toString()
        val nameStr = nameText.text.toString()
        // DELETEのテスト
        val res = deleteAccount(idStr)
        if(res_deleteUser.id != "default")
            Toast.makeText(this, "delete success: $res_deleteUser", Toast.LENGTH_LONG).show()
        else
            Toast.makeText(this, "delete failure", Toast.LENGTH_LONG).show()
    }
}

