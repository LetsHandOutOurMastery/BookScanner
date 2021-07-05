package com.awecode.thupraiisbnscanner.view.history

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.awecode.thupraiisbnscanner.R
import com.awecode.thupraiisbnscanner.db.entity.BarcodeData
import kotlinx.android.synthetic.main.barcode_item.view.*

class HistoryAdapter(val dataList: List<BarcodeData>, val itemClick: (BarcodeData) -> Unit) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.barcode_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindHistory(dataList[position])
    }

    override fun getItemCount() = dataList.size


    class ViewHolder(view: View, val itemClick: (BarcodeData) -> Unit) : RecyclerView.ViewHolder(view) {

        fun bindHistory(data: BarcodeData) {
            with(data) {

                itemView.isbnTextView.text = "ISBN: $isbn"
                itemView.dateTextView.text = date
                itemView.bookNameTextView.text = bookTitle
                itemView.bookPublisherTextView.text = publisher

                if (euroBoughtPrice.isNullOrEmpty())
                    itemView.europriceTextView.visibility = View.INVISIBLE
                else {
                    itemView.europriceTextView.visibility = View.VISIBLE
                    itemView.europriceTextView.text = "Bought Price:  \n ${euroBoughtPrice} (€)"
                }

                if (aedBoughtPrice.isNullOrEmpty())
                    itemView.aedpriceTextView.visibility = View.INVISIBLE
                else {
                    itemView.aedpriceTextView.visibility = View.VISIBLE
                    itemView.aedpriceTextView.text = "Bought Price:  \n ${aedBoughtPrice} (AED)"
                }

                if (sellingPrice.isNullOrEmpty())
                    itemView.sellingpriceTextView.visibility = View.INVISIBLE
                else {
                    itemView.sellingpriceTextView.visibility = View.VISIBLE
                    itemView.sellingpriceTextView.text = "Bought Price:  \n ${sellingPrice} (AED)"
                }

                if (bookImage != null) {
                    //show saved barcode image
                    val bitmap = BitmapFactory.decodeByteArray(bookImage, 0, bookImage?.size!!)
                    itemView.bookCoverImageView.setImageBitmap(bitmap)
                } else
                    itemView.bookCoverImageView.setImageBitmap(null)

                if (barcodeImage != null) {
                    //show saved barcode image
                    val bitmap = BitmapFactory.decodeByteArray(barcodeImage, 0, barcodeImage?.size!!)
                    itemView.barcodeImageView.setImageBitmap(bitmap)
                } else
                    itemView.barcodeImageView.setImageBitmap(null)

            }
        }
    }
}