package com.main.covid.service.bluetooth

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.main.covid.BuildConfig
import com.main.covid.bluetracer.Preference
import com.main.covid.bluetracer.bluetooth.BLEAdvertiser
import com.main.covid.bluetracer.bluetooth.gatt.ACTION_RECEIVED_STATUS
import com.main.covid.bluetracer.bluetooth.gatt.ACTION_RECEIVED_STREETPASS
import com.main.covid.bluetracer.bluetooth.gatt.STATUS
import com.main.covid.bluetracer.bluetooth.gatt.STREET_PASS
import com.main.covid.db.status.Status
import com.main.covid.db.status.persistence.StatusRecord
import com.main.covid.db.status.persistence.StatusRecordStorage
import com.main.covid.db.streetpass.StreetPassRecord
import com.main.covid.db.streetpass.StreetPassRecordStorage
import com.main.covid.bluetracer.idmanager.TempIDManager
import com.main.covid.bluetracer.idmanager.TemporaryID
import com.main.covid.bluetracer.logging.CentralLog
import com.main.covid.bluetracer.permissions.RequestFileWritePermission
import com.main.covid.bluetracer.streetpass.ConnectionRecord
import com.main.covid.bluetracer.streetpass.StreetPassScanner
import com.main.covid.bluetracer.streetpass.StreetPassServer
import com.main.covid.bluetracer.streetpass.StreetPassWorker
import com.main.covid.db.SharedPrefsRepository
import com.main.covid.notification.BluetracerNotificationTemplates
import com.main.covid.utils.BluetracerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

class BluetoothMonitoringService : Service(), CoroutineScope {

    private val sharedPrefsRepository: SharedPrefsRepository by inject(SharedPrefsRepository::class.java)

    private var mNotificationManager: NotificationManager? = null

    private lateinit var serviceUUID: String

    private var streetPassServer: StreetPassServer? = null
    private var streetPassScanner: StreetPassScanner? = null
    private var advertiser: BLEAdvertiser? = null

    private var worker: StreetPassWorker? = null

    private val streetPassReceiver = StreetPassReceiver()
    private val statusReceiver = StatusReceiver()
    private val bluetoothStatusReceiver = BluetoothStatusReceiver()

    private lateinit var streetPassRecordStorage: StreetPassRecordStorage
    private lateinit var statusRecordStorage: StatusRecordStorage

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var commandHandler: CommandHandler
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var notificationShown: NOTIFICATION_STATE? = null

