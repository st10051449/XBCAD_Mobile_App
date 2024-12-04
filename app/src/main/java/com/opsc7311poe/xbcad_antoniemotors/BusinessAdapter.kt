import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opsc7311poe.xbcad_antoniemotors.R

class BusinessAdapter(
    private var businessNames: MutableList<String>,
    private val onItemClick: (String) -> Unit // Pass business name instead of position
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    private val fullList = ArrayList(businessNames) // Store original list for filtering

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_business_name, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        holder.bind(businessNames[position], onItemClick)
    }

    override fun getItemCount(): Int = businessNames.size

    fun filterList(query: String) {
        businessNames = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter { it.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class BusinessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtBusinessName: TextView = itemView.findViewById(R.id.txtBusinessName)

        fun bind(businessName: String, onClick: (String) -> Unit) {
            txtBusinessName.text = businessName
            itemView.setOnClickListener { onClick(businessName) } // Pass name directly
        }
    }
}

