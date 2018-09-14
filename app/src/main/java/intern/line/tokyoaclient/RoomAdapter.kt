package intern.line.tokyoaclient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.HttpConnection.model.RoomWithImageUrlAndLatestTalkTimestamp
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl


class RoomAdapterWithImage(context: Context, rooms: List<RoomWithImageUrlAndLatestTalkTimestamp>) : ArrayAdapter<RoomWithImageUrlAndLatestTalkTimestamp>(context, 0, rooms) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: RoomViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.room_list_item, parent, false)
            holder = RoomViewHolder(
                    (view.findViewById(R.id.nameTextView) as TextView),
                    (view.findViewById(R.id.textTextView) as TextView),
                    (view.findViewById(R.id.timeTextView) as TextView),
                    (view.findViewById(R.id.icon) as ImageView),
                    "default.jpg"
            )
            view.tag = holder
        } else {
            holder = view.tag as RoomViewHolder
        }

        val room = getItem(position) as RoomWithImageUrlAndLatestTalkTimestamp
        holder.nameTextView.text = room.roomName
        holder.latestTalkTextView.text = room.latestTalk
        holder.latestTalkTime.text = room.latestTalkTime.toString().substring(11, 16)
        holder.pathToFile = room.pathToFile
        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + room.pathToFile).into(holder.iconImageView)
        return view!!
    }
}

data class RoomViewHolder(
        val nameTextView: TextView,
        val latestTalkTextView: TextView,
        val latestTalkTime: TextView,
        val iconImageView: ImageView,
        var pathToFile: String
)

class RoomComparator(): Comparator<RoomWithImageUrlAndLatestTalkTimestamp> {
    override fun compare(lt: RoomWithImageUrlAndLatestTalkTimestamp, rt: RoomWithImageUrlAndLatestTalkTimestamp): Int {
        return lt.latestTalkTime.compareTo(rt.latestTalkTime)
    }
}