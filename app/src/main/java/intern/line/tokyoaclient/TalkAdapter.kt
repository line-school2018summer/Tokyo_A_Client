package intern.line.tokyoaclient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.HttpConnection.model.TalkWithImageUrl


class TalkAdapter(context: Context, users: List<TalkWithImageUrl>) : ArrayAdapter<TalkWithImageUrl>(context, 0, users) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: TalkViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.talk_list_item, parent, false)
            holder = TalkViewHolder(
                    (view.findViewById(R.id.nameTextView) as TextView),
                    (view.findViewById(R.id.textTextView) as TextView),
                    (view.findViewById(R.id.timeTextView) as TextView),
                    (view.findViewById(R.id.icon) as ImageView)
            )
            view.tag = holder
        } else {
            holder = view.tag as TalkViewHolder
        }

        val user = getItem(position) as TalkWithImageUrl
        holder.nameTextView.text = user.senderName
        holder.textTextView.text = user.text
        holder.timeTextView.text = user.createdAt.toString().substring(11, 16) // yyyy-mm-dd hh:mm:ss
        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + user.pathToFile).into(holder.iconImageView)
        return view!!
    }
}

data class TalkViewHolder(val nameTextView: TextView, val textTextView: TextView, val timeTextView: TextView, val iconImageView: ImageView)

class TimeComparator(): Comparator<TalkWithImageUrl> {
    override fun compare(lt: TalkWithImageUrl, rt: TalkWithImageUrl): Int {
        return lt.createdAt.compareTo(rt.createdAt)
    }
}
