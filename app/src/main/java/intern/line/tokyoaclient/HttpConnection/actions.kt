package intern.line.tokyoaclient.HttpConnection

import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


var res_getAllUsers: List<UserProfile> = mutableListOf<UserProfile>()
var res_getUsersByName: List<UserProfile> = mutableListOf<UserProfile>()
var res_getUserById: UserProfile = UserProfile()
var res_getUsersByLikelyName: List<UserProfile> = mutableListOf<UserProfile>()
var res_deleteUser: UserProfile = UserProfile()

fun getAllUsers(): List<UserProfile> {
    var res: List<UserProfile> = mutableListOf<UserProfile>()
    service.getAllUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                res_getAllUsers = it
                res = it
                println("get success: $it")
            }, {
                println("get failure")
            })
    return res
}

fun getUserById(id: String): UserProfile {
    var res: UserProfile = UserProfile()
    service.getUserById(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                res_getUserById = it
                res = it
                println("get id success: $it")
            }, {
                println("get id failure")
            })
    return res
}

fun getUsersByName(name: String): List<UserProfile> {
    var res: List<UserProfile> = mutableListOf<UserProfile>()
    service.getUserByName(name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                res_getUsersByName = it
                res = it
                println("get name success: $it")
            }, {
                println("get name failure")
            })
    return res
}

fun getUsersByLikelyName(name: String): List<UserProfile> {
    var res: List<UserProfile> = mutableListOf<UserProfile>()
    service.getUserByLikelyName(name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                res_getUsersByLikelyName = it
                res = it
                println("get likelyname success: $it")
            }, {
                println("get likelyname failure")
            })
    return res
}

fun createAccount(id: String, name: String): Boolean {
    var success: Boolean = false
    service.addUser(id, name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                success = true
                println("create success: $it")
            }, {
                println("create failure")
            })
    return success
}

fun modifyAccount(id: String, name: String): Boolean {
    var success: Boolean = false
    service.modifyUser(id, name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                success = true
                println("modify success: $it")
            }, {
                println("modify failure")
            })
    return success
}

fun deleteAccount(id: String): UserProfile {
    var res: UserProfile = UserProfile()
    service.deleteUser(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                res_deleteUser = it
                res = it
                println("delete success: $it")
            }, {
                println("delete failure")
            })
    return res
}