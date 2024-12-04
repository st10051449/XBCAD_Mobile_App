data class QuoteData(
    val id: String? = null,
    val companyName: String? = null,
    val customerName: String? = null,
    val serviceType: String? = null,
    val parts: List<Map<String, Any>> = listOf(),
    val labourCost: String? = null,
    val totalCost: String? = null,
    val dateCreated: String? = null,
    val streetName: String? = null,
    val city: String? = null,
    val postCode: Int? = null,
    val suburb: String? = null
) {
    constructor() : this(null, null,null, null, listOf(), null, null, null, null, null,null, null)
}