package intern.line.tokyoaclient

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v7.app.AppCompatActivity
import android.widget.*
import intern.line.tokyoaclient.HttpConnection.imageService
import kotlinx.android.synthetic.main.activity_edit_icon.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.IOException


class EditIconActivity : AppCompatActivity() {

    private val RESULT_PICK_IMAGEFILE = 1001
    private lateinit var userName: String
    private lateinit var userId: String
    private lateinit var imageView: ImageView
    private var bmp: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_icon)

        userName = intent.getStringExtra("userName")
        userId = intent.getStringExtra("userId")
        imageView = findViewById(R.id.imageView) as ImageView

        //ボタンをゲットしておく
        val goToGalleryButton = findViewById(R.id.goToGallery) as Button
        val applyButton = findViewById(R.id.applyButton) as Button
        applyButton.isClickable = false

        //それぞれのボタンが押されたときにメソッドを呼び出す
        goToGalleryButton.setOnClickListener {
            openGallery()
        }

        applyButton.setOnClickListener {
            updateIcon()
        }
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
                        imageView.setImageBitmap(bmp)
                        applyButton.isClickable = true
                    }
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

    fun updateIcon() {
        imageView.setImageBitmap(null)
        try {
            val content = bitmapToByteArray(bmp) // ByteArray
            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), content);
            val body = MultipartBody.Part.createFormData("file", "hoge.jpg", requestFile); // 第一引数はサーバ側の@RequestParamの名前と一致させる

            imageService.modifyImage(userId, body)
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

    private fun bitmapToByteArray (bitmap: Bitmap?): ByteArray {
        var byteArrayOutputStream = ByteArrayOutputStream()

        // PNG, クオリティー100としてbyte配列にデータを格納
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }
}