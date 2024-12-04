import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opsc7311poe.xbcad_antoniemotors.R

class ManagerAdapter(
    private var managerNames: MutableList<String>,
    private val onItemClick: (String) -> Unit // Pass admin name instead of position
) : RecyclerView.Adapter<ManagerAdapter.ManagerViewHolder>() {

    private val fullList = ArrayList(managerNames) // Store original list for filtering

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_name, parent, false)
        return ManagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManagerViewHolder, position: Int) {
        holder.bind(managerNames[position], onItemClick)
    }

    override fun getItemCount(): Int = managerNames.size

    fun filterList(query: String) {
        managerNames = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter { it.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class ManagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAdminName: TextView = itemView.findViewById(R.id.txtAdminName)

        fun bind(adminName: String, onClick: (String) -> Unit) {
            txtAdminName.text = adminName
            itemView.setOnClickListener { onClick(adminName) } // Pass name directly
        }
    }
}