    override fun onCreate() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        setup()
    }

    private fun setup() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        CentralLog.setPowerManager(pm)

        commandHandler =
            CommandHandler(WeakReference(this))

        CentralLog.d(TAG, "Creating service - BluetoothMonitoringService")
        serviceUUID = BuildConfig.BLE_SSID

        worker = StreetPassWorker(this.applicationContext)

        unregisterReceivers()
        registerReceivers()

        streetPassRecordStorage = StreetPassRecordStorage(this.applicationContext)
        statusRecordStorage = StatusRecordStorage(this.applicationContext)

        setupNotifications()
        broadcastMessage = TempIDManager.retrieveTemporaryID(this.applicationContext)
    }

    fun teardown() {
        streetPassServer?.tearDown()
        streetPassServer = null

        streetPassScanner?.stopScan()
        streetPassScanner = null

        commandHandler.removeCallbacksAndMessages(null)

        BluetracerUtils.cancelBMUpdateCheck(this.applicationContext)
        BluetracerUtils.cancelNextScan(this.applicationContext)
        BluetracerUtils.cancelNextAdvertise(this.applicationContext)
    }

    private fun setupNotifications() {

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name =
                CHANNEL_SERVICE
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mChannel.enableLights(false)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(0L)
            mChannel.setSound(null, null)
            mChannel.setShowBadge(false)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    private fun notifyLackingThings(override: Boolean = false) {
        if (notificationShown != NOTIFICATION_STATE.LACKING_THINGS || override) {
            val notif =
                BluetracerNotificationTemplates.lackingThingsNotification(this.applicationContext,
                    CHANNEL_ID
                )
            startForeground(NOTIFICATION_ID, notif)
            notificationShown =
                NOTIFICATION_STATE.LACKING_THINGS
        }
    }

    private fun notifyRunning(override: Boolean = false) {
        if (notificationShown != NOTIFICATION_STATE.RUNNING || override) {
            val notif =
                BluetracerNotificationTemplates.getRunningNotification(this.applicationContext,
                    CHANNEL_ID
                )
            startForeground(NOTIFICATION_ID, notif)
            notificationShown =
                NOTIFICATION_STATE.RUNNING
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val perms = BluetracerUtils.getRequiredPermissions()
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun hasWritePermissions(): Boolean {
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun acquireWritePermission() {
        val intent = Intent(this.applicationContext, RequestFileWritePermission::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun isBluetoothEnabled(): Boolean {
        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CentralLog.i(TAG, "Service onStartCommand")

        //check for permissions
        if (!isBluetoothEnabled()) {
            CentralLog.i(
                TAG,
                "bluetooth: ${isBluetoothEnabled()}"
            )
            notifyLackingThings()
            return START_STICKY
        }

        intent?.let {
            val cmd = intent.getIntExtra(COMMAND_KEY, Command.INVALID.index)
            runService(
                Command.findByValue(
                    cmd
                )
            )

            return START_STICKY
        }

        if (intent == null) {
            CentralLog.e(TAG, "WTF? Nothing in intent @ onStartCommand")
//            Utils.startBluetoothMonitoringService(applicationContext)
            commandHandler.startBluetoothMonitoringService()
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY
    }

    fun runService(cmd: Command?) {

        val doWork = true
        CentralLog.i(TAG, "Command is:${cmd?.string}")

        //check for permissions
        if (!isBluetoothEnabled()) {
            CentralLog.i(
                TAG,
                "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            notifyLackingThings()
            return
        }

        //show running foreground notification if its not showing that
        notifyRunning()

        when (cmd) {
            Command.ACTION_START -> {
                setupService()
                BluetracerUtils.scheduleNextHealthCheck(this.applicationContext,
                    healthCheckInterval
                )
                BluetracerUtils.scheduleRepeatingPurge(this.applicationContext,
                    purgeInterval
                )
                BluetracerUtils.scheduleBMUpdateCheck(this.applicationContext,
                    bmCheckInterval
                )
                actionStart()
            }

            Command.ACTION_SCAN -> {
                scheduleScan()

                if (doWork) {
                    actionScan()
                }
            }

            Command.ACTION_ADVERTISE -> {
                scheduleAdvertisement()
                if (doWork) {
                    actionAdvertise()
                }
            }

            Command.ACTION_UPDATE_BM -> {
                BluetracerUtils.scheduleBMUpdateCheck(this.applicationContext,
                    bmCheckInterval
                )
                actionUpdateBm()
            }

            Command.ACTION_STOP -> {
                actionStop()
            }

            Command.ACTION_SELF_CHECK -> {
                BluetracerUtils.scheduleNextHealthCheck(this.applicationContext,
                    healthCheckInterval
                )
                if (doWork) {
                    actionHealthCheck()
                }
            }

            Command.ACTION_PURGE -> {
                actionPurge()
            }

            else -> CentralLog.i(TAG, "Invalid / ignored command: $cmd. Nothing to do")
        }
    }

    private fun actionStop() {
        stopForeground(true)
        stopSelf()
        CentralLog.w(TAG, "Service Stopping")
    }

    private fun actionHealthCheck() {
//        performUserLoginCheck()
        performHealthCheck()
        BluetracerUtils.scheduleRepeatingPurge(this.applicationContext,
            purgeInterval
        )
    }

    private fun actionPurge() {
        performPurge()
    }

    private fun actionStart() {
        CentralLog.d(TAG, "Action Start")

//        //TODO uncomment to run locally
//        TempIDManager.getTemporaryIDMocks(this)
//
//        CentralLog.d(TAG, "Get TemporaryIDs completed")
//        //this will run whether it starts or fails.
//        val fetch = TemporaryID(1000000000000000, "Rubenid2", 20000000000000000L)
//        fetch.let {
//            broadcastMessage = it
//            setupCycles()
//        }

        TempIDManager.getTemporaryIDs(this, object: TempIDManager.ApiCallback {
            override fun onSuccess(context: Context) {

                CentralLog.d(TAG, "Get TemporaryIDs completed")
                //this will run whether it starts or fails.
                val fetch = TempIDManager.retrieveTemporaryID(context)
                fetch?.let {
                    broadcastMessage = it
                    setupCycles()
                }
            }

            override fun onFailure(context: Context) {
                CentralLog.d(TAG, "Get TemporaryIDs completed incorrectly" +
                        ". Setting up cycles anyways")
                setupCycles()

            }
        })


    }

    private fun actionUpdateBm() {

//        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
//            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionUpdateBM")
//            //need to pull new BM
//            TempIDManager.getTemporaryIDMocks(this)
//            //this will run whether it starts or fails.
//            val fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
//            fetch?.let {
//                CentralLog.i(TAG, "[TempID] Updated Temp ID")
//                broadcastMessage = it
//            }
//
//            if (fetch == null) {
//                CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID")
//            }
//
//        } else {
//            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionUpdateBM")
//        }

        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionUpdateBM")
            //need to pull new BM
            TempIDManager.getTemporaryIDs(this, object : TempIDManager.ApiCallback {
                override fun onSuccess(context: Context) {
                    //this will run whether it starts or fails.
                    val fetch = TempIDManager.retrieveTemporaryID(context)
                    fetch?.let {
                        CentralLog.i(TAG, "[TempID] Updated Temp ID")
                        broadcastMessage = it
                    }

                    if (fetch == null) {
                        CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID")
                    }
                }

                override fun onFailure(context: Context) {
                    CentralLog.d(TAG, "Get TemporaryIDs completed incorrectly")
                    CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionUpdateBM")
                }
            })
        }
        else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionUpdateBM")
        }
    }

    private fun calcPhaseShift(min: Long, max: Long): Long {
        return (min + (Math.random() * (max - min))).toLong()
    }

    private fun actionScan() {
//        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
//            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionScan")
//            //need to pull new BM
//            TempIDManager.getTemporaryIDMocks(this.applicationContext)
//                //this will run whether it starts or fails.
//                val fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
//                fetch?.let {
//                    broadcastMessage = it
//                    performScan()
//                }
//                performScan()
//
//        } else {
//            performScan()
//            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionScan")
//        }

        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionScan")
            //need to pull new BM
            TempIDManager.getTemporaryIDs(this, object : TempIDManager.ApiCallback {
                override fun onSuccess(context: Context) {
                    //this will run whether it starts or fails.
                    val fetch = TempIDManager.retrieveTemporaryID(context)
                    fetch?.let {
                        broadcastMessage = it
                        performScan()
                    }
                    performScan()
                }

                override fun onFailure(context: Context) {
                    CentralLog.d(TAG, "Get TemporaryIDs completed incorrectly")
                    CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionScan")
                    performScan()
                }
            })
        } else {
            performScan()
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionScan")
        }
    }

    private fun actionAdvertise() {
        setupAdvertiser()
        if (isBluetoothEnabled()) {
            advertiser?.startAdvertising(advertisingDuration)
        } else {
            CentralLog.w(TAG, "Unable to start advertising, bluetooth is off")
        }
    }

    private fun setupService() {
        streetPassServer =
            streetPassServer ?: StreetPassServer(this.applicationContext, serviceUUID)
        setupScanner()
        setupAdvertiser()
    }

    private fun setupScanner() {
        streetPassScanner = streetPassScanner ?: StreetPassScanner(
            this,
            serviceUUID,
            scanDuration
        )
    }

    private fun setupAdvertiser() {
        advertiser = advertiser ?: BLEAdvertiser(serviceUUID)
    }

    private fun setupCycles() {
        setupScanCycles()
        setupAdvertisingCycles()
    }

    private fun setupScanCycles() {
        commandHandler.scheduleNextScan(0)
    }

    private fun setupAdvertisingCycles() {
        commandHandler.scheduleNextAdvertise(0)
    }

    private fun performScan() {
        setupScanner()
        startScan()
    }

    private fun scheduleScan() {
        if (!infiniteScanning) {
            commandHandler.scheduleNextScan(
                scanDuration + calcPhaseShift(
                    minScanInterval,
                    maxScanInterval
                )
            )
        }
    }

    private fun scheduleAdvertisement() {
        if (!infiniteAdvertising) {
            commandHandler.scheduleNextAdvertise(advertisingDuration + advertisingGap)
        }
    }

    private fun startScan() {

        if (isBluetoothEnabled()) {

            streetPassScanner?.let { scanner ->
                if (!scanner.isScanning()) {
                    scanner.startScan()
                } else {
                    CentralLog.e(TAG, "Already scanning!")
                }
            }
        } else {
            CentralLog.w(TAG, "Unable to start scan - bluetooth is off")
        }
    }

    private fun performHealthCheck() {

        CentralLog.i(TAG, "Performing self diagnosis")

        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(TAG, "no location permission")
            notifyLackingThings(true)
            return
        }

        notifyRunning(true)

        //ensure our service is there
        setupService()

        if (!infiniteScanning) {
            if (!commandHandler.hasScanScheduled()) {
                CentralLog.w(TAG, "Missing Scan Schedule - rectifying")
                commandHandler.scheduleNextScan(100)
            } else {
                CentralLog.w(TAG, "Scan Schedule present")
            }
        } else {
            CentralLog.w(TAG, "Should be operating under infinite scan mode")
        }

        if (!infiniteAdvertising) {
            if (!commandHandler.hasAdvertiseScheduled()) {
                CentralLog.w(TAG, "Missing Advertise Schedule - rectifying")
//                setupAdvertisingCycles()
                commandHandler.scheduleNextAdvertise(100)
            } else {
                CentralLog.w(
                    TAG,
                    "Advertise Schedule present. Should be advertising?:  ${advertiser?.shouldBeAdvertising
                        ?: false}. Is Advertising?: ${advertiser?.isAdvertising ?: false}"
                )
            }
        } else {
            CentralLog.w(TAG, "Should be operating under infinite advertise mode")
        }


    }

    private fun performPurge() {
        val context = this
        launch {
            val before = System.currentTimeMillis() - purgeTTL
            CentralLog.i(TAG, "Coroutine - Purging of data before epoch time $before")

            streetPassRecordStorage.purgeOldRecords(before)
            statusRecordStorage.purgeOldRecords(before)
            Preference.putLastPurgeTime(context, System.currentTimeMillis())
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed - tearing down")
        stopService()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed")
    }

    private fun stopService() {
        teardown()
        unregisterReceivers()

        worker?.terminateConnections()
        worker?.unregisterReceivers()

        job.cancel()
    }


    private fun registerReceivers() {
        val recordAvailableFilter = IntentFilter(ACTION_RECEIVED_STREETPASS)
        localBroadcastManager.registerReceiver(streetPassReceiver, recordAvailableFilter)

        val statusReceivedFilter = IntentFilter(ACTION_RECEIVED_STATUS)
        localBroadcastManager.registerReceiver(statusReceiver, statusReceivedFilter)

        val bluetoothStatusReceivedFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStatusReceiver, bluetoothStatusReceivedFilter)

        CentralLog.i(TAG, "Receivers registered")
    }

    private fun unregisterReceivers() {
        try {
            localBroadcastManager.unregisterReceiver(streetPassReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "streetPassReceiver is not registered?")
        }

        try {
            localBroadcastManager.unregisterReceiver(statusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "statusReceiver is not registered?")
        }

        try {
            unregisterReceiver(bluetoothStatusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "bluetoothStatusReceiver is not registered?")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                    when (state) {
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
                            notifyLackingThings()
                            teardown()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_OFF")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_ON")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_ON")
                            if(sharedPrefsRepository.getBTOn())
                                BluetracerUtils.startBluetoothMonitoringService(this@BluetoothMonitoringService.applicationContext)
                        }
                    }
                }
            }
        }
    }

    inner class StreetPassReceiver : BroadcastReceiver() {

        private val TAG = "StreetPassReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STREETPASS == intent.action) {
                val connRecord: ConnectionRecord = intent.getParcelableExtra(STREET_PASS)
                CentralLog.d(
                    TAG,
                    "StreetPass received: $connRecord"
                )

                if (connRecord.msg.isNotEmpty()) {
                    val record = StreetPassRecord(
                        v = connRecord.version,
                        msg = connRecord.msg,
                        org = connRecord.org,
                        modelP = connRecord.peripheral.modelP,
                        modelC = connRecord.central.modelC,
                        rssi = connRecord.rssi,
                        txPower = connRecord.txPower
                    )

                    launch {
                        CentralLog.d(
                            TAG,
                            "Coroutine - Saving StreetPassRecord: ${BluetracerUtils.getDate(record.timestamp)}"
                        )
                        streetPassRecordStorage.saveRecord(record)
                    }
                }
            }
        }
    }

    inner class StatusReceiver : BroadcastReceiver() {
        private val TAG = "StatusReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STATUS == intent.action) {
                val statusRecord: Status = intent.getParcelableExtra(STATUS)
                CentralLog.d(TAG, "Status received: ${statusRecord.msg}")

                if (statusRecord.msg.isNotEmpty()) {
                    val statusRecord = StatusRecord(statusRecord.msg)
                    launch {
                        statusRecordStorage.saveRecord(statusRecord)
                    }
                }
            }
        }
    }

    enum class Command(val index: Int, val string: String) {
        INVALID(-1, "INVALID"),
        ACTION_START(0, "START"),
        ACTION_SCAN(1, "SCAN"),
        ACTION_STOP(2, "STOP"),
        ACTION_ADVERTISE(3, "ADVERTISE"),
        ACTION_SELF_CHECK(4, "SELF_CHECK"),
        ACTION_UPDATE_BM(5, "UPDATE_BM"),
        ACTION_PURGE(6, "PURGE");

        companion object {
            private val types = values().associate { it.index to it }
            fun findByValue(value: Int) = types[value]
        }
    }

    enum class NOTIFICATION_STATE {
        RUNNING,
        LACKING_THINGS
    }

    companion object {

        const val TAG = "BTMService"

        private const val NOTIFICATION_ID = BuildConfig.SERVICE_FOREGROUND_NOTIFICATION_ID
        private const val CHANNEL_ID = BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
        const val CHANNEL_SERVICE = BuildConfig.SERVICE_FOREGROUND_CHANNEL_NAME

        const val PUSH_NOTIFICATION_ID = BuildConfig.PUSH_NOTIFICATION_ID

        const val COMMAND_KEY = "${BuildConfig.APPLICATION_ID}_CMD"

        const val PENDING_ACTIVITY = 5
        const val PENDING_START = 6
        const val PENDING_SCAN_REQ_CODE = 7
        const val PENDING_ADVERTISE_REQ_CODE = 8
        const val PENDING_HEALTH_CHECK_CODE = 9
        const val PENDING_WIZARD_REQ_CODE = 10
        const val PENDING_BM_UPDATE = 11
        const val PENDING_PURGE_CODE = 12

        var broadcastMessage: TemporaryID? = null

        //should be more than advertising gap?
        const val scanDuration: Long = BuildConfig.SCAN_DURATION
        const val minScanInterval: Long = BuildConfig.MIN_SCAN_INTERVAL
        const val maxScanInterval: Long = BuildConfig.MAX_SCAN_INTERVAL

        const val advertisingDuration: Long = BuildConfig.ADVERTISING_DURATION
        const val advertisingGap: Long = BuildConfig.ADVERTISING_INTERVAL

        const val maxQueueTime: Long = BuildConfig.MAX_QUEUE_TIME
        const val bmCheckInterval: Long = BuildConfig.BM_CHECK_INTERVAL
        const val healthCheckInterval: Long = BuildConfig.HEALTH_CHECK_INTERVAL
        const val purgeInterval: Long = BuildConfig.PURGE_INTERVAL
        const val purgeTTL: Long = BuildConfig.PURGE_TTL

        const val connectionTimeout: Long = BuildConfig.CONNECTION_TIMEOUT

        const val blacklistDuration: Long = BuildConfig.BLACKLIST_DURATION

        const val infiniteScanning = false
        const val infiniteAdvertising = false

        const val useBlacklist = true
        const val bmValidityCheck = false
    }
}
