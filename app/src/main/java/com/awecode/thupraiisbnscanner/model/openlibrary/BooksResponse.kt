package com.awecode.thupraiisbnscanner.model

import Languages
import Last_modified
import Notes
import Type
import Works
import com.google.gson.annotations.SerializedName



data class BooksResponse (

	@SerializedName("type") val type : Type,
	@SerializedName("publish_date") val publish_date : String,
	@SerializedName("publish_country") val publish_country : String,
	@SerializedName("languages") val languages : List<Languages>,
	@SerializedName("oclc_numbers") val oclc_numbers : List<Int>,
	@SerializedName("dewey_decimal_class") val dewey_decimal_class : List<String>,
	@SerializedName("series") val series : List<String>,
	@SerializedName("notes") val notes : Notes,
	@SerializedName("subjects") val subjects : List<String>,
	@SerializedName("title") val title : String,
	@SerializedName("publishers") val publishers : List<String>,
	@SerializedName("publish_places") val publish_places : List<String>,
	@SerializedName("isbn_13") val isbn_13 : List<String>,
	@SerializedName("isbn_10") val isbn_10 : List<String>,
	@SerializedName("pagination") val pagination : String,
	@SerializedName("number_of_pages") val number_of_pages : Int,
	@SerializedName("ocaid") val ocaid : String,
	@SerializedName("source_records") val source_records : List<String>,
	@SerializedName("covers") val covers : List<Int>,
	@SerializedName("works") val works : List<Works>,
	@SerializedName("key") val key : String,
	@SerializedName("latest_revision") val latest_revision : Int,
	@SerializedName("revision") val revision : Int,
	@SerializedName("created") val created : Created,
	@SerializedName("last_modified") val last_modified : Last_modified
)

/*
"{"publishers":["Ladybird Books Ltd"],"number_of_pages":48,"weight":"3.5 ounces","covers":[2555922],
"physical_format":"Hardcover","last_modified":{"type":"/type/datetime","value":"2010-08-17T02:42:39.462808"},"latest_revision":3,
"key":"/books/OL10535787M","authors":[{"key":"/authors/OL2796073A"},{"key":"/authors/OL2796167A"}],"contributions":["Stuart Trotter (Illustrator)"],
"subjects":["English language readers","English"],"title":"The Dream (Read with Me: Key Words Reading Scheme)","identifiers":{"librarything":["3994028"]},
"created":{"type":"/type/datetime","value":"2008-04-30T09:38:13.731961"},"isbn_13":["9780721416212"],"isbn_10":["0721416217"],
"publish_date":"March 25, 1999","type":{"key":"/type/edition"},"physical_dimensions":"6.5 x 4.6 x 0.3 inches","revision":3}"*/
