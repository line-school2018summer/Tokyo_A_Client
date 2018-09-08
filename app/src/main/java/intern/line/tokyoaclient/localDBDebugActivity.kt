package intern.line.tokyoaclient

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import kotlinx.android.synthetic.main.activity_localdb_debug.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.LocalDataBase.TalkDBHelper
import java.sql.Time

class localDBDebugActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var sdb: SQLiteDatabase
    private lateinit var helper: TalkDBHelper
    private lateinit var adapter: TestDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_localdb_debug)

        listView = findViewById(R.id.localDBContent) as ListView
        adapter = TestDataAdapter(this, ArrayList())
        listView.setAdapter(adapter)

        try {
            helper = TalkDBHelper(this)
        } catch(e: SQLiteException) {
            Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
        }


        try {
            sdb = helper.writableDatabase
            // sdb = helper.readableDatabase
            Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
        } catch(e: SQLiteException) {
            Toast.makeText(this, "writable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
            return
        }

        val getImageButton = findViewById(R.id.getImageButton) as Button
        getImageButton.setOnClickListener {
            getValue()
        }

        val postImageButton = findViewById(R.id.postImageButton) as Button
        postImageButton.setOnClickListener {
            insertValue(inputText.text.toString())
        }

        val putImageButton = findViewById(R.id.putImageButton) as Button
        putImageButton.setOnClickListener {
            updateValue("-1", inputText.text.toString())
        }

        val deleteImageButton = findViewById(R.id.deleteImageButton) as Button
        deleteImageButton.setOnClickListener {
            deleteValue("-1")
        }
    }

    private fun insertValue(str: String) {
        val value: ContentValues = ContentValues().also {
            it.put("talk_id", -1)
            it.put("sender_id", "sender")
            it.put("room_id", "room0")
            it.put("content", str)
            it.put("num_read", 0)
            it.put("created_at", Time(0).toString())
            it.put("updated_at", Time(0).toString())
        }
        val res: Long = sdb.insert("talks", null, value)
        if(res < 0) {
            // error
            Toast.makeText(this, "error in INSERT", Toast.LENGTH_SHORT).show()
        }
        // sdb.execSQL("insert into sample (name, years) values ($str, 20)")
    }

    private fun updateValue(id: String, newStr: String) {
        val value: ContentValues = ContentValues().also {
            it.put("content", newStr)
        }
        sdb.update("talks", value, "talk_id=?", arrayOf(id))
    }

    private fun deleteValue(id: String) {
        sdb.delete("talks", "talk_id=?", arrayOf(id))
    }

    private fun getValue() {
        adapter.clear() // 空にする
        val sqlstr = "select * from talks"
        val cursor: Cursor = sdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while (cursor.moveToNext()) {
            adapter.add(Talk(cursor.getString(0).toLong(), // talk_id
                    cursor.getString(1), // sender_id
                    cursor.getString(2), // room_id
                    cursor.getString(3), // content
                    cursor.getString(4).toLong() // num_read
            ))
        }
        adapter.notifyDataSetChanged()
    }
}
