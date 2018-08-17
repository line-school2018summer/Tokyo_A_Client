package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class MainPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        var data:String = intent.getStringExtra("userId")
        getName(data)
    }

    fun getName(idStr:String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
            service.getUserById(idStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "get id succeeded", Toast.LENGTH_LONG).show()
                        println("get id succeeded: $it")
                        (findViewById(R.id.resultText) as TextView).text = getString(R.string.result, it.name)
                    }, {
                        Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                        println("get id failed: $it")
                    })
        }
}