package com.example.sharegri

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sharegri.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

private const val TAG = "ESP32_ASA"
/* デバイス名 環境に合わせて変更*/
private const val DEVICE_NAME = "ESP32_ASA"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val REQUEST_ENABLE_BT = 1

    var count = 0;

    private val moterState: MutableMap<String,Boolean> = mutableMapOf("moter1" to false,"moter2" to false,"moter3" to false,"moter4" to false)

    /* Bluetoothデバイス */
    private var mDevice: BluetoothDevice? = null

    /* Bluetooth UUID(固定) */
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    /* Soket */
    private var mSocket: BluetoothSocket? = null

    /* Thread */
    private var mThread: Thread? = null

    /* Threadの状態を表す */
    private var isRunning = false

    /** Action(ステータス表示).  */
    private val VIEW_STATUS = 0

    /** Action(取得文字列).  */
    private val VIEW_INPUT = 1

    /** Connect確認用フラグ  */
    private var connectFlg = false

    /** BluetoothのOutputStream.  */
    var mmOutputStream: OutputStream? = null

    private var mInputTextView: TextView? = null

    private var mStatusTextView: TextView? = null

    /** 接続ボタン.  */
    private var connectButton: Button? = null

    /** 書込みボタン.  */
    private var writeButton: Button? = null

    private var connectedThread: ConnectedThread? = null
    private var connectThread: ConnectThread? = null

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mInputTextView = binding.inputValue
        mStatusTextView = binding.statusValue

        writeButton = binding.writeButton;

