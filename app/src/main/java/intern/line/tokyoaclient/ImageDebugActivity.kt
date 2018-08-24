package intern.line.tokyoaclient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.HttpConnection.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException


class ImageDebugActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var imageView: ImageView
    private  lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_debug)
        userId = intent.getStringExtra("userId")

        imageView = findViewById(R.id.iconImage) as ImageView
        textView = findViewById(R.id.iconText) as TextView

        val getImageButton = findViewById(R.id.getImageButton) as Button
        getImageButton.setOnClickListener {
            getImage(userId)
            getImageUrl(userId)
        }

        val postImageButton = findViewById(R.id.postImageButton) as Button
        postImageButton.setOnClickListener {
            // importImage("github_logo.png")
            postImage(userId, "github_logo.png")
            getImageUrl(userId)
        }

        val putImageButton = findViewById(R.id.putImageButton) as Button
        putImageButton.setOnClickListener {
            putImage(userId, "github_logo.png")
            getImageUrl(userId)
        }

        val deleteImageButton = findViewById(R.id.deleteImageButton) as Button
        deleteImageButton.setOnClickListener {
            deleteImage(userId)
            getImageUrl(userId)
        }
    }

    private fun getImageUrl(id: String) {
        imageService.getImageUrlById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.pathToFile != "url") {
                        Toast.makeText(this, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                        println("get image url succeeded: $it")
                        textView.text = "file path: ${it.pathToFile}"
                    } else {
                        Toast.makeText(this, "Image url for id $id not found. Please post image first.", Toast.LENGTH_SHORT).show()
                        println("Image url for id $id not found. Please post image first.")
                        textView.text = "file not found"
                    }
                }, {
                    Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image url failed: $it")
                    textView.text = "file not found"
                })
    }

    private fun getImage(id: String) {
        imageView.setImageBitmap(null)
        imageService.getImageUrlById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.pathToFile != "url") {
                        Toast.makeText(this, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                        println("get image url succeeded: $it")
                        textView.text = "file path: ${it.pathToFile}"

                        Glide.with(this).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.pathToFile).into(imageView);

                    } else {
                        Toast.makeText(this, "Image url for id $id not found. Please post image first.", Toast.LENGTH_SHORT).show()
                        println("Image url for id $id not found. Please post image first.")
                        textView.text = "file not found"
                    }
                }, {
                    Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image url failed: $it")
                })
    }

    private fun postImage(id: String, pathToImage: String) {
        imageView.setImageBitmap(null)
        try {
            resources.assets.open(pathToImage).use { istream ->
                val bitmap: Bitmap = BitmapFactory.decodeStream(istream)
                imageView.setImageBitmap(bitmap) // 表示

                val content = bitmapToByteArray(bitmap) // ByteArray
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), content);
                val body = MultipartBody.Part.createFormData("file", pathToImage, requestFile); // 第一引数はサーバ側の@RequestParamの名前と一致させる

                imageService.addImage(id, body)
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
        }
    }

    private fun putImage(id: String, pathToImage: String) {
        imageView.setImageBitmap(null)
        try {
            resources.assets.open(pathToImage).use { istream ->
                val bitmap: Bitmap = BitmapFactory.decodeStream(istream)
                imageView.setImageBitmap(bitmap) // 表示

                val content = bitmapToByteArray(bitmap) // ByteArray
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), content);
                val body = MultipartBody.Part.createFormData("file", pathToImage, requestFile); // 第一引数はサーバ側の@RequestParamの名前と一致させる

                imageService.modifyImage(id, body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Toast.makeText(this, "put image succeeded", Toast.LENGTH_SHORT).show()
                            println("put image succeeded")
                        }, {
                            Toast.makeText(this, "put image failed: $it", Toast.LENGTH_SHORT).show()
                            println("put image failed: $it")
                        })
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun deleteImage(id: String) {
        imageView.setImageBitmap(null)
        imageService.deleteImage(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(this, "delete image url succeeded", Toast.LENGTH_SHORT).show()
                    println("delete image url succeeded")
                }, {
                    Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image url failed: $it")
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

    private fun bitmapToByteArray (bitmap: Bitmap): ByteArray {
        var byteArrayOutputStream = ByteArrayOutputStream()

        // PNG, クオリティー100としてbyte配列にデータを格納
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }
}