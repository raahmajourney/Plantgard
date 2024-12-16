package com.example.plantgard.response

import com.google.gson.annotations.SerializedName

data class RegisterResponse(

	@field:SerializedName("password")
	val password: String?= null,

	@field:SerializedName("name")
	val name: String?= null,

	@field:SerializedName("email")
	val email: String?= null
)
