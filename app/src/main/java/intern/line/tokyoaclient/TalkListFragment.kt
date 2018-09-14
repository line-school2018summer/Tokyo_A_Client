package intern.line.tokyoaclient


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import intern.line.tokyoaclient.HttpConnection.model.RoomWithImageUrlAndLatestTalkTimestamp
import intern.line.tokyoaclient.HttpConnection.roomService
import intern.line.tokyoaclient.HttpConnection.talkService
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*


private lateinit var roomList: ListView
private var adapter: RoomAdapterWithImage? = null
private lateinit var data: ArrayList<RoomWithImageUrlAndLatestTalkTimestamp>

private var sinceTalkId = 0L
private lateinit var userId: String


class TalkListFragment : Fragment() {
    private lateinit var v: View

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
                        // getRoomIcon(r.roomId)
                        getLatestTalk(r.roomId)
                    }
                }, {
                    Toast.makeText(context, "get room failed: $it", Toast.LENGTH_LONG).show()
                    println("get room failed: $it")
                })
    }

    private fun getLatestTalk(roomId: String) {
        talkService.getTalk(roomId, sinceTalkId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //Toast.makeText(this, "get talk succeeded", Toast.LENGTH_SHORT).show()
                    // println("get talk succeeded: $it")

                    if (!it.isEmpty()) {
                        adapter?.add(RoomWithImageUrlAndLatestTalkTimestamp(
                                roomId,
                                "hoge",
                                "default.jpg",
                                it.last().text,
                                it.last().createdAt
                        ))
                    }

                    println("sinceTalkId: $sinceTalkId")
                    adapter?.notifyDataSetChanged()
                    Collections.sort(data, RoomComparator())
                }, {
                    Toast.makeText(context, "get talk failed: $it", Toast.LENGTH_SHORT).show()
                    println("get talk failed: $it")
                })
    }

    private fun getRoomIcon(roomId: String) {
        roomService.getRoomMembersByRoomId(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    // Toast.makeText(context, "get room succeeded", Toast.LENGTH_SHORT).show()
                    // println("get room succeeded: $it")
                    if(it.size == 2){ // 個人チャット
                        var i = 0
                        while(it[i].uid.equals(userId))
                            i++
                        val friendId = it[i].uid
                        getUserIcon(friendId)
                    } else {
                        // TODO
                    }
                }, {
                    Toast.makeText(context, "get room failed: $it", Toast.LENGTH_LONG).show()
                    println("get room failed: $it")
                })
    }

    private fun getUserIcon(userId: String) {

    }
}
