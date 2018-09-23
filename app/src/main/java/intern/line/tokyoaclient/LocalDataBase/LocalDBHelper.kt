package intern.line.tokyoaclient.LocalDataBase

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.debugLog
import java.sql.Timestamp


class SelfInfoDBHelper(var context: Context?) : SQLiteOpenHelper(context, "self_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("create table info ( " +
                "id text not null, " +
                "name text not null, " +
                "path_to_file text not null" +
                ");")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //バージョンアップしたときに実行される
        //テーブルのdeleteなどを行う
        db?.execSQL("drop table if exists info")
        onCreate(db)
        Toast.makeText(context, "table updated", Toast.LENGTH_SHORT).show()
    }
}

class SelfInfoLocalDBService {
    fun addInfo(id: String, name: String, pathToFile: String, sdb: SQLiteDatabase, context: Context?) {
        val value: ContentValues = ContentValues().also {
            it.put("id", id)
            it.put("name", name)
            it.put("path_to_file", pathToFile)
        }
        val res: Long = sdb.insert("info", null, value)
        if(res < 0) {
            // error
            Toast.makeText(context, "error in INSERT", Toast.LENGTH_SHORT).show()
        }
    }

    fun getInfo(sdb: SQLiteDatabase, func: (Cursor) -> Unit) {
        val sqlstr = "select * from info"
        val cursor = sdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        func(cursor)
    }

    fun updateInfo(id: String, name: String, pathToFile: String, sdb: SQLiteDatabase) {
        val value: ContentValues = ContentValues().also {
            it.put("name", name)
            it.put("path_to_file", pathToFile)
        }
        sdb.update("info", value, "id=?", arrayOf(id))
    }

    fun updateNameInfo(id: String, name: String, sdb: SQLiteDatabase) {
        val value: ContentValues = ContentValues().also {
            it.put("name", name)
        }
        sdb.update("info", value, "id=?", arrayOf(id))
    }

    fun updatePathToFileInfo(id: String, pathToFile: String, sdb: SQLiteDatabase) {
        val value: ContentValues = ContentValues().also {
            it.put("path_to_file", pathToFile)
        }
        sdb.update("info", value, "id=?", arrayOf(id))
    }
}


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

class TalkLocalDBService {
    fun getTalkByRoomId(roomId: String, tdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from talks where room_id = '$roomId'"
        val cursor = tdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while(cursor.moveToNext()) {
            func(cursor)
        }
    }

    fun getTalkByTalkId(talkId: Long, tdb: SQLiteDatabase, func: (Cursor) -> Unit) {
        val sqlstr = "select * from talks where talk_id = '$talkId'"
        val cursor = tdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        func(cursor)
    }

