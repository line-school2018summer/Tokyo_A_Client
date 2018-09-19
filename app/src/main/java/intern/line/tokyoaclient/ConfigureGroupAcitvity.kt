package intern.line.tokyoaclient

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.*
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.Adapter.UserListAdapterWithImage
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.LocalDataBase.FriendDBHelper
import intern.line.tokyoaclient.LocalDataBase.RoomDBHelper
import intern.line.tokyoaclient.LocalDataBase.RoomLocalDBService
import intern.line.tokyoaclient.LocalDataBase.TalkDBHelper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.IOException
import java.sql.Time
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList


class ConfigureGroupActivity : AppCompatActivity() {

    private val RESULT_PICK_IMAGEFILE = 1001
    private lateinit var userId: String
    private lateinit var groupName: EditText
    private lateinit var groupIconImageView: ImageView
    private var defaultIcon = true
    private lateinit var groupMemberList: ListView
    private var adapter: UserListAdapterWithImage? = null
    private lateinit var data: ArrayList<UserProfileWithImageUrl>
    private var countDown = 0
    private var bmp: Bitmap? = null
    // localDB
    private lateinit var fdb: SQLiteDatabase
    private lateinit var friendHelper: FriendDBHelper
    private lateinit var rdb: SQLiteDatabase
    private lateinit var roomHelper: RoomDBHelper
    private lateinit var tdb: SQLiteDatabase
    private lateinit var talkHelper: TalkDBHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_group)

        groupMemberList = (findViewById(R.id.groupMembers) as ListView)
        data = ArrayList()
        userId = intent.getStringExtra("userId")
        val userIds = intent.getStringArrayExtra("userIds")
        val userNames = intent.getStringArrayExtra("userNames")
        val userIcons = intent.getStringArrayExtra("userIcons")
        for (i in 0..userIds.size-1) {
            data.add(UserProfileWithImageUrl(userIds[i], userNames[i], userIcons[i]))
        }
        adapter = UserListAdapterWithImage(this, data)
        groupMemberList.setAdapter(adapter)

        groupName = findViewById(R.id.groupName) as EditText
        groupIconImageView = findViewById(R.id.groupIcon) as ImageView
        Glide.with(this).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(groupIconImageView)

        if (USE_LOCAL_DB) {
            try {
                friendHelper = FriendDBHelper(this)
                roomHelper = RoomDBHelper(this)
                talkHelper = TalkDBHelper(this)
            } catch (e: SQLiteException) {
                Toast.makeText(this, "helper error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("helper error: ${e.toString()}")
            }

            try {
                fdb = friendHelper.writableDatabase
                rdb = roomHelper.writableDatabase
                tdb = talkHelper.writableDatabase
                Toast.makeText(this, "accessed to database", Toast.LENGTH_SHORT).show()
            } catch (e: SQLiteException) {
                Toast.makeText(this, "writable error: ${e.toString()}", Toast.LENGTH_SHORT).show()
                println("writable error: ${e.toString()}")
            }
        } else {
            this.deleteDatabase(FriendDBHelper(this).databaseName)
            this.deleteDatabase(RoomDBHelper(this).databaseName)
            this.deleteDatabase(TalkDBHelper(this).databaseName)
        }
        
        groupIconImageView.setOnClickListener {
            openGallery()
        }

        var createButton = findViewById(R.id.createButton) as Button
        createButton.setOnClickListener {
            createGroup(data)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンが押されたときの処理
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun createGroup(groupMembers: ArrayList<UserProfileWithImageUrl>) {
        if(groupName.text.toString() == "") {
            groupName.error = "なにか入力してください"
        } else {
            val roomId = UUID.randomUUID().toString()
            val roomName = groupName.text.toString()
            roomService.addRoomAsGroup(roomId, roomName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if(!defaultIcon)
                            addIcon(roomId)
                        else
                            addDefaultIcon(roomId)
                        countDown = data.size
                        for (s in data) {
                            addMemberToRoom(s.id, roomId)
                        }
                        addMemberToRoom(userId, roomId) // 自分
                        while(countDown > 0) {
                            println("Creating group ... $countDown")
                        }
                        if(USE_LOCAL_DB) {
                            val groupMembers: ArrayList<String> = ArrayList()
                            groupMembers.addAll(data.map { it -> it.id })
                            groupMembers.add(userId)
                            addGroupToLocalDB(groupMembers, roomId, roomName)
                        }
                        // グループメンバー選択画面へのintent
                        var intent = Intent()
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }, {
                        Toast.makeText(this, "create group failed: $it", Toast.LENGTH_LONG).show()
                        println("create group failed: $it")
                    })
        }
    }

    fun addIcon(roomId: String) {
        groupIconImageView.setImageBitmap(null)
        try {
            val content = bitmapToByteArray(bmp) // ByteArray
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), content);
            val body = MultipartBody.Part.createFormData("file", "hoge.jpg", requestFile); // 第一引数はサーバ側の@RequestParamの名前と一致させる

            imageService.addImage(roomId, body)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "put image succeeded", Toast.LENGTH_SHORT).show()
                        println("put image succeeded")
                        finish()
                    }, {
                        Toast.makeText(this, "put image failed: $it", Toast.LENGTH_SHORT).show()
                        println("put image failed: $it")
                    })
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun addDefaultIcon(roomId: String) {
        imageService.addDefaultImage(roomId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "put image succeeded", Toast.LENGTH_SHORT).show()
                        println("put image succeeded")
                        finish()
                    }, {
                        Toast.makeText(this, "put image failed: $it", Toast.LENGTH_SHORT).show()
                        println("put image failed: $it")
                    })
    }

    private fun addMemberToRoom(userId: String, roomId: String) {
        roomService.addRoomMember(roomId, userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    Toast.makeText(this, "add room member failed: $it", Toast.LENGTH_LONG).show()
                    println("add room member failed: $it")
                })
        countDown--
    }

    private fun addGroupToLocalDB(groupMemberUserIds: ArrayList<String>, roomId: String, roomName: String) {
        // ルームの情報を取得してから，localDBに保存
        roomService.getRoomByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val room = it
                    imageService.getImageUrlById(roomId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                val pathToFile = it.pathToFile
                                RoomLocalDBService().getRoomById(roomId, rdb, this) {
                                    if (it.count == 0) { // ルームがまだ存在していなければ
                                        // ルームのlocalDBへの保存
                                        RoomLocalDBService().addRoom(
                                                roomId,
                                                roomName,
                                                pathToFile,
                                                room.createdAt,
                                                true,
                                                -1,
                                                "",
                                                Timestamp(0L),
                                                rdb, this)

                                        // ルームメンバーのlocalDBへの保存
                                        RoomLocalDBService().addRoomMembers(roomId, groupMemberUserIds, rdb, this)

                                        debugLog(this, "add group room!")
                                    }
                                }
                            }, {
                                debugLog(this, "get image url failed: $it")
                            })
                }, {
                    debugLog(this, "get room failed: $it")
                })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        startActivityForResult(intent, RESULT_PICK_IMAGEFILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if(requestCode == RESULT_PICK_IMAGEFILE && resultCode == Activity.RESULT_OK) {
            if(resultData?.getData() != null) {
                var pfDescripor: ParcelFileDescriptor? = null
                try {
                    val uri: Uri = resultData.getData()
                    pfDescripor = contentResolver.openFileDescriptor(uri, "r")
                    if(pfDescripor != null) {
                        var fileDescriptor: FileDescriptor = pfDescripor.fileDescriptor
                        bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                        pfDescripor.close()
                        groupIconImageView.setImageBitmap(bmp)
                    }
                    defaultIcon = false
                } catch(e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        if(pfDescripor != null) {
                            pfDescripor.close()
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun bitmapToByteArray (bitmap: Bitmap?): ByteArray {
        var byteArrayOutputStream = ByteArrayOutputStream()

        // PNG, クオリティー100としてbyte配列にデータを格納
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }
}