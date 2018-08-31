package intern.line.tokyoaclient

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import java.io.File

class changeiconActivity : AppCompatActivity(), View.OnClickListener {
    private var buttonLocal: Button? = null
    private var buttonCamera: Button? = null
    private var imageView: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changeicon)
        buttonLocal = findViewById(R.id.buttonLocal) as Button
        buttonCamera = findViewById(R.id.buttonCamera) as Button
        imageView = findViewById(R.id.imageView) as ImageView
        buttonLocal!!.setOnClickListener(this)
        buttonCamera!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.buttonLocal ->
                getPicFromLocal()
            R.id.buttonCamera ->
                getPicFromCamera()

            else -> {
            }
        }
    }

    private fun getPicFromLocal() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, CODE_PHOTO_REQUEST)
    }


    private fun getPicFromCamera() {
        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE
        //撮った写真を保存された場所
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(USER_ICON))
        startActivityForResult(intent, CODE_CAMERA_REQUEST)
    }


    private fun photoClip(uri: Uri?) {
        // 调用系统中自带的图片剪裁
        val intent = Intent()
        intent.action = "com.android.camera.action.CROP"
        intent.setDataAndType(uri, "image/*")
        // crop=true　編集できる
        intent.putExtra("crop", "true")
        // aspectX aspectY 高さと広さ
        intent.putExtra("aspectX", 1)
        intent.putExtra("aspectY", 1)
        intent.putExtra("outputX", 150)
        intent.putExtra("outputY", 150)
        intent.putExtra("return-data", true)
        startActivityForResult(intent, CODE_PHOTO_CLIP)
    }

    private fun setImageToHeadView(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            val photo = extras.getParcelable<Bitmap>("data")
            imageView!!.setImageBitmap(photo)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            CODE_CAMERA_REQUEST -> if (USER_ICON.exists()) {
                photoClip(Uri.fromFile(USER_ICON))
            }
            CODE_PHOTO_REQUEST -> if (data != null) {
                photoClip(data.data)
            }
            CODE_PHOTO_CLIP -> if (data != null) {
                setImageToHeadView(data)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        //保存されたファイル
        private val USER_ICON = File(Environment.getExternalStorageDirectory(), "user_icon.jpg")
        private val CODE_PHOTO_REQUEST = 1
        private val CODE_CAMERA_REQUEST = 2
        private val CODE_PHOTO_CLIP = 3
    }
}