package xyz.kurozero.nekosmoe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.kurozero.nekosmoe.R
import xyz.kurozero.nekosmoe.model.Neko
import xyz.kurozero.nekosmoe.model.Nekos

class NekosGridRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listOfNekos = listOf<Neko>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NekosViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_list_item, parent, false))
    }

    override fun getItemCount(): Int = listOfNekos.size

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val nekosViewHolder = viewHolder as NekosViewHolder
        nekosViewHolder.bindView(listOfNekos[position])
    }

    fun setNekosList(nekos: List<Neko>) {
        this.listOfNekos = nekos
        notifyDataSetChanged()
    }
}