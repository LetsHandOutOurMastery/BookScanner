package com.awecode.thupraiisbnscanner

import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.awecode.thupraiisbnscanner.db.BarcodeDataBase
import com.awecode.thupraiisbnscanner.db.entity.BarcodeData
import com.awecode.thupraiisbnscanner.model.BooksResponse
import com.awecode.thupraiisbnscanner.utils.CommonUtils
import com.awecode.thupraiisbnscanner.utils.DbWorkerThread
import com.awecode.thupraiisbnscanner.utils.showToast
import com.awecode.thupraiisbnscanner.view.MainActivity
import com.awecode.thupraiisbnscanner.view.base.BaseActivity
import com.awecode.thupraiisbnscanner.view.setting.SettingActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.retrofitcoroutines.example.remote.UserApi
import kotlinx.android.synthetic.main.activity_scan_books.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Type

class ScanBooksActivity : BaseActivity() {
    private var mDb: BarcodeDataBase? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    override val layoutId = R.layout.activity_scan_books
    private var mUri: Uri? = null
    private val OPERATION_CAPTURE_PHOTO = 1
    private val OPERATION_CHOOSE_PHOTO = 2
    private var isbnNumber: String? = null
    private var barcodePath: String? = null

    override fun initView() {
        super.initView()
        initializeWorkerThread()
        mDb = BarcodeDataBase.getInstance(this)
        val builder: StrictMode.VmPolicy.Builder =  StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        if (this.intent.extras != null && this.intent.extras!!.containsKey("isbn") && this.intent.extras!!.containsKey("barcodepath")) {
            isbnNumber = intent.getStringExtra("isbn").toString()
            barcodePath = intent.getStringExtra("barcodepath").toString()

            if(isbnNumber!!.isNotEmpty() && barcodePath!!.isNotEmpty()){
                loadData(isbnNumber!!, barcodePath!!)
            } else {
                progress.visibility = View.GONE
                message.visibility =View.VISIBLE
                message.text = "Click to add Image"
                message.setOnClickListener {
                    showChooseImageDialog()
                }
            }
        } else {
            progress.visibility = View.GONE
            message.visibility =View.VISIBLE
            message.text = "Click to add Image"
            message.setOnClickListener {
                showChooseImageDialog()
            }
        }

        cancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            this.finish()

        }

        rescanButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra("resumeScan","resumeScan")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            this.finish()

        }


        //save button click listener
        saveButton.setOnClickListener {
            var imageByteArray: ByteArray? = null
            if(bookImageView.drawable != null){
                val bitmap = (bookImageView.drawable as BitmapDrawable).getBitmap()
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream)
                imageByteArray = stream.toByteArray()
            }
            if(validateBookName(bookTitleEditText.text.toString())){
                if (validatePrice(euroPriceEditText.text.toString())) {
                    if (validatePrice(aedPriceEditText.text.toString())) {
                        if (validatePrice(sellingPriceEditText.text.toString())) {
                            val barcodeData = BarcodeData(
                                null,
                                isbnNumber,
                                bookTitleEditText.text.toString(),
                                bookPublishedEditText.text.toString(),
                                sellingPriceEditText.text.toString(),
                                euroPriceEditText.text.toString(),
                                aedPriceEditText.text.toString(),
                                receivedDateEditText.text.toString(),
                                imageByteArray,
                                barcodePath?.let { it1 -> CommonUtils.readFile(it1) }
                            )
                            insertBarcodeDataInDb(barcodeData)
                        } else
                            showToast("Price is empty. Please enter valid amount.", Toast.LENGTH_LONG)
                    } else
                        showToast("AED Buying Price is empty. Please enter valid amount.", Toast.LENGTH_LONG)
                } else
                    showToast("Euro Buying Price is empty. Please enter valid amount.", Toast.LENGTH_LONG)


            } else {
                showToast("Please enter book name.", Toast.LENGTH_LONG)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         if(requestCode == OPERATION_CAPTURE_PHOTO){
            if (resultCode == Activity.RESULT_OK) {
                val bitmap = BitmapFactory.decodeStream(
                    mUri?.let { contentResolver.openInputStream(it) })
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
                bookImageView!!.setImageBitmap(bitmap)
                if(bookImageView.drawable != null){
                    message.visibility = View.GONE
                }
            }
        } else if(requestCode == OPERATION_CHOOSE_PHOTO){
            if (resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= 19) {
                    handleImageOnKitkat(data)
                }
                if(bookImageView.drawable != null){
                    message.visibility = View.GONE
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    override fun onDestroy() {
        BarcodeDataBase.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.setting -> {
                openSettingActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSettingActivity() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    private fun initializeWorkerThread() {
        mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()
    }

    /**
     * Insert barcode data into DB
     */
    private fun insertBarcodeDataInDb(barcodeData: BarcodeData) {
        val task = Runnable { mDb?.barcodeDataDao()?.insert(barcodeData) }
        mDbWorkerThread.postTask(task)
        showToast("Book Data Saved")
        startActivity(Intent(this, MainActivity::class.java)
            .putExtra("resumeScan","resumeScan")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        this.finish()
    }

    fun loadData(isbnNumber: String, barcodePath: String){
        val retrofit1 = Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service1: UserApi = retrofit1.create(UserApi::class.java)
        val jsonCall: Call<JsonObject> = service1.readJson(isbnNumber)
        jsonCall.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if(response.code() == 200 && response.body() != null) {
                    Log.i("LOG_TAG", response.body().toString())
                    val listType: Type = object : TypeToken<BooksResponse>() {}.type
                    val booksResponse = Gson().fromJson(response.body().toString(), listType) as BooksResponse
                    Log.i("LOG_TAG_book", booksResponse.toString())
                    populateData(booksResponse, isbnNumber, barcodePath)
                } else {
                    showToast("Book Not found! Please add Manually")
                    progress.visibility = View.GONE
                    message.visibility =View.VISIBLE
                    message.text = "Click to add Image"
                    message.setOnClickListener {
                        showChooseImageDialog()
                    }
                }

            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.e("LOG_TAG", t.toString())
            }
        })
    }

    private fun populateData(booksResponse: BooksResponse,isbnNumber: String, barcodePath: String ) {
        bookTitleEditText.text = Editable.Factory.getInstance().newEditable(booksResponse.title)
        bookPublishedEditText.text = Editable.Factory.getInstance().newEditable(booksResponse.publishers.toString())
        receivedDateEditText.text = Editable.Factory.getInstance().newEditable(CommonUtils.getTodayStringDate())
        val imageURL = "http://covers.openlibrary.org/b/isbn/${isbnNumber}-M.jpg"
        loadImage(bookImageView, imageURL, progress, message)
        //show keyboard in price edittext
        CommonUtils.showKeyboard(this)
    }




    override fun onBackPressed() {
        cancelButton.performClick()
    }
    private fun validatePrice(price: String): Boolean {
        if (price.isNullOrEmpty())
            return false
        return true
    }

    private fun validateBookName(bookName: String): Boolean {
        if (bookName.isNullOrEmpty())
            return false
        return true
    }
    private fun loadImage(bookImageView: ImageView, uri: String?, progress: ProgressBar, message: TextView) {
        progress.visibility = View.VISIBLE
        Log.v("Image Url","URL: $uri")
        Glide.with(this)
            .load(uri)
            .error(R.drawable.error_image)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    Log.v("Glide","Exception: ${e.toString()}")
                    return false
                }
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    if(bookImageView.drawable == null){
                        message.text = "Error! \n Click to add Image"
                        message.setOnClickListener {
                            showChooseImageDialog()
                        }
                    }
                    return false
                }
            })
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(bookImageView)
    }

    private fun showChooseImageDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.chooe_image_local, null)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        val chooseCamera = dialogView.findViewById<Button>(R.id.choose_camera)
        val chooseGallery = dialogView.findViewById<Button>(R.id.choose_gallery)

        //save button click listener
        chooseCamera.setOnClickListener {
            alertDialog.dismiss()//dismiss alert dialog
            capturePhoto()
        }

        //cancel button click listener
        chooseGallery.setOnClickListener {
            alertDialog.dismiss()//dismiss alert dialog
            val checkSelfPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED){
                //Requests permissions to be granted to this application at runtime
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
            else{
                openGallery()
            }
        }

        alertDialog.show()

        //show keyboard in price edittext
        CommonUtils.showKeyboard(this)
    }

    private fun openGallery(){
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, OPERATION_CHOOSE_PHOTO)
    }
    private fun renderImage(imagePath: String?){
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            bookImageView?.setImageBitmap(bitmap)
        }
        else {
            showToast("ImagePath is null")
        }
    }
    private fun getImagePath(uri: Uri?, selection: String?): String {
        var path: String? = null
        val cursor = uri?.let { contentResolver.query(it, null, selection, null, null ) }
        if (cursor != null){
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path!!
    }
    @TargetApi(19)
    private fun handleImageOnKitkat(data: Intent?) {
        var imagePath: String? = null
        val uri = data!!.data
        //DocumentsContract defines the contract between a documents provider and the platform.
        if (DocumentsContract.isDocumentUri(this, uri)){
            val docId = DocumentsContract.getDocumentId(uri)
            if (uri != null) {
                if ("com.android.providers.media.documents" == uri.authority){
                    val id = docId.split(":")[1]
                    val selsetion = MediaStore.Images.Media._ID + "=" + id
                    imagePath = getImagePath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        selsetion)
                } else if ("com.android.providers.downloads.documents" == uri.authority){
                    val contentUri = ContentUris.withAppendedId(Uri.parse(
                        "content://downloads/public_downloads"), java.lang.Long.valueOf(docId))
                    imagePath = getImagePath(contentUri, null)
                }
            }
        }
        else if (uri != null) {
            if ("content".equals(uri.scheme, ignoreCase = true)){
                imagePath = getImagePath(uri, null)
            }
            else if (uri != null) {
                if ("file".equals(uri.scheme, ignoreCase = true)){
                    imagePath = uri.path
                }
            }
        }
        renderImage(imagePath)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>
                                            , grantedResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when(requestCode){
            1 ->
                if (grantedResults.isNotEmpty() && grantedResults.get(0) ==
                    PackageManager.PERMISSION_GRANTED){
                    openGallery()
                }else {
                    showToast("Unfortunately You are Denied Permission to Perform this Operataion.")
                }
        }
    }

    private fun capturePhoto(){
        val capturedImage = File(externalCacheDir, "My_Captured_Photo.jpg")
        if(capturedImage.exists()) {
            capturedImage.delete()
        }
        capturedImage.createNewFile()
        mUri = if(Build.VERSION.SDK_INT >= 24){
            FileProvider.getUriForFile(this, "com.awecode.thupraiisbnscanner.fileprovider",
                capturedImage)
        } else {
            Uri.fromFile(capturedImage)
        }

        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
        startActivityForResult(intent, OPERATION_CAPTURE_PHOTO)
    }


}