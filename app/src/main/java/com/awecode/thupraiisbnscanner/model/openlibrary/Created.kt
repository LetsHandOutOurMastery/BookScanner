package com.awecode.thupraiisbnscanner.model

import com.google.gson.annotations.SerializedName


data class Created (

	@SerializedName("type") val type : String,
	@SerializedName("value") val value : String
)