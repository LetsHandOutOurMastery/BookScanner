import com.google.gson.annotations.SerializedName


data class Notes (

	@SerializedName("type") val type : String,
	@SerializedName("value") val value : String
)