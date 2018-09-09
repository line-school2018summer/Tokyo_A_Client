package intern.line.tokyoaclient.LocalDataBase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast


class TalkDBHelper(var context: Context?) : SQLiteOpenHelper(context, "talk_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        //データベースがないときに実行される
        db?.execSQL("create table talks ( " +
                "talk_id integer not null, " +
                "sender_id text not null, " +
                "room_id text not null, " +
                "content text not null, " +
                "num_read integer not null, " +
                "created_at text not null, " +
                "updated_at text not null" +
                ");")
        Toast.makeText(context, "table created", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //バージョンアップしたときに実行される
        //テーブルのdeleteなどを行う
        db?.execSQL("drop table if exists talks")
        onCreate(db)
        Toast.makeText(context, "table updated", Toast.LENGTH_SHORT).show()
    }
}

class FriendDBHelper(var context: Context?) : SQLiteOpenHelper(context, "friend_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        //データベースがないときに実行される
        // フレンド
        // 初期およびフレンド追加時以外は変化しないはず？
        db?.execSQL("create table friends ( " +
                "friend_id text not null " +
                "friend_name text not null " +
                ");")
        // フレンド名
        // フレンドがユーザ名を変更した時に差分が出てしまう
        db?.execSQL("create table friend_name ( " +
                "id text not null, " +
                "name text not null " +
                ");")

        Toast.makeText(context, "table created", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //バージョンアップしたときに実行される
        //テーブルのdeleteなどを行う
        db?.execSQL("drop table if exists friend_name")
        onCreate(db)
        Toast.makeText(context, "table updated", Toast.LENGTH_SHORT).show()
    }
}