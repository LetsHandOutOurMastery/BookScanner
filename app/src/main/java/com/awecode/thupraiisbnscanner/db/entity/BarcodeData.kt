package com.awecode.thupraiisbnscanner.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcodeData")
data class BarcodeData(@PrimaryKey(autoGenerate = true) var id: Long?,
                       @ColumnInfo(name = "isbn") var isbn: String?,
                       @ColumnInfo(name = "booktitle") var bookTitle: String,
                       @ColumnInfo(name = "publisher") var publisher: String,
                       @ColumnInfo(name = "sellingprice") var sellingPrice: String?,
                       @ColumnInfo(name = "euroboughtprice") var euroBoughtPrice: String?,
                       @ColumnInfo(name = "aedboughtprice") var aedBoughtPrice: String?,
                       @ColumnInfo(name = "date") var date: String,
                       @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "bookimage") var bookImage: ByteArray?,
                       @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "barcodeimage") var barcodeImage: ByteArray?)