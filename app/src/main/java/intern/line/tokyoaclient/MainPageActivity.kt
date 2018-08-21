package intern.line.tokyoaclient

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.facebook.stetho.Stetho
import intern.line.tokyoaclient.HttpConnection.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainPageActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Stetho.initializeWithDefaults(this);
        setContentView(R.layout.activity_main_page)
        userId = intent.getStringExtra("userId")
        getName(userId)

        imageView = findViewById(R.id.iconImage) as ImageView
        // importImage("github_logo.png")
        // postImage("github_logo.png")
        getImage(2)

        val goToTalkRoomButton = findViewById(R.id.goToTalkRoomButton) as Button
        goToTalkRoomButton.setOnClickListener {
            // goToTalkRoom(roomId)
            goToTalkRoom(0) // for test
        }
    }

    fun getName(idStr:String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
            userProfileService.getUserById(idStr)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(this, "get id succeeded", Toast.LENGTH_SHORT).show()
                        println("get id succeeded: $it")
                        (findViewById(R.id.resultText) as TextView).text = getString(R.string.result, it.name)
                    }, {
                        Toast.makeText(this, "get id failed: $it", Toast.LENGTH_LONG).show()
                        println("get id failed: $it")
                    })
        }

    private fun importImage(pathToImage: String) {
        try {resources.assets.open(pathToImage).use { istream ->
                val bitmap: Bitmap = BitmapFactory.decodeStream(istream)
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun postImage(pathToImage: String) {
        try {
            resources.assets.open(pathToImage).use { istream ->
                val bitmap: Bitmap = BitmapFactory.decodeStream(istream)
                imageView.setImageBitmap(bitmap) // 表示

                val content = bitmapToByteArray(bitmap) // ByteArray
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), content);
                val body = MultipartBody.Part.createFormData("file", pathToImage, requestFile); // 第一引数はサーバ側の@RequestParamの名前と一致させる

                imageService.addImage(body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Toast.makeText(this, "post image succeeded", Toast.LENGTH_SHORT).show()
                            println("post image succeeded")
                        }, {
                            Toast.makeText(this, "post image failed: $it", Toast.LENGTH_SHORT).show()
                            println("post image failed: $it")
                        })
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            println("unknown error")
        }
    }

    private fun getImage(id: Long) {
        imageService.getImageById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "get image succeeded", Toast.LENGTH_SHORT).show()
                    println("get image succeeded")
                    // val blob = it.rawData
                    // val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                    // imageView.setImageBitmap(bitmap)
                }, {
                    Toast.makeText(this, "get image failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image failed: $it")
                })
    }

    private fun bitmapToByteArray (bitmap: Bitmap): ByteArray {
        var byteArrayOutputStream = ByteArrayOutputStream()

        // PNG, クオリティー100としてbyte配列にデータを格納
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }

    private fun goToTalkRoom(roomId: Long) {
        intent(userId, roomId)
    }

    private fun intent(userId: String, roomId: Long) {
        var intent= Intent(this, TalkActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }
}