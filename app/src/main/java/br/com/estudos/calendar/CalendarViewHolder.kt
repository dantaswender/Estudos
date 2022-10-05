package br.com.estudos.calendar

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import br.com.estudos.R

class CalendarViewHolder(val itemView: View, val onItemListener: OnItemListener) :
    RecyclerView.ViewHolder(itemView), View.OnClickListener {
    val dayOfMonth: TextView
//    private val onItemListener: OnItemListener
    override fun onClick(view: View) {
        onItemListener.onItemClick(adapterPosition, dayOfMonth.text as String)
    }

    init {
        dayOfMonth = itemView.findViewById(R.id.cellDayText)
//        this.onItemListener = onItemListener
        itemView.setOnClickListener(this)
    }
}