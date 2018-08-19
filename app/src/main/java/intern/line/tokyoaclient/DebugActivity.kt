package intern.line.tokyoaclient

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.*
import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class DebugActivity : AppCompatActivity() {

    private lateinit var idText: EditText
    private lateinit var nameText: EditText
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<UserProfile>

    private lateinit var idStr: String
    private lateinit var nameStr: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        idText = findViewById(R.id.idDebug) as EditText
        nameText = findViewById(R.id.nameDebug) as EditText

        listView = findViewById(R.id.listview) as ListView
        var data: ArrayList<UserProfile> = ArrayList<UserProfile>()
        adapter = ArrayAdapter<UserProfile>(this, android.R.layout.simple_list_item_1, data)
        listView.setAdapter(adapter)


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
        idStr = idText.text.toString()
        nameStr = nameText.text.toString()
        adapter = ArrayAdapter<UserProfile>(this, android.R.layout.simple_list_item_1, ArrayList<UserProfile>())

        if (idStr != "") {
            userProfileService.getUserById(idStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        adapter.add(it)
                        Toast.makeText(this, "get id succeeded", Toast.LENGTH_LONG).show()
                        println("get id succeeded: $it")
                    }, {
                        Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                        println("get id failed: $it")
                    })
        } else {
            if (nameStr != "") {
                userProfileService.getUserByLikelyName(nameStr)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            adapter.addAll(it)
                            Toast.makeText(this, "get name succeeded", Toast.LENGTH_LONG).show()
                            println("get name succeeded: $it")
                        }, {
                            Toast.makeText(this, "get name failed: $it", Toast.LENGTH_LONG).show()
                            println("get name failed: $it")
                        })
            }
        }

        listView.setAdapter(adapter)
    }

    public fun post() {
        idStr = idText.text.toString()
        nameStr = nameText.text.toString()
        adapter = ArrayAdapter<UserProfile>(this, android.R.layout.simple_list_item_1, ArrayList<UserProfile>())

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

        listView.setAdapter(adapter)
    }

    public fun put() {
        idStr = idText.text.toString()
        nameStr = nameText.text.toString()
        adapter = ArrayAdapter<UserProfile>(this, android.R.layout.simple_list_item_1, ArrayList<UserProfile>())

        userProfileService.modifyUser(idStr, nameStr)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Toast.makeText(this, "modify succeeded", Toast.LENGTH_LONG).show()
                println("modify successed")
            }, {
                Toast.makeText(this, "modify failed: $it", Toast.LENGTH_LONG).show()
                println("modify failed: $it")
            })

        listView.setAdapter(adapter)
    }

    public fun delete() {
        idStr = idText.text.toString()
        nameStr = nameText.text.toString()
        adapter = ArrayAdapter<UserProfile>(this, android.R.layout.simple_list_item_1, ArrayList<UserProfile>())

        userProfileService.deleteUser(idStr)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.add(it)
                Toast.makeText(this, "delete succeeded", Toast.LENGTH_LONG).show()
                println("delete succeeded: $it")
            }, {
                Toast.makeText(this, "delete failed: $it", Toast.LENGTH_LONG).show()
                println("delete failed: $it")
            })

        listView.setAdapter(adapter)
    }
}

