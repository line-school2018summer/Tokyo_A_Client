package intern.line.tokyoaclient.Fragment


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import intern.line.tokyoaclient.Adapter.RoomAdapterWithImage
import intern.line.tokyoaclient.Adapter.RoomComparator
import intern.line.tokyoaclient.HttpConnection.imageService
import intern.line.tokyoaclient.HttpConnection.model.RoomWithImageUrlAndLatestTalk
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.HttpConnection.talkService
import intern.line.tokyoaclient.HttpConnection.userProfileService
import intern.line.tokyoaclient.R
import intern.line.tokyoaclient.TalkActivity
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*


private lateinit var roomList: ListView
private var adapter: RoomAdapterWithImage? = null
private lateinit var data: ArrayList<RoomWithImageUrlAndLatestTalk>

private var sinceTalkId = 0L
private lateinit var userId: String


class TalkListFragment : Fragment() {
    private lateinit var v: View
    private var alive = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_talk_list, container, false)

        //MyPagerAdapterで設定しておいたargumentsを取得
        userId = arguments!!.getString("userId")
        roomList = v.findViewById(R.id.roomListView) as ListView

        data = ArrayList()
        adapter = RoomAdapterWithImage(context!!, data)
        roomList.setAdapter(adapter)

        getRoom(userId)

        roomList.setOnItemClickListener { _, _, position, _ ->
            val roomId: String = data[position].roomId
            goToTalk(roomId, data[position].roomName)
        }

        return v
    }

    private fun getRoom(userId: String) {
        roomService.getRoomsByUserId(userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get room succeeded", Toast.LENGTH_SHORT).show()
                    // println("get room succeeded: $it")
                    for(r in it) {
                        getLatestTalkWithLongPolling(r.roomId, -1)
                    }
                }, {
                    Toast.makeText(context, "get room failed: $it", Toast.LENGTH_LONG).show()
                    println("get room failed: $it")
                })
    }

    private fun getLatestTalkWithLongPolling(roomId: String, sinceTalkId: Long) {
        talkService.getTalk(roomId, sinceTalkId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    // println("get talk succeeded: $it")

                    if (!it.isEmpty()) {
                        var target = searchDataWithRoomId(roomId)
                        if(target != null) {
                            target.latestTalk = it.last().text
                            target.latestTalkTime = it.last().createdAt
                            adapter?.notifyDataSetChanged()
                            Collections.sort(data, RoomComparator())
                        } else {
                            getRoomNameAndIcon(RoomWithImageUrlAndLatestTalk(
                                    roomId,
                                    "hoge",
                                    "default.jpg",
                                    it.last().text,
                                    it.last().createdAt
                            ))
                        }
                        if(alive)
                            getLatestTalkWithLongPolling(roomId, it.last().talkId)
                    } else {
                        if(alive)
                            getLatestTalkWithLongPolling(roomId, sinceTalkId)
                    }
                }, {
                    Toast.makeText(context, "get talk failed: $it", Toast.LENGTH_SHORT).show()
                    println("get talk failed: $it")
                    if(alive)
                        getLatestTalkWithLongPolling(roomId, sinceTalkId)
                })
    }

    private fun searchDataWithRoomId(roomId: String): RoomWithImageUrlAndLatestTalk? {
        val target = data.find { it.roomId == roomId }
        return target
    }

    private fun getRoomNameAndIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk) {
        roomService.getRoomMembersByRoomId(roomWithImageUrlAndLatestTalk.roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get room succeeded", Toast.LENGTH_SHORT).show()
                    // println("get room succeeded: $it")
                    if(it.size == 2){ // 個人チャット
                        val friendId = it.find { !it.uid.equals(userId) }?.uid
                        if(friendId == null) { // 自分が二人の個人チャット（本来ありえない）
                            // getFriendNameAndIcon(roomWithImageUrlAndLatestTalk, userId)
                        } else {
                            getFriendNameAndIcon(roomWithImageUrlAndLatestTalk, friendId)
                        }
                    } else { // 数で判別すると，2人のグループのときにバグりそう．roomにisGroupなどのフラグをたてるのかなあ．
                        // TODO
                    }
                }, {
                    Toast.makeText(context, "get room failed: $it", Toast.LENGTH_LONG).show()
                    println("get room failed: $it")
                })
    }

    private fun getFriendNameAndIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, friendId: String) {
        userProfileService.getUserById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    println("get name succeeded: $it")
                    roomWithImageUrlAndLatestTalk.roomName = it.name
                    getFriendIcon(roomWithImageUrlAndLatestTalk, friendId)
                }, {
                    // Toast.makeText(this, "get name failed: $it", Toast.LENGTH_SHORT).show()
                    println("get name failed: $it")
                })
    }

    private fun getFriendIcon(roomWithImageUrlAndLatestTalk: RoomWithImageUrlAndLatestTalk, friendId: String) {
        imageService.getImageUrlById(friendId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(this, "get image url succeeded", Toast.LENGTH_SHORT).show()
                    println("get image url succeeded: $it")
                    roomWithImageUrlAndLatestTalk.pathToFile = it.pathToFile
                    adapter?.add(roomWithImageUrlAndLatestTalk)
                    Collections.sort(data, RoomComparator())
                }, {
                    // Toast.makeText(this, "get image url failed: $it", Toast.LENGTH_SHORT).show()
                    println("get image url failed: $it")
                })
    }

    private fun goToTalk(roomId: String, name: String) {
        val intent = Intent(context, TalkActivity::class.java)
        intent.putExtra("roomName", name)
        intent.putExtra("userId", userId)
        intent.putExtra("roomId", roomId.toString())
        startActivity(intent)
    }
}
