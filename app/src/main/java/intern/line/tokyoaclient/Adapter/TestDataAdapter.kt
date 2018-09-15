package intern.line.tokyoaclient.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import intern.line.tokyoaclient.HttpConnection.model.Talk
import intern.line.tokyoaclient.R

class TestDataAdapter(context: Context, data: List<Talk>) : ArrayAdapter<Talk>(context, 0, data) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: TestDataViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.test_data_item, parent, false)
            holder = TestDataViewHolder(
                    (view.findViewById(R.id.talkNameTextView) as TextView),
                    (view.findViewById(R.id.talkContentTextView) as TextView)
            )
            view.tag = holder
        } else {
            holder = view.tag as TestDataViewHolder
        }

        val data = getItem(position) as Talk
        holder.testDataNameTextView.text = data.senderId
        holder.testDataYearsTextView.text = data.text
        return view!!
    }
}

data class TestDataViewHolder(val testDataNameTextView: TextView, val testDataYearsTextView: TextView)