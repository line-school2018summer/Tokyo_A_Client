package intern.line.tokyoaclient.Fragment


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.EditIconActivity
import intern.line.tokyoaclient.EditNameActivity
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.R
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

private lateinit var editNameButton: Button
private lateinit var editIcon: ImageView
private lateinit var userId: String

/**
 * A simple [Fragment] subclass.
 *
 */
class SettingFragment : Fragment() {

    private lateinit var v: View
    private lateinit var userName: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_setting, container, false)

        userId = arguments!!.getString("userId")
        getOwnName(userId)

        editNameButton = v.findViewById(R.id.editNameButton) as Button
        editNameButton.setOnClickListener {
            goNameEdit()
        }

        editIcon = v.findViewById(R.id.editIcon) as ImageView
        getIcon(userId)
        editIcon.setOnClickListener {
            goIconEdit()
        }

        return v
    }

    private fun getOwnName(idStr: String) { // idを引数に、nameをゲットする関数。ユーザー情報のGET/POSTメソッドはどっかに分離したほうがわかりやすそう。
        userProfileService.getUserById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "get name succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    (v.findViewById(R.id.userName) as TextView).text = it.name
                    userName = it.name
                }, {
                    Toast.makeText(context, "get name failed: $it", Toast.LENGTH_LONG).show()
                    println("get name failed: $it")
                })
    }

    private fun goNameEdit() {
        var intent = Intent(context, EditNameActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("userName", userName)
        startActivity(intent)
    }


    private fun getIcon(idStr: String) {
        imageService.getImageUrlById(idStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get image url succeeded: $it", Toast.LENGTH_SHORT).show()
                    println("get image url succeeded: $it")
                    if(it.pathToFile != "") {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + it.pathToFile).into(editIcon)
                    } else {
                        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(editIcon)
                    }
                }, {
                    Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + "default.jpg").into(editIcon)
                    // Toast.makeText(context, "get image url failed: $it", Toast.LENGTH_LONG).show()
                    println("get image url failed: $it")
                })
    }

    private fun goIconEdit() {
        var intent = Intent(context, EditIconActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("userName", userName)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        getOwnName(userId)
        getIcon(userId)
    }
}
