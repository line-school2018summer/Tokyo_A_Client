package intern.line.tokyoaclient.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrl
import intern.line.tokyoaclient.HttpConnection.model.UserProfileWithImageUrlSelection
import intern.line.tokyoaclient.R


class SettingListAdapter(context: Context, settings: List<String>) : ArrayAdapter<String>(context, 0, settings) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: SettingViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.setting_list_item, parent, false)
            holder = SettingViewHolder(
                    (view.findViewById(R.id.settingTextView) as TextView)
            )
            view.tag = holder
        } else {
            holder = view.tag as SettingViewHolder
        }

        val str = getItem(position) as String
        holder.settingTextView.text = str
        return view!!
    }
}

data class SettingViewHolder(val settingTextView: TextView)