data class Leave(
    val leaveDays: Int = 0,
    val dateAssigned: String = "",
    val expiryDate: String = ""
) {
    // No-argument constructor for Firebase
    constructor() : this(
        leaveDays = 0,
        dateAssigned = "",
        expiryDate = ""
    )
}
