package intern.line.tokyoaclient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import intern.line.tokyoaclient.HttpConnection.model.UserProfile


class UserListAdapter(context: Context, users: List<UserProfile>) : ArrayAdapter<UserProfile>(context, 0, users) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: ViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.friend_list_item, parent, false)
            holder = ViewHolder(
                    (view.findViewById(R.id.nameTextView) as TextView),
                    (view.findViewById(R.id.idTextView) as TextView)
            )
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val user = getItem(position) as UserProfile
        holder.nameTextView.text = user.name
        holder.idTextView.text = user.id
        return view!!
    }
}

data class ViewHolder(val nameTextView: TextView, val idTextView: TextView)