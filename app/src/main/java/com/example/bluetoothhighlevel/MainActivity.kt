package com.example.bluetoothhighlevel


import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothhighlevel.databinding.ActivityMainBinding

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        val mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothManager.adapter
    }
    private val bleScanner by lazy {
        mBluetoothAdapter.bluetoothLeScanner
    }
    private var isScanning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.starScan.setOnClickListener {
            if (isScanning){
                stopScanning()
            }else{
                starScan()
            }
        }
    }

    private fun starScan() {
        if (!hasRequiredRuntimePermissions()){
            requestRelevantRuntimePermissions()
        }else{
            //Empezar escaneo de dispositivos
            Toast.makeText(this, "Escaneode dispositivos", Toast.LENGTH_SHORT).show()

            val filter = ScanFilter.Builder()
                .setDeviceName("CUBE")
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()

            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }
    private val scanCallback = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            with(result.device){
                Log.i("ScanCallback", "Name: $name y Address: $address")
            }
        }
    }

    private fun stopScanning(){
        bleScanner.stopScan(scanCallback)
        isScanning = false
        Toast.makeText(this, "Detener escaneo", Toast.LENGTH_SHORT).show()
    }

    private fun requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()){return}

        when{
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->{
                requestLocationPermissions()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->{
                requestBluetoothPermissions()
            }
        }


    }

    private fun requestBluetoothPermissions() {
        runOnUiThread{
            val dialog = AlertDialog.Builder(this)
                .setTitle("Permisos de localizacion requeridos")
                .setMessage("A PArtir de android 6 los permisos de Localizacion para usar el bluetoot")
                .setNegativeButton("Cancelar"){ view, _ ->
                    view.dismiss()
                }
                .setPositiveButton("Aceptar"){view, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
                        RUNTIME_PERMISSION_REQUEST_CODE)

                    view.dismiss()
                }
                .setCancelable(false)
                .create()
            dialog.show()
        }


    }

    private fun requestLocationPermissions() {

        runOnUiThread{
            val dialog = AlertDialog.Builder(this)
                .setTitle("Permisos de localizacion requeridos")
                .setMessage("A PArtir de android 6 los permisos de Localizacion para usar el bluetoot")
                .setNegativeButton("Cancelar"){ view, _ ->
                    view.dismiss()
                }
                .setPositiveButton("Aceptar"){view, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        RUNTIME_PERMISSION_REQUEST_CODE)

                    view.dismiss()
                }
                .setCancelable(false)
                .create()
            dialog.show()
        }

    }


    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter.isEnabled){
            promptEnableBluetooth()
        }
    }


    private fun promptEnableBluetooth() {
        if (!mBluetoothAdapter.isEnabled){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            ENABLE_BLUETOOTH_REQUEST_CODE ->{
                if (resultCode != Activity.RESULT_OK){
                    promptEnableBluetooth()
                }
            }
        }
    }

    fun hasPermissions(permissionType: String): Boolean{
        return ContextCompat.checkSelfPermission(this,
            permissionType) == PackageManager.PERMISSION_GRANTED
    }
    fun hasRequiredRuntimePermissions():Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            hasPermissions(android.Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermissions(android.Manifest.permission.BLUETOOTH_CONNECT)

        }else{
            hasPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            RUNTIME_PERMISSION_REQUEST_CODE ->{
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any{
                    it.second == PackageManager.PERMISSION_DENIED && !ActivityCompat.shouldShowRequestPermissionRationale(this,
                    it.first)
                }
                val containsDenial = grantResults.any {
                    it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED }

                when{
                    containsPermanentDenial ->{
                        Toast.makeText(this, "Deve activar los servicios manualmente", Toast.LENGTH_SHORT).show()
                    }
                    containsDenial ->{
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() ->{
                        starScan()

                    }
                    else -> {
                        recreate()
                    }
                }
            }
        }
    }
}