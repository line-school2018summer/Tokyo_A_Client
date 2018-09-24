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

class UserListAdapterWithImage(context: Context, usersWithImageUrl: List<UserProfileWithImageUrl>) : ArrayAdapter<UserProfileWithImageUrl>(context, 0, usersWithImageUrl) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: ViewHolderWithImage

        if (view == null) {
            view = layoutInflater.inflate(R.layout.friend_list_item, parent, false)
            holder = ViewHolderWithImage(
                    (view.findViewById(R.id.nameTextView) as TextView),
                    (view.findViewById(R.id.idTextView) as TextView),
                    (view.findViewById(R.id.icon) as ImageView),
                    "default.jpg"
            )
            view.tag = holder
        } else {
            holder = view.tag as ViewHolderWithImage
        }

        val user = getItem(position) as UserProfileWithImageUrl
        holder.nameTextView.text = user.name
        holder.idTextView.text = user.id
        holder.idTextView.visibility = View.INVISIBLE
        holder.pathToFile = user.pathToFile
        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + user.pathToFile).into(holder.iconImageView)
        return view!!
    }
}

data class ViewHolderWithImage(val nameTextView: TextView, val idTextView: TextView, val iconImageView: ImageView, var pathToFile: String)

class NameComparator(): Comparator<UserProfileWithImageUrl> {
    override fun compare(lt: UserProfileWithImageUrl, rt: UserProfileWithImageUrl): Int {
        return lt.name.compareTo(rt.name)
    }
}

class UserListAdapterWithImageSelection(context: Context, usersWithImageUrl: List<UserProfileWithImageUrlSelection>) : ArrayAdapter<UserProfileWithImageUrlSelection>(context, 0, usersWithImageUrl) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: ViewHolderWithImageSelection

        if (view == null) {
            view = layoutInflater.inflate(R.layout.multiple_choice_friend_list_item, parent, false)
            holder = ViewHolderWithImageSelection(
                    (view.findViewById(R.id.nameTextView) as CheckedTextView),
                    (view.findViewById(R.id.icon) as ImageView),
                    "default.jpg"
            )
            view.tag = holder
        } else {
            holder = view.tag as ViewHolderWithImageSelection
        }

        val user = getItem(position) as UserProfileWithImageUrlSelection
        if(user.isChecked)
            holder.nameTextView.toggle()
        holder.nameTextView.text = user.name
        holder.pathToFile = user.pathToFile
        Glide.with(context).load("http://ec2-52-197-250-179.ap-northeast-1.compute.amazonaws.com/image/url/" + user.pathToFile).into(holder.iconImageView)
        return view!!
    }
}

data class ViewHolderWithImageSelection(val nameTextView: CheckedTextView, val iconImageView: ImageView, var pathToFile: String)

class NameComparatorSelection(): Comparator<UserProfileWithImageUrlSelection> {
    override fun compare(lt: UserProfileWithImageUrlSelection, rt: UserProfileWithImageUrlSelection): Int {
        return lt.name.compareTo(rt.name)
    }
}