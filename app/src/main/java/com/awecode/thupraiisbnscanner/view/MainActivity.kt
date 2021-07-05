package com.awecode.thupraiisbnscanner.view

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Editable
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.awecode.thupraiisbnscanner.R
import com.awecode.thupraiisbnscanner.ScanBooksActivity
import com.awecode.thupraiisbnscanner.db.BarcodeDataBase
import com.awecode.thupraiisbnscanner.db.entity.BarcodeData
import com.awecode.thupraiisbnscanner.model.Setting
import com.awecode.thupraiisbnscanner.model.listener.SqliteToXlsExportListener
import com.awecode.thupraiisbnscanner.utils.*
import com.awecode.thupraiisbnscanner.view.base.BaseActivity
import com.awecode.thupraiisbnscanner.view.history.BarcodeHistoryActivity
import com.awecode.thupraiisbnscanner.view.setting.SettingActivity
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.price_input_layout.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit


class MainActivity : BaseActivity(), SqliteToXlsExportListener {

    private val TAG = MainActivity::class.java.simpleName

    override val layoutId = R.layout.activity_main

    private var mDb: BarcodeDataBase? = null

    private lateinit var mDbWorkerThread: DbWorkerThread

    private var mXlsFilePath: String? = null
    private var mUri: Uri? = null


    override fun initView() {
        super.initView()
        initializeWorkerThread()
        mDb = BarcodeDataBase.getInstance(this)
        val builder: VmPolicy.Builder =  VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        if (this.intent.extras != null && this.intent.extras!!.containsKey("resumeScan")) {
            resumeBarcodeScanner()
        }
    }


    /**
     * Get the results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    showToast("Cancelled")
                } else {
                    if (Setting.priceInputStatus){
                        //redirect to 2nd screen
                        startActivity(Intent(this, ScanBooksActivity::class.java)
                            .putExtra("isbn",result.contents)
                            .putExtra("barcodepath", result.barcodeImagePath)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    this.finish()
                }
            }
         else {
            super.onActivityResult(requestCode, resultCode, data)
        }
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

    override fun onDestroy() {
        BarcodeDataBase.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
    }

    override fun onExportStart() {
        logv("xls file export started")
    }

    override fun onExportComplete(filePath: String, folderPath: String, fileName: String) {
        showToast("File export success. Check Download folder.")
        mXlsFilePath = "$folderPath/$fileName"
        showToast("File Exported to:$folderPath/$fileName")
        shareButton.visibility = View.VISIBLE
    }

    override fun onExportError() {
        logv("xls file export error")
    }
    private fun openSettingActivity() {
        startActivity(Intent(this, SettingActivity::class.java))
    }

    private fun initializeWorkerThread() {
        mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()
    }

    /**
     * Resume barcode after 2 seconds
     */
    private fun resumeBarcodeScanner() {
        Observable.timer(Setting.delayTime, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    startZxingScanner()
                }
    }

    fun startBtnClick(view: View?) {
        askRunTimePermissions()
    }

    fun exportBtnClick(view: View?) {
        val converter = SqliteXlsConverter()
        converter.setOnExportListener(this)
        converter.exportSqliteToExcel(this)
    }

    fun addManualBtnClick(view: View?) {
        startActivity(Intent(this, ScanBooksActivity::class.java))
    }

    fun shareFileBtnClick(view: View?) {
        if(!mXlsFilePath.isNullOrEmpty()) {
            CommonUtils.shareFile(this, File(mXlsFilePath))
        } else {
            showToast("Error! Export File and then Share")
        }
    }

    fun historyBtnClick(view: View?) {
        openHistory()
    }

    fun clearBtnClicked(view: View?) {
        showDeleteConfirmationDialog()
    }

    private fun showDeleteConfirmationDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm!")
        builder.setMessage("Are you sure you want to delete all data?")
        builder.setPositiveButton("Yes, Delete") { dialog, which ->
            deleteAllBarcodeData()
        }
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
        val dialog = builder.create()
        dialog.show()
    }

    private fun deleteAllBarcodeData() {
        val task = Runnable { mDb?.barcodeDataDao()?.deleteAll() }
        mDbWorkerThread.postTask(task)
    }

    private fun openHistory() {
        startActivity(Intent(this, BarcodeHistoryActivity::class.java))
    }

    private fun askRunTimePermissions() {
        rxPermissions
                ?.request(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ?.subscribe { granted ->
                    if (granted == true) {
                        // All requested permissions are granted
                        startZxingScanner()
                    } else {
                        // At least one permission is denied
                        Toast.makeText(this, "Please provide all permissions from application manager.", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun startZxingScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan a ISBN")
        integrator.setCameraId(0)  // Use a specific camera of the device
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()

    }
}