    fun addTalk(talk: Talk, tdb: SQLiteDatabase, context: Context?) {
        getTalkByTalkId(talk.talkId, tdb) {
            if(it.count == 0) { // 見つからなければ，addする
                val value: ContentValues = ContentValues().also {
                    it.put("talk_id", talk.talkId)
                    it.put("sender_id", talk.senderId)
                    it.put("room_id", talk.roomId)
                    it.put("content", talk.text)
                    it.put("num_read", talk.numRead)
                    it.put("created_at", talk.createdAt.toString())
                    it.put("updated_at", talk.updatedAt.toString())
                }
                val res: Long = tdb.insert("talks", null, value)
                // debugLog(context, "INSERTed talk_id: $talk.talkId")
                if(res < 0) {
                    // error
                    Toast.makeText(context, "error in INSERT", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class FriendDBHelper(var context: Context?) : SQLiteOpenHelper(context, "friend_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        //データベースがないときに実行される
        // フレンド
        // 初期およびフレンド追加時以外は変化しないはず？
        db?.execSQL("create table friends ( " +
                "id text not null, " +
                "name text not null, " +
                "path_to_file text not null" +
                ");")

        Toast.makeText(context, "table created", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //バージョンアップしたときに実行される
        //テーブルのdeleteなどを行う
        db?.execSQL("drop table if exists friends")
        onCreate(db)
        Toast.makeText(context, "table updated", Toast.LENGTH_SHORT).show()
    }
}

class FriendLocalDBService {
    fun addFriend(id: String, name: String, pathToFile: String, fdb: SQLiteDatabase, context: Context?) {
        getFriend(id, fdb, context) {
            if (it.count == 0) { // データが見つからない場合，新規作成
                val value: ContentValues = ContentValues().also {
                    it.put("id", id)
                    it.put("name", name)
                    it.put("path_to_file", pathToFile)
                }
                val res: Long = fdb.insert("friends", null, value)
                if (res < 0) {
                    // error
                    Toast.makeText(context, "error in INSERT", Toast.LENGTH_SHORT).show()
                }
            } else { // すでにデータが存在する場合，更新
                updateFriend(id, name, pathToFile, fdb)
            }
        }
    }

    fun getAllFriend(fdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from friends"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while(cursor.moveToNext()) {
            func(cursor)
        }
    }

    fun getFriend(id: String, fdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from friends where id = '$id'"
        val cursor = fdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        func(cursor)
    }

    fun updateFriend(id: String, name: String, pathToFile: String, fdb: SQLiteDatabase) {
        val value: ContentValues = ContentValues().also {
            it.put("name", name)
            it.put("path_to_file", pathToFile)
        }
        fdb.update("friends", value, "id=?", arrayOf(id))
    }
}

class RoomDBHelper(var context: Context?) : SQLiteOpenHelper(context, "room_info.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        //データベースがないときに実行される
        //
        db?.execSQL("create table rooms ( " +
                "room_id text not null, " +
                "room_name text not null, " +
                "path_to_file text not null, " +
                "created_at text not null, " +
                "is_group integer not null, " +
                "since_talk_id integer not null, " +
                "latest_talk text not null, " +
                "latest_talk_time text not null" +
                ");")
        //
        db?.execSQL("create table room_members ( " +
                "room_id text not null, " +
                "user_id text not null" +
                ");")

        Toast.makeText(context, "table created", Toast.LENGTH_SHORT).show()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //バージョンアップしたときに実行される
        //テーブルのdeleteなどを行う
        // TODO: バージョンアップ時の挙動
        db?.execSQL("drop table if exists rooms")
        db?.execSQL("drop table if exists room_members")
        onCreate(db)
        Toast.makeText(context, "table updated", Toast.LENGTH_SHORT).show()
    }
}

class RoomLocalDBService {
    fun addRoom(id: String,
                name: String,
                pathToFile: String,
                createdAt: Timestamp,
                isGroup: Boolean,
                sinceTalkId: Long,
                latestTalk: String,
                latestTalkTime: Timestamp,
                rdb: SQLiteDatabase, context: Context?) {
        getRoomById(id, rdb, context) {
            if(it.count == 0) {
                val value: ContentValues = ContentValues().also {
                    it.put("room_id", id)
                    it.put("room_name", name)
                    it.put("path_to_file", pathToFile)
                    it.put("created_at", createdAt.toString())
                    it.put("is_group", if(isGroup) 1 else 0)
                    it.put("since_talk_id", sinceTalkId)
                    it.put("latest_talk", latestTalk)
                    it.put("latest_talk_time", latestTalkTime.toString())
                }
                val res: Long = rdb.insert("rooms", null, value)
                if(res < 0) {
                    // error
                    Toast.makeText(context, "error in INSERT", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getAllRoom(rdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from rooms"
        val cursor = rdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        while(cursor.moveToNext()) {
            func(cursor)
        }
    }

    fun getRoomById(id: String, rdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from rooms where room_id = '$id'"
        val cursor = rdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        func(cursor)
    }

    fun getRoomMembers(id: String, rdb: SQLiteDatabase, context: Context?, func: (Cursor) -> Unit) {
        val sqlstr = "select * from room_members where room_id = '$id'"
        val cursor = rdb.rawQuery(sqlstr, null)
        cursor.moveToPosition(-1)
        func(cursor)
    }

    fun addRoomMembers(roomId: String, userIds: ArrayList<String>, rdb: SQLiteDatabase, context: Context?) {
        for(u in userIds) {
            val value: ContentValues = ContentValues().also {
                it.put("room_id", roomId)
                it.put("user_id", u)
            }
            val res: Long = rdb.insert("room_members", null, value)
            if (res < 0) {
                // error
                Toast.makeText(context, "error in INSERT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateSinceTalkId(latestTalk: Talk, rdb: SQLiteDatabase, context: Context?) {
        val value: ContentValues = ContentValues().also {
            it.put("since_talk_id", latestTalk.talkId)
            it.put("latest_talk", latestTalk.text)
            it.put("latest_talk_time", latestTalk.createdAt.toString())
        }
        rdb.update("rooms", value, "room_id=?", arrayOf(latestTalk.roomId))
    }

    fun updateSinceTalkId(roomId: String, sinceTalkId: Long, latestTalk: String, latestTalkTime: Timestamp, rdb: SQLiteDatabase, context: Context?) {
        val value: ContentValues = ContentValues().also {
            it.put("since_talk_id", sinceTalkId)
            it.put("latest_talk", latestTalk)
            it.put("latest_talk_time", latestTalkTime.toString())
        }
        rdb.update("rooms", value, "room_id=?", arrayOf(roomId))
    }
}