//        binding.connectButton.setOnClickListener{
//            onClick(view)
//            run()
//        };
        binding.writeButton.setOnClickListener{
            if(moterState["moter1"] == true){
                binding.writeButton.text = "START"
                binding.writeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#73C503")))
                onClick(view,"2")
                moterState["moter1"] = false
            }else{
                binding.writeButton.text = "STOP"
                binding.writeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EB5757")))
                onClick(view,"1")
                moterState["moter1"] = true
            }
        };

        binding.writeButton2.setOnClickListener{
            if(moterState["moter2"] == true){
                binding.writeButton2.text = "START"
                binding.writeButton2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#73C503")))
                onClick(view,"2")
                moterState["moter2"] = false
            }else{
                binding.writeButton2.text = "STOP"
                binding.writeButton2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EB5757")))
                onClick(view,"1")
                moterState["moter2"] = true
            }
        }

        binding.writeButton3.setOnClickListener{
            if(moterState["moter3"] == true){
                binding.writeButton3.text = "START"
                binding.writeButton3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#73C503")))
                onClick(view,"2")
                moterState["moter3"] = false
            }else{
                binding.writeButton3.text = "STOP"
                binding.writeButton3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EB5757")))
                onClick(view,"1")
                moterState["moter3"] = true
            }
        }

        binding.writeButton4.setOnClickListener{
            if(moterState["moter4"] == true){
                binding.writeButton4.text = "START"
                binding.writeButton4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#73C503")))
                onClick(view,"2")
                moterState["moter4"] = false
            }else{
                binding.writeButton4.text = "STOP"
                binding.writeButton4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EB5757")))
                onClick(view,"4")
                moterState["moter4"] = true
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            if (device.name == DEVICE_NAME) {
                Log.d(TAG, "name = %s, MAC <%s>".format(deviceName, deviceHardwareAddress))
                device.uuids.forEach { uuid ->
                    Log.d(TAG, "uuid is %s".format(uuid.uuid))
                }
                connectThread = ConnectThread(device)
                connectThread?.start()
                return
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "サポートしていません", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter?.isEnabled!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    fun onClickMoterButton(v:View,index: String) {
        if(moterState["moter$index"] == true){
            binding.writeButton3.text = "START"
            binding.writeButton3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#73C503")))
            onClick(v,"2")
            moterState["moter3"] = false
        }else{
            binding.writeButton3.text = "STOP"
            binding.writeButton3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EB5757")))
            onClick(v,"1")
            moterState["moter3"] = true
        }
    }

//    fun connect(){
//        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//        pairedDevices?.forEach { device ->
//            val deviceName = device.name
//            val deviceHardwareAddress = device.address // MAC address
//            if (device.name == DEVICE_NAME) {
//                Log.d(TAG, "name = %s, MAC <%s>".format(deviceName, deviceHardwareAddress))
//                device.uuids.forEach { uuid ->
//                    Log.d(TAG, "uuid is %s".format(uuid.uuid))
//                }
//                connectThread = ConnectThread(device)
//                connectThread?.start()
//                return
//            }
//        }
//    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        connectFlg = false
        try {
            mSocket!!.close()
        } catch (e: Exception) {
        }
    }

//    @Override
//    fun run() {
//        var mmInStream: InputStream? = null
//        var valueMsg = Message()
//        valueMsg.what = VIEW_STATUS
//        valueMsg.obj = "connecting..."
//        mHandler.sendMessage(valueMsg)
//        try {
//
//            // 取得したデバイス名を使ってBluetoothでSocket接続
//            mSocket = mDevice!!.createRfcommSocketToServiceRecord(MY_UUID)
//            mSocket?.connect()
//            mmInStream = mSocket?.getInputStream()
//            mmOutputStream = mSocket?.getOutputStream()
//
//            // InputStreamのバッファを格納
//            val buffer = ByteArray(1024)
//
//            // 取得したバッファのサイズを格納
//            var bytes: Int
//            valueMsg = Message()
//            valueMsg.what = VIEW_STATUS
//            valueMsg.obj = "connected."
//            mHandler.sendMessage(valueMsg)
//            connectFlg = true
//            while (isRunning) {
//
//                // InputStreamの読み込み
//                if (mmInStream != null) {
//                    bytes = mmInStream.read(buffer)
//                    Log.i(TAG, "bytes=$bytes")
//                    // String型に変換
//                    val readMsg = String(buffer, 0, bytes)
//
//                    // null以外なら表示
//                    if (readMsg.trim { it <= ' ' } != null && readMsg.trim { it <= ' ' } != "") {
//                        Log.i(TAG, "value=" + readMsg.trim { it <= ' ' })
//                        valueMsg = Message()
//                        valueMsg.what = VIEW_INPUT
//                        valueMsg.obj = readMsg
//                        mHandler.sendMessage(valueMsg)
//                }
//                }
//            }
//        } // エラー処理
//        catch (e: Exception) {
//            valueMsg = Message()
//            valueMsg.what = VIEW_STATUS
//            valueMsg.obj = "Error1:$e"
//            mHandler.sendMessage(valueMsg)
//            try {
//                mSocket!!.close()
//            } catch (ee: Exception) {
//            }
//            isRunning = false
//            connectFlg = false
//        }
//    }

    private fun onClick(v: View,index:String) {
                try {
                    // Writeボタン押下時、'2'を送信
                    if(count==0){
                        mmOutputStream!!.write(index.toByteArray())
                        count++
                    }else{
                        mmOutputStream!!.write(index.toByteArray())
                        count--
                    }
                    // 画面上に"Write:"を表示
                    mStatusTextView!!.text = "Connected:"
                } catch (e: IOException) {
                    val valueMsg = Message()
                    valueMsg.what = VIEW_STATUS
                    valueMsg.obj = "Error2:$e"
                    mHandler.sendMessage(valueMsg)
                }
    }


    private var mHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            val action = msg.what
            val msgStr = msg.obj as String
            if (action == VIEW_INPUT) {
                mInputTextView!!.text = msgStr
            } else if (action == VIEW_STATUS) {
                mStatusTextView!!.text = msgStr
            }
        }
    }

    fun manageMyConnectedSocket(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            if (mmSocket == null) {
                return
            }
            val socket = mmSocket
            socket ?: return
            socket.connect()
            manageMyConnectedSocket(socket)
            mmOutputStream = mmSocket?.outputStream
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }


    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()
            Log.d(TAG, "connect start!")
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
                Log.d(TAG, mmBuffer[0].toString())
            }
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}