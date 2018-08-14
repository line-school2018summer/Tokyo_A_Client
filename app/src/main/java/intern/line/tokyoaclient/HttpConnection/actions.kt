package intern.line.tokyoaclient.HttpConnection

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


var res1: List<UserProfile> = mutableListOf<UserProfile>()

fun getAllUsers(): List<UserProfile> {
    // 非同期処理
    // 同期処理(以下)をするとアプリが落ちる
    // service.getAllUsers(id).execute()
    var res2: List<UserProfile> = mutableListOf<UserProfile>()
    service.getAllUsers().enqueue(object : Callback<List<UserProfile>> {
        override fun onResponse(call: Call<List<UserProfile>>?, response: Response<List<UserProfile>>?) {
            response?.body()?.let {
                // 通信成功
                println("get success")
                res1 = it.toMutableList()
                res2 = it.toMutableList()
            }
        }

        override fun onFailure(call: Call<List<UserProfile>>?, t: Throwable?) {
            t?.let {
                // 通信失敗
                println("get failure")
            }
        }
    })

    return res2
}

fun getName(id: String): String? {
    // 非同期処理
    var name: String? = null
    service.getUserById(id).enqueue(object : Callback<UserProfile> {
        public override fun onResponse(
                call: Call<UserProfile>?,
                response: Response<UserProfile>?) {
            response?.body()?.let {
                // 通信成功
                name = it.name
                println(it.toString())
            }
        }

        public override fun onFailure(call: Call<UserProfile>?, t: Throwable?) {
            t?.let {
                // 通信失敗
                // name = null
            }
        }
    })

    return name
}

fun createAccount(id: String, name: String): Boolean {
    // 非同期処理
    var success: Boolean = false
    service.addUser(id, name).enqueue(object : Callback<Unit> {
        public override fun onResponse (
                call: Call<Unit>?,
                response: Response<Unit>?) {
            response?.body()?.let {
                // 通信成功
                println("create success")
                success = true
            }
        }

        public override fun onFailure(call: Call<Unit>?, t: Throwable?) {
            t?.let {
                // 通信失敗
                println("create failure")
                println(it.toString())
                // success = false
            }
        }
    })

    return success
}

