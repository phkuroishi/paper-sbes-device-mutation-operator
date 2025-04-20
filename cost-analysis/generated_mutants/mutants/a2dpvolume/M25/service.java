package a2dp.Vol;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothA2dp;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import static a2dp.Vol.R.drawable.ic_launcher;

public class service extends Service implements OnAudioFocusChangeListener {

    private static final String A2DP_BACKGROUND = "a2dp_background";
    private static final String A2DP_FOREGROUND = "a2dp_foreground";
    NotificationChannel channel_b, channel_f;
    private boolean carEnterReceiverRegistered = false;
    private boolean carExitReceiverRegistered = false;
    private boolean homeExitRegistered = false;
    private boolean homeEnterRegistered = false;
    private boolean powerExitRegistered = false;
    private boolean powerEnterRegistered = false;
    private boolean headSetPlugReciverRegistered = false;

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        createNotificationChannel();
        updateNot();
        return START_STICKY;
    }

    private TextToSpeech mTts;
    public static boolean mTtsReady = false;
    static AudioManager am2 = (AudioManager) null;
    private static Integer OldVol = 5;
    private static Integer OldVol2 = 5;
    private static Integer Oldsilent;
    public static Integer connects = 0;
    public static boolean run = false;
    public static boolean ScoRegistered = false;
    private static int mvolsLeft = 0;
    private static int pvolsLeft = 0;
    private static String notify_pref = "always";
    public static btDevice[] btdConn = new btDevice[20]; // n the devices in the
    // database that has
    // connected
    private DeviceDB DB; // database of device data stored in SQlite
    static boolean mIsBound = false;
    static IBluetoothA2dp ibta2;
    static String DeviceToConnect = null;
    private boolean carMode = true;
    private boolean homeDock = false;
    private boolean headsetPlug = false;
    private boolean power = false;
    private boolean enableGTalk = false;
    private boolean enableSMS = false;
    private static boolean ramp_vol = false;
    HashMap<String, String> myHash;
    private boolean toasts = true;
    private boolean notify = true;
    private static boolean hideVolUi = false;
    private NotificationManager mNotificationManager = null;
    private NotificationManagerCompat notificationManagerCompat = null;
    private boolean speakerPhoneWasOn = true;
    private boolean musicWasPlaying = false;
    private boolean bluetoothWasOff = false;
    private boolean clearedTts = true;
    private static final String FIX_STREAM = "fix_stream";
    AudioFocusRequest afr;
    private String LastMessage;

    boolean oldwifistate = true;
    boolean tmessageRegistered = false; // tracks state of the message reader receiver
    WifiManager wifiManager;
    LocationManager locmanager;
    String a2dpDir = "";
    boolean local;
    private static final String A2DP_Vol = "A2DP_Vol";
    private static final String LOG_TAG = "A2DP_Volume";
    // private static final String MY_UUID_STRING =
    // "af87c0d0-faac-11de-a839-0800200c9a66";
    private static final String OLD_VOLUME = "old_vol";
    private static final String OLD_PH_VOL = "old_phone_vol";
    private static final int MUSIC_STREAM = 0;
    private static final int IN_CALL_STREAM = 1;
    private static final int ALARM_STREAM = 2;

    private PackageManager mPackageManager;
    public static final String PREFS_NAME = "btVol";
    private static final String CON_PREFS_NAME = "a2dp.Vol.ConnectWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    private int MAX_MESSAGE_LENGTH = 350;
    float MAX_ACC = 10; // worst acceptable location in meters
    long MAX_TIME = 20000; // gps listener timout time in milliseconds and
    private long Message_delay = 3000; // delay before reading SMS
    private int MessageStream = 0;
    private long vol_delay = 5000; // delay time between the device connection
    // and the volume adjustment

    SharedPreferences preferences;
    private static MyApplication application;

    private volatile boolean connecting = false;
    private volatile boolean disconnecting = false;
    private int connectedIcon;
    private TelephonyManager tm; // Context.getSystemService(Context.TELEPHONY_SERVICE);

    // permissions status flags
    Boolean permReadContacts = true;
    Boolean permLocation = true;
    Boolean permPhone = true;
    Boolean permSMS = true;
    Boolean permStorage = true;


    /*
     * private HandlerThread thread; private LinkedList<String> addresses;
     * private TalkObserver observer;
     */
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
/*        if ((channel_b == null)||(channel_f == null)) {
            createNotificationChannel();
        }*/
        return null;
    }

    @Override
    public void onCreate() {

        application = (MyApplication) this.getApplication();
/*
        if ((channel_b == null)||(channel_f == null)) {
            createNotificationChannel();
        }
*/

        // get and load preferences

        try {
            preferences = PreferenceManager
                    .getDefaultSharedPreferences(application);

            carMode = preferences.getBoolean("car_mode", true);
            homeDock = preferences.getBoolean("home_dock", false);
            headsetPlug = preferences.getBoolean("headset", false);
            power = preferences.getBoolean("power", false);
            toasts = preferences.getBoolean("toasts", true);
            // notify = preferences.getBoolean("notify1", true);
            enableSMS = preferences.getBoolean("enableTTS", false);
            enableGTalk = preferences.getBoolean("enableGTalk", true);
            notify_pref = preferences.getString("notify_pref", "always");
            hideVolUi = preferences.getBoolean("hideVolUi", false);
            // Long yyy = new Long(preferences.getString("gpsTime", "15000"));
            MAX_TIME = Long.valueOf(preferences.getString("gpsTime", "15000"));

            // Float xxx = new Float(preferences.getString("gpsDistance",
            // "10"));
            MAX_ACC = Float.valueOf(preferences.getString("gpsDistance", "10"));

            local = preferences.getBoolean("useLocalStorage", false);
            if (local)
                a2dpDir = getFilesDir().toString();
            else
                a2dpDir = Environment.getExternalStorageDirectory()
                        + "/A2DPVol";

            OldVol2 = preferences.getInt(OLD_VOLUME, 10);
            OldVol = preferences.getInt(OLD_PH_VOL, 5);
            Oldsilent = preferences.getInt("oldsilent", 10);

        } catch (NumberFormatException e) {
            MAX_ACC = 10;
            MAX_TIME = 15000;
            Toast.makeText(this, "prefs failed to load ", Toast.LENGTH_LONG)
                    .show();
            e.printStackTrace();
            Log.e(LOG_TAG, "prefs failed to load " + e.getMessage());
        }

        registerRecievers();

        // create audio manager instance
        am2 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // open database instance
        this.DB = new DeviceDB(application);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);

        locmanager = (LocationManager) getBaseContext().getSystemService(
                Context.LOCATION_SERVICE);

        // set run flag to true. This is used for the GUI
        run = true;
        // if all the above works, let the user know it is started
        if (toasts)
            Toast.makeText(this, R.string.ServiceStarted, Toast.LENGTH_LONG)
                    .show();

        // Tell the world we are running
        final String IRun = "a2dp.vol.service.RUNNING";
        Intent i = new Intent();
        i.setAction(IRun);
        application.sendBroadcast(i);

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        mPackageManager = getPackageManager();

        notify = notify_pref.equalsIgnoreCase("always")
                || notify_pref.equalsIgnoreCase("connected_only");

        // we will ignore the no notifications preference in API 25 and up since this can be managed
        // by the user now within Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            notify = true;
        }

        if (notify) {
            updateNot();
        }
        getConnects();
        if (connects < 1) {
            getOldvol();
        }
        // test location file maker
        /*
         * FileOutputStream fos; try { fos = openFileOutput("My_Last_Location",
         * Context.MODE_WORLD_READABLE); String temp =
         * "http://maps.google.com/maps?q=40.7423612,-89.63056078333334+(Lambo 11/26/10, 05:59:46 pm acc=8)"
         * ; fos.write(temp.getBytes()); fos.close(); } catch
         * (FileNotFoundException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
         * catch block e.printStackTrace(); }
         */
        // end test file maker
        /*
         * if (enableTTS) { mTts = new TextToSpeech(application,
         * listenerStarted); }
         */
    }

    private void registerRecievers() {
        // create intent filter for a bluetooth stream connection
        IntentFilter filter = new IntentFilter(
                android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED);

        // create intent filter for a bluetooth stream disconnection
//        IntentFilter filter2 = new IntentFilter(
//                android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
        // M25
        IntentFilter filter2 = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);

        IntentFilter btNotEnabled = new IntentFilter(
                android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
        application.registerReceiver(btOFFReciever, btNotEnabled);

        IntentFilter clearMessage = new IntentFilter("a2dp.vol.service.CLEAR");
        application.registerReceiver(messageClear, clearMessage);

        IntentFilter updateNotification = new IntentFilter("a2dp.vol.service.NOTIFY");
        application.registerReceiver(runUpdate, updateNotification);

        application.registerReceiver(mReceiver, filter);
        application.registerReceiver(mReceiver2, filter2);

        if (carMode) {
            // Create listener for when car mode disconnects
            IntentFilter carExit = new IntentFilter(android.app.UiModeManager.ACTION_EXIT_CAR_MODE);
            if (!carExitReceiverRegistered) {
                application.registerReceiver(CarExitReceiver, carExit);
                carExitReceiverRegistered = true;
                Log.i(LOG_TAG, "Car Mode exit receiver registered");
            }

            // Create listener for when car mode connects
            IntentFilter carEnter = new IntentFilter(android.app.UiModeManager.ACTION_ENTER_CAR_MODE);
            if (!carEnterReceiverRegistered) {
                application.registerReceiver(CarEnterReceiver, carEnter);
                carEnterReceiverRegistered = true;
                Log.i(LOG_TAG, "Car Mode enter receiver registered");
            }
        }

        if (homeDock) {
            // Create listener for when car mode disconnects
            IntentFilter homeExit = new IntentFilter(android.app.UiModeManager.ACTION_EXIT_DESK_MODE);
            if (!homeExitRegistered) {
                application.registerReceiver(HomeExitReceiver, homeExit);
                homeExitRegistered = true;
                Log.i(LOG_TAG, "Home Dock exit receiver registered");
            }

            // Create listener for when car mode connects
            IntentFilter homeEnter = new IntentFilter(android.app.UiModeManager.ACTION_ENTER_DESK_MODE);
            if (!homeEnterRegistered) {
                application.registerReceiver(HomeEnterReceiver, homeEnter);
                homeEnterRegistered = true;
                Log.i(LOG_TAG, "Home Dock enter receiver registered");
            }
        }

        if (power) {
            // Create listener for when power disconnects
            IntentFilter powerExit = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
            if (!powerExitRegistered) {
                application.registerReceiver(PowerExitReceiver, powerExit);
                powerExitRegistered = true;
                Log.i(LOG_TAG, "Power exit receiver registered");
            }

            // Create listener for when power connects
            IntentFilter powerEnter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
            if (!powerEnterRegistered) {
                application.registerReceiver(PowerEnterReceiver, powerEnter);
                powerEnterRegistered = true;
                Log.i(LOG_TAG, "Power enter receiver registered");
            }
        }

        if (headsetPlug) {
            // create listener for headset plug
            IntentFilter filter7 = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            if (!headSetPlugReciverRegistered) {
                application.registerReceiver(headSetReceiver, filter7);
                headSetPlugReciverRegistered = true;
                Log.i(LOG_TAG, "Audio Jack receiver registered");
            }
        }


    }

    @Override
    public void onDestroy() {
        // let the GUI know we closed
        mNotificationManager.cancelAll();
        run = false;
        Log.i(LOG_TAG, "Service stopped");
        // in case the location listener is running, stop it
        stopService(new Intent(application, StoreLoc.class));
        // close the database
        try {
            application.unregisterReceiver(mReceiver);
            application.unregisterReceiver(mReceiver2);
            application.unregisterReceiver(btOFFReciever);
            application.unregisterReceiver(runUpdate);
            if (headSetPlugReciverRegistered) {
                application.unregisterReceiver(headSetReceiver);
                headSetPlugReciverRegistered = false;
                Log.i(LOG_TAG, "Audio Jack receiver unregistered");
            }
            if (carEnterReceiverRegistered) {
                application.unregisterReceiver(CarEnterReceiver);
                carEnterReceiverRegistered = false;
                Log.i(LOG_TAG, "Car Enter receiver unregistered");
            }

            if (carExitReceiverRegistered) {
                application.unregisterReceiver(CarExitReceiver);
                carExitReceiverRegistered = false;
                Log.i(LOG_TAG, "Car Exit receiver unregistered");
            }

            if (homeEnterRegistered) {
                application.unregisterReceiver(HomeEnterReceiver);
                homeEnterRegistered = false;
                Log.i(LOG_TAG, "Home enter receiver unregistered");
            }

            if (homeExitRegistered) {
                application.unregisterReceiver(HomeExitReceiver);
                homeExitRegistered = false;
                Log.i(LOG_TAG, "Home exit receiver unregistered");
            }

            if (powerExitRegistered) {
                application.unregisterReceiver(PowerExitReceiver);
                powerExitRegistered = false;
                Log.i(LOG_TAG, "Power exit receiver unregistered");
            }

            if (powerEnterRegistered) {
                application.unregisterReceiver(PowerEnterReceiver);
                powerEnterRegistered = false;
                Log.i(LOG_TAG, "Power enter receiver unregistered");
            }

            if (mTtsReady) {
                try {
                    if (!clearedTts) {
                        clearTts();
                    }
                    if (mTts != null)
                        mTts.shutdown();
                    mTtsReady = false;

                    if (ScoRegistered) {
                        application.unregisterReceiver(sco_change);
                        ScoRegistered = false;
                    }

                    if (tmessageRegistered) {
                        application.unregisterReceiver(tmessage);
                        tmessageRegistered = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            DB.getDb().close();
            application.unregisterReceiver(messageClear);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Tell the world we are not running
        final String IStop = "a2dp.vol.service.STOPPED_RUNNING";
        Intent i = new Intent();
        i.setAction(IStop);
        application.sendBroadcast(i);


        // let the user know the service stopped
        if (toasts)
            Toast.makeText(this, R.string.ServiceStopped, Toast.LENGTH_LONG)
                    .show();
        if (mIsBound) {
            doUnbind();
        }
        this.stopForeground(true);
    }


    public void onStart() {

        run = true;
        connecting = false;
        disconnecting = false;
        if (notify)
            updateNot();
        getConnects();
        if (connects < 1) {
            getOldvol();
        }

        Log.i(LOG_TAG, "Service started");
    }


    private final BroadcastReceiver tmessage = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            TextReader(intent.getStringExtra("message"));

        }

    };

    private final BroadcastReceiver messageClear = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            clearTts();

        }

    };

    private final BroadcastReceiver runUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNot();
        }
    };

    // used to clear all the Bluetooth connections if the Bluetooth adapter has
    // been turned OFF.
    private final BroadcastReceiver btOFFReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int state1 = android.bluetooth.BluetoothAdapter.STATE_OFF;
            int state2 = android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF;
            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
            String mac = "";
            if (mBTA.getState() == state1) {

                if (btdConn != null)
                    for (int j = 0; j < btdConn.length; j++) {
                        if (btdConn[j] != null)
                            if (btdConn[j].getMac().length() > 2) {
                                mac = btdConn[j].getMac();
                                DoDisconnected(btdConn[j]);
                                btdConn[j] = null;
                            }
                    }
                getConnects();

                if (mTtsReady) {
                    try {
                        if (!clearedTts) {
                            clearTts();
                        }
                        //mTts.shutdown();
                        //mTtsReady = false;
                        //unregisterReceiver(SMScatcher);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String Ireload = "a2dp.Vol.main.RELOAD_LIST";
                Intent itent = new Intent();
                itent.setAction(Ireload);
                itent.putExtra("disconnect", mac);
                application.sendBroadcast(itent);

                Log.i(LOG_TAG, "Bluetooth turned OFF broadcast receiver " + mac);
                //updateA2DPconnect2();
            }
        }
    };

    private final BroadcastReceiver headSetReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            int state = intent.getIntExtra("state", -1);
            btDevice bt2 = null;
            try {
                bt2 = DB.getBTD("3"); // get headset plug data
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error receiving audio jack data from databse " + e.getLocalizedMessage());
                return;
            }
            if (bt2 != null && "3".equalsIgnoreCase(bt2.getMac())) {
                if (state == 0 && connects > 0) {
                    disconnecting = true;
                    DoDisconnected(bt2);
                } else if (state == 1) {
                    connecting = true;
                    DoConnected(bt2);
                }
            }
            Log.i(LOG_TAG, "Audio jack received " + state);
        }

    };

    // a device has just connected. Do the on connect stuff
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!connecting) {
                connecting = true;

                BluetoothDevice bt;
                try {
                    bt = (BluetoothDevice) Objects.requireNonNull(intent.getExtras()).get(
                            BluetoothDevice.EXTRA_DEVICE);
                } catch (Exception e1) {
                    bt = null;
                    e1.printStackTrace();
                    connecting = false;
                    //return;
                }

                btDevice bt2 = null;

                // first see if a bluetooth device connected
                if (bt != null) {
                    try {
                        String addres = bt.getAddress();
                        bt2 = DB.getBTD(addres);

                    } catch (Exception e) {

                        bt2 = null;
                        connecting = false;
                        //return;
                    }
                } else
                // if not a bluetooth device, must be a special device
                {
                    try {
                        // Log.d(LOG_TAG, intent.toString());
                        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(
                                "android.app.action.ENTER_CAR_MODE")) {
                            bt2 = DB.getBTD("1"); // get car mode data
                        } else if (intent.getAction().equalsIgnoreCase(
                                "android.app.action.ENTER_DESK_MODE")) {
                            bt2 = DB.getBTD("2"); // get home dock data
                        } else if (intent.getAction().equalsIgnoreCase(
                                Intent.ACTION_POWER_CONNECTED)) {
                            bt2 = DB.getBTD("4"); // get power data

                        } else
                            bt2 = null;

                    } catch (Exception e) {

                        bt2 = null;
                        Log.e(LOG_TAG, "Error" + e.toString());
                    }
                }
                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    connecting = false;
                    Log.i(LOG_TAG, "Unknown device received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Broadcast received: " + bt2.getDesc1() + ", " + bt2.getDesc2());
                    DoConnected(bt2);
                }

            }
            //updateA2DPconnect2();
        }
    };

    private final BroadcastReceiver CarEnterReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!connecting) {
                connecting = true;

                btDevice bt2 = DB.getBTD("1");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    connecting = false;
                    Log.i(LOG_TAG, "Unknown device received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Broadcast received: " + bt2.getDesc1() + ", " + bt2.getDesc2());
                    DoConnected(bt2);
                }
            }
        }
    };

    private final BroadcastReceiver HomeEnterReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!connecting) {
                connecting = true;

                btDevice bt2 = DB.getBTD("2");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    connecting = false;
                    Log.i(LOG_TAG, "Unknown device received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Broadcast received: " + bt2.getDesc1() + ", " + bt2.getDesc2());
                    DoConnected(bt2);
                }
            }
        }
    };

    private final BroadcastReceiver PowerEnterReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!connecting) {
                connecting = true;

                btDevice bt2 = DB.getBTD("4");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    connecting = false;
                    Log.i(LOG_TAG, "Unknown device received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Broadcast received: " + bt2.getDesc1() + ", " + bt2.getDesc2());
                    DoConnected(bt2);
                }
            }
        }
    };


    protected void DoConnected(btDevice bt2) {
        boolean done = false;

        int l = 0;

        for (int k = 0; k < btdConn.length; k++) {
            if (btdConn[k] != null)
                if (bt2.getMac().equalsIgnoreCase(btdConn[k].getMac())) {
                    l = k;
                    done = true;
                }
        }

        if (!done) {
            do {
                if (btdConn[l] == null) {
                    btdConn[l] = bt2;
                    done = true;
                }
                l++;
                if (l >= btdConn.length)
                    done = true;
            } while (!done);
        }
        getConnects();
        if (connects <= 1) {
            //getOldvol();
            getOldPvol();
            oldwifistate = wifiManager.isWifiEnabled();
        }

        connectedIcon = checkIcon(bt2.getIcon());
        MessageStream = bt2.getSmsstream();
        vol_delay = bt2.getVoldelay() * 1000;
        Message_delay = bt2.getSmsdelay() * 1000;
        ramp_vol = bt2.isVolramp();

        if (bt2.wifi) {
            try {
                oldwifistate = wifiManager.isWifiEnabled();
                dowifi(false);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error " + e.getMessage());
            }
        }

        // connect the selected BT device
        if (bt2.getBdevice() != null && bt2.getBdevice().length() == 17) {
            DeviceToConnect = bt2.getBdevice();
            getIBluetoothA2dp(application);
        }


        if (toasts)
            Toast.makeText(application, bt2.toString(), Toast.LENGTH_LONG)
                    .show();

        // If we defined an app to auto-start then run it on connect if not in
        // call
        // tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (bt2.hasIntent()) {
            if (tm != null) {
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE)
                    runApp(bt2);
            } else
                runApp(bt2);
        }


        if (enableGTalk && bt2.isEnableTTS()) {
            mTts = new TextToSpeech(application, listenerStarted);
            IntentFilter messageFilter = new IntentFilter(
                    "a2dp.vol.service.MESSAGE");
            if (!tmessageRegistered) {
                application.registerReceiver(tmessage, messageFilter);
                tmessageRegistered = true;
            }
            IntentFilter sco_filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            if (!ScoRegistered && am2.isBluetoothScoAvailableOffCall()) {
                application.registerReceiver(sco_change, sco_filter);
                ScoRegistered = true;
            }
        }


        String Ireload = "a2dp.Vol.main.RELOAD_LIST";
        Intent itent = new Intent();
        itent.setAction(Ireload);
        itent.putExtra("connect", bt2.getMac());
        application.sendBroadcast(itent);
        connecting = false;

        application.sendBroadcast(new Intent("a2dp.Vol.Clear"));

        if (bt2.isSetpv()) {
            final int vol1 = bt2.getPhonev();
            new CountDownTimer(vol_delay + 500, vol_delay + 500) {

                @Override
                public void onFinish() {
                    setPVolume(vol1);
                }

                @Override
                public void onTick(long arg0) {
                    // TODO Auto-generated method stub

                }
            }.start();
        }

        if (bt2.isSilent())
            try {
                if (ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.e(LOG_TAG, "Lacking notification policy access permissions to set silent");
                    return;
                } else {
                    am2.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                    Log.i(LOG_TAG, "Set silent");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.e(LOG_TAG, "cannot set silent " + e.getLocalizedMessage());
            }


        if (bt2.isSetV()) {
            final int vol = bt2.getDefVol();
            new CountDownTimer(vol_delay, vol_delay) {

                @Override
                public void onFinish() {
                    setVolume(vol);
                }

                @Override
                public void onTick(long arg0) {
                    // TODO Auto-generated method stub

                }
            }.start();
        }

        if (bt2.isCarmode()) {
            set_car_mode(true);
        }

        updateNot();
    }

    // Device disconnected
    private final BroadcastReceiver mReceiver2 = new BroadcastReceiver() {
        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent) Triggered on bluetooth disconnect
         */
        @Override
        public void onReceive(Context context2, Intent intent2) {
            btDevice bt2 = null;
            if (!disconnecting && !connecting) {
                disconnecting = true;

                BluetoothDevice bt;
                try {
                    bt = (BluetoothDevice) Objects.requireNonNull(intent2.getExtras()).get(
                            BluetoothDevice.EXTRA_DEVICE);
                } catch (Exception e1) {
                    bt = null;
                    e1.printStackTrace();
                    disconnecting = false;
                    return;
                }

                if (bt != null) {
                    try {
                        String addres = bt.getAddress();
                        bt2 = DB.getBTD(addres);
                    } catch (Exception e) {
                        bt2 = null;
                        Log.e(LOG_TAG, "Error" + e.toString());
                        disconnecting = false;
                        return;
                    }
                } else
                    try {
                        if (Objects.requireNonNull(intent2.getAction()).equalsIgnoreCase(
                                "android.app.action.EXIT_CAR_MODE"))
                            bt2 = DB.getBTD("1");
                        else if (intent2.getAction().equalsIgnoreCase(
                                "android.app.action.EXIT_DESK_MODE"))
                            bt2 = DB.getBTD("2");
                        else if (intent2.getAction().equalsIgnoreCase(
                                Intent.ACTION_POWER_DISCONNECTED))
                            bt2 = DB.getBTD("4");
                        else
                            bt2 = null;
                    } catch (Exception e) {
                        bt2 = null;
                        Log.e(LOG_TAG, e.toString());
                    }
                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    disconnecting = false;
                    Log.i(LOG_TAG, "Unknown device disconnect received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Disconnected: " + bt2.getDesc1() + "," + bt2.getDesc2());
                    DoDisconnected(bt2);
                }
            }
        }
    };

    // Device disconnected
    private final BroadcastReceiver CarExitReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context2, Intent intent2) {
            btDevice bt2 = null;
            if (!disconnecting) {
                disconnecting = true;
                bt2 = DB.getBTD("1");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    disconnecting = false;
                    Log.i(LOG_TAG, "Unknown device disconnect received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Disconnected: " + bt2.getDesc1() + "," + bt2.getDesc2());
                    DoDisconnected(bt2);
                }
            }
        }
    };

    // Device disconnected
    private final BroadcastReceiver HomeExitReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context2, Intent intent2) {
            btDevice bt2 = null;
            if (!disconnecting) {
                disconnecting = true;
                bt2 = DB.getBTD("2");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    disconnecting = false;
                    Log.i(LOG_TAG, "Unknown device disconnect received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Disconnected: " + bt2.getDesc1() + "," + bt2.getDesc2());
                    DoDisconnected(bt2);
                }
            }
        }
    };

    // Device disconnected
    private final BroadcastReceiver PowerExitReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context2, Intent intent2) {
            btDevice bt2 = null;
            if (!disconnecting) {
                disconnecting = true;
                bt2 = DB.getBTD("4");

                // if it is none of the devices in the database, exit here
                if (bt2 == null || bt2.getMac() == null) {
                    disconnecting = false;
                    Log.i(LOG_TAG, "Unknown device disconnect received, ignoring");
                } else {
                    Log.i(LOG_TAG, "Disconnected: " + bt2.getDesc1() + "," + bt2.getDesc2());
                    DoDisconnected(bt2);
                }
            }
        }
    };


    protected void DoDisconnected(btDevice bt2) {

        int SavVol = am2.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (bt2.hasIntent()) {

            // if we opened a package for this device, try to close it now
            if (bt2.getPname().length() > 3 && bt2.isAppkill()) {
                // also open the home screen to make music app revert to
                // background
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                // now we can kill the app is asked to

                final String kpackage = bt2.getPname();
                CountDownTimer killTimer = new CountDownTimer(3000, 3000) {
                    @Override
                    public void onFinish() {

                        try {
                            stopApp(kpackage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(LOG_TAG, "Error " + e.getMessage());
                        }

                    }

                    @Override
                    public void onTick(long arg0) {

                        try {
                            stopApp(kpackage);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(LOG_TAG, "Error " + e.getMessage());
                        }
                    }
                };
                killTimer.start();

            }
        }

        // start the location capture service
        if (bt2 != null && bt2.isGetLoc() && !isMyServiceRunning(StoreLoc.class)) {
            Intent dolock = new Intent(a2dp.Vol.service.this, StoreLoc.class);
            dolock.putExtra("device", bt2.getMac());
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    startService(dolock);
                } else {
                    startForegroundService(dolock);
                }

                //update the notification when done storing. I could do this better by passing intent
                // from storloc
                //Update notification 2s after location capture done
                Long timeout = Long.valueOf(preferences.getString("gpsTime", "15000")) + 2000;
                CountDownTimer not_update = new CountDownTimer(timeout, 10000) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        notificationManagerCompat.cancel(3);
                        updateNot();
                    }
                };

                not_update.start();

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (bt2.wifi) {
            dowifi(oldwifistate);
        }

        // Remove disconnected device from connected devices array
        if (bt2 != null)
            for (int k = 0; k < btdConn.length; k++)
                if (btdConn[k] != null)
                    if (bt2.getMac().equalsIgnoreCase(btdConn[k].getMac()))
                        btdConn[k] = null;

        getConnects();

        if ((bt2 != null && bt2.isSetV()))
            if (mvolsLeft < 1)
                new CountDownTimer(3000, 3000) {

                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        setVolume(OldVol2);
                    }
                }.start();

        if ((bt2 != null && bt2.isSetpv()) || bt2 == null)
            if (pvolsLeft < 1)
                setPVolume(OldVol);

        if (notify && (bt2.mac != null)) {
            updateNot();
        }

        if (mTtsReady && ((bt2.isEnableTTS() || enableGTalk) || connects < 1)) {
            try {
                if (!clearedTts) {
                    clearTts();
                }
            } catch (Exception e) {
                // Toast.makeText(application, e.getMessage(),
                // Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            if (ScoRegistered && sco_change != null) {
                try {
                    application.unregisterReceiver(sco_change);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ScoRegistered = false;
            }
        }

        if (tmessageRegistered) {
            try {
                application.unregisterReceiver(tmessage);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            tmessageRegistered = false;
        }

        if (bt2.isSilent())
            try {
                if (ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.e(LOG_TAG, "Lacking notification policy access permissions to clear silent");
                    return;
                } else {
                    am2.setStreamVolume(AudioManager.STREAM_NOTIFICATION, Oldsilent, 0);
                    Log.i(LOG_TAG, "Clear silent to " + Oldsilent);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.e(LOG_TAG, "Cannot clear silent " + e.getLocalizedMessage());
            }


        if (bt2.getBdevice() != null && bt2.getBdevice().length() == 17) {
            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
            if (mBTA != null) {
                if (mBTA.isEnabled() && bluetoothWasOff) {
                    // If Bluetooth was off turn it back off
                    mBTA.disable();
                }
                // unbind the service used to connect the device
                doUnbind();
            }
        }

        if (bt2.isAutovol()) {
            bt2.setDefVol(SavVol);
            DB.update(bt2);
        }
        if (bt2.isCarmode()) {
            set_car_mode(false);
        }

        final String Ireload = "a2dp.Vol.main.RELOAD_LIST";
        Intent itent = new Intent();
        itent.setAction(Ireload);
        itent.putExtra("disconnect", bt2.getMac());
        application.sendBroadcast(itent);


        disconnecting = false;
    }

    // makes the media volume adjustment
    public static void setVolume(int inputVol) {

        if (am2.isVolumeFixed())
            Toast.makeText(application, "Volume fixed", Toast.LENGTH_LONG).show();

        int curvol = am2.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (inputVol < 0)
            inputVol = 0;
        if (inputVol > am2.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            inputVol = am2.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (ramp_vol && (inputVol > curvol)) {
            final int minputVol = inputVol;

            new CountDownTimer(((inputVol - curvol) * 1000), 1000) {

                @Override
                public void onFinish() {
                    int ui = 0;
                    if (hideVolUi) ui = 0;
                    else ui = AudioManager.FLAG_SHOW_UI;
                    try {
                        am2.setStreamVolume(AudioManager.STREAM_MUSIC, minputVol,
                                ui);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to set volume " + e.getLocalizedMessage());
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            NotificationManager mNotificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
                            if (!Objects.requireNonNull(mNotificationManager).isNotificationPolicyAccessGranted()) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                application.startActivity(intent);
                            }
                        }
                        e.printStackTrace();
                    }
                }

                @Override
                public void onTick(long millisUntilFinished) {
                    int ui = 0;
                    if (hideVolUi) ui = 0;
                    else ui = AudioManager.FLAG_SHOW_UI;
                    int cvol = am2.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int newvol = cvol;
                    if ((cvol + 1) < minputVol)
                        ++newvol;
                    try {
                        am2.setStreamVolume(AudioManager.STREAM_MUSIC, newvol,
                                ui);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Failed to set volume " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }

            }.start();
        } else {
            int ui = 0;
            if (hideVolUi) ui = 0;
            else ui = AudioManager.FLAG_SHOW_UI;
            try {
                am2.setStreamVolume(AudioManager.STREAM_MUSIC, inputVol,
                        ui);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to set volume " + e.getLocalizedMessage());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    NotificationManager mNotificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (!Objects.requireNonNull(mNotificationManager).isNotificationPolicyAccessGranted()) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        application.startActivity(intent);
                    }
                }
                e.printStackTrace();
            }
        }
        Log.i(LOG_TAG, "Media volume adjusted to " + inputVol);
    }

    // captures the media volume so it can be later restored
    private void getOldvol() {
        if (mvolsLeft <= 1) {
            OldVol2 = am2.getStreamVolume(AudioManager.STREAM_MUSIC);
            // Store the old volume in preferences so it can be extracted if another
            // instance starts or the service is killed and restarted
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(OLD_VOLUME, OldVol2);
            editor.apply();
            Log.i(LOG_TAG, "Captured media volume " + OldVol2);
        }
    }

    // captures the phone volume so it can be later restored
    private void getOldPvol() {
        if (pvolsLeft <= 1) {
            OldVol = am2.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            Oldsilent = am2.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            // Store the old volume in preferences so it can be extracted if another
            // instance starts or the service is killed and restarted
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(OLD_PH_VOL, OldVol);
            editor.putInt("oldsilent", Oldsilent);
            editor.apply();
            Log.i(LOG_TAG, "Captured phone volume " + OldVol);
        }
    }

    // makes the phone volume adjustment
    public static int setPVolume(int inputVol) {
        int outVol;
        if (inputVol < 0)
            inputVol = 0;
        if (inputVol > am2.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL))
            inputVol = am2.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        if (hideVolUi) {
            try {
                am2.setStreamVolume(AudioManager.STREAM_VOICE_CALL, inputVol,
                        AudioManager.FLAG_SHOW_UI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                am2.setStreamVolume(AudioManager.STREAM_VOICE_CALL, inputVol, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        outVol = am2.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        Log.i(LOG_TAG, "Phone volume set to " + outVol);
        return outVol;
    }

    private void createNotificationChannel() {
        mNotificationManager = getSystemService(NotificationManager.class);
        notificationManagerCompat = NotificationManagerCompat.from(application);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // set up background notification channel
            CharSequence name = getString(R.string.background_channel_name);
            String description = getString(R.string.background_channel_description);
            int importance = NotificationManager.IMPORTANCE_MIN;
            channel_b = new NotificationChannel(A2DP_BACKGROUND, name, importance);
            channel_b.setDescription(description);
            //channel_b.setImportance(NotificationManager.IMPORTANCE_MIN);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(channel_b);

            // set up foreground notification channel
            CharSequence name2 = getString(R.string.foreground_channel_name);
            String description2 = getString(R.string.foreground_channel_description);
            int importance2 = NotificationManager.IMPORTANCE_LOW;
            channel_f = new NotificationChannel(A2DP_FOREGROUND, name2, importance2);
            channel_f.setDescription(description2);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(channel_f);

        }
    }

    // make sure icon is valid
    private int checkIcon(int icon) {
        ArrayList<Integer> icons = new ArrayList<>();
        icons.add(R.drawable.car2);
        icons.add(R.drawable.headset);
        icons.add(R.drawable.ic_launcher);
        icons.add(R.drawable.icon5);
        icons.add(R.drawable.usb);
        icons.add(R.drawable.jack);

        if (icons.contains(icon)) {
            return icon;
        } else {
            Log.e(LOG_TAG, "Invalid icon use attempted, changing to default");
            return R.drawable.ic_launcher;
        }
    }

    public void updateNot() {

        if ((channel_b == null) || (channel_f == null)) {
            createNotificationChannel();
        }

        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        } else {
            createNotificationChannel();
        }
        if (notificationManagerCompat != null) {
            notificationManagerCompat.cancelAll();
        } else {
            createNotificationChannel();
        }

        // show all connected devices in notification
        boolean connect = false;
        String temp;   // name of connected devices to display
        if (connects > 0) {
            temp = getResources().getString(R.string.connectedTo);
            for (int k = 0; k < btdConn.length; k++) {
                if (btdConn[k] != null && btdConn[k].mac != null) {
                    connect = true;
                    if (k > 0) temp += ",";
                    temp += " " + btdConn[k].toString();
                }
            }

        } else
            temp = getResources().getString(R.string.ServRunning);

        Log.i(LOG_TAG, "UpdateNot: " + temp);

        // useful feedback in background notification
        String str; // String to use if there are no connected devices
        if (mTtsReady) {
            str = getResources().getString(R.string.TTSready);
        } else {
            str = "";
        }

        Notification not = null;
        if (connect) {
            if (mNotificationManager != null) mNotificationManager.cancel(1);
            if (notificationManagerCompat != null) {
                notificationManagerCompat.cancelAll();
            }

            Intent notificationIntent = new Intent(this, main.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                not = new NotificationCompat.Builder(application, A2DP_FOREGROUND)
                        .setContentTitle(
                                getResources().getString(R.string.connectedTo))
                        .setContentIntent(contentIntent)
                        .setSmallIcon(checkIcon(connectedIcon))
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setContentText(temp)
                        .setChannelId(A2DP_FOREGROUND).build();
                notificationManagerCompat.notify(1, not);
                //Toast.makeText(application, "Test on " + car + " " +not.getChannelId(), Toast.LENGTH_LONG).show();
            } else {
                not = new NotificationCompat.Builder(application, LAUNCHER_APPS_SERVICE)
                        .setContentTitle(
                                getResources().getString(R.string.connectedTo))
                        .setContentIntent(contentIntent)
                        .setSmallIcon(checkIcon(connectedIcon))
                        .setContentText(temp)
                        .setPriority(Notification.PRIORITY_LOW)
                        .build();
                notificationManagerCompat.notify(1, not);
            }

            this.startForeground(1, not);
            //Toast.makeText(application, "Test on " + car, Toast.LENGTH_LONG).show();
        } else {
            if (mNotificationManager != null) mNotificationManager.cancel(1);

            if (notify_pref.equalsIgnoreCase("always") || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                Intent notificationIntent = new Intent(this, main.class);
                PendingIntent contentIntent = PendingIntent.getActivity(
                        this, 0, notificationIntent, 0);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    not = new NotificationCompat.Builder(application, A2DP_BACKGROUND)
                            .setContentTitle(
                                    getResources().getString(R.string.ServRunning))
                            .setContentIntent(contentIntent)
                            .setSmallIcon(ic_launcher)
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .setContentText(str)
                            .setChannelId(A2DP_BACKGROUND).build();
                    notificationManagerCompat.notify(1, not);
                    //Toast.makeText(application, "Test off " + car + " " +not.getChannelId(), Toast.LENGTH_LONG).show();
                } else {
                    not = new NotificationCompat.Builder(application, LAUNCHER_APPS_SERVICE)
                            .setContentTitle(
                                    getResources().getString(R.string.ServRunning))
                            .setContentIntent(contentIntent)
                            .setSmallIcon(ic_launcher)
                            .setContentText(str)
                            .setPriority(Notification.PRIORITY_MIN)
                            .build();
                    notificationManagerCompat.notify(1, not);
                }


                this.startForeground(1, not);

            }
        }
        UpdateConnectWidgets();
    }

    public void UpdateConnectWidgets() {
        Intent intent = new Intent(application, ConnectWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        SharedPreferences prefs = application.getSharedPreferences(CON_PREFS_NAME, 0);

        AppWidgetManager awm = AppWidgetManager.getInstance(getApplication());
        RemoteViews views = new RemoteViews(application.getPackageName(),
                R.layout.connect_widget);
        int[] ids = awm.getAppWidgetIds(new ComponentName(getApplication(), ConnectWidget.class));

        if (ids.length > 0)
            for (int id : ids) {
                String bt_mac = prefs.getString(PREF_PREFIX_KEY + Integer.toString(id), "O");

                if (bt_mac != null)
                    if (bt_mac.length() == 17) {
                        //bt_mac = bt_mac_pref.substring(bt_mac_pref.length() - 17);

                        boolean connected = false;
                        if (btdConn != null)
                            for (btDevice device : btdConn) {
                                String mac = "";
                                if (device != null) mac = device.getMac();
                                if (mac.length() == 17 && bt_mac.equalsIgnoreCase(device.mac))
                                    connected = true;
                            }
                        if (connected) {
                            views.setInt(R.id.WidgetButton, "setBackgroundResource", R.drawable.icon);

                        } else {
                            views.setInt(R.id.WidgetButton, "setBackgroundResource", R.drawable.icon2);

                        }
                        awm.updateAppWidget(id, views);
                    }
            }

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private boolean runApp(btDevice bt) {

        Intent i;
        String pname = bt.getPname();
        String cAction = bt.getAppaction();
        String cData = bt.getAppdata();
        String cType = bt.getApptype();
        boolean restart = bt.isApprestart();

        if (restart && pname != null && pname.length() > 3) {
            try {
                ActivityManager act1 = (ActivityManager) this
                        .getSystemService(ACTIVITY_SERVICE);
                Objects.requireNonNull(act1).killBackgroundProcesses(pname);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pname == null || pname.equals("")) {
            return false;
        } else if (cData.length() > 1) {
            try {
                //i = Intent.getIntent(cData);
                i = Intent.parseUri(cData, 0);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return false;
            }
        } else if (!cAction.equals("")) {
            i = new Intent();
            i.setAction(cAction);
            if (!cData.equals("")) {
                i.setData(Uri.parse(cData));
            }
            if (!cType.equals("")) {
                i.setType(cType);
            }
        } else {
            try {
                i = mPackageManager.getLaunchIntentForPackage(pname);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            Objects.requireNonNull(i).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // add extra for referrer used for apps like Spotify
        try {
            i.putExtra(Intent.EXTRA_REFERRER,
                    Uri.parse("android-app://" + application.getPackageName()));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Cannot add app referrer extra to intent " + e.getLocalizedMessage());
        }

        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            Toast t = Toast.makeText(getApplicationContext(),
                    R.string.app_not_found, Toast.LENGTH_SHORT);
            if (toasts)
                t.show();
            e.printStackTrace();
            Log.e(LOG_TAG, "App start failed " + e.getLocalizedMessage());
            return false;
        }

    }

    protected void stopApp(String packageName) {
        Intent mIntent = getPackageManager().getLaunchIntentForPackage(
                packageName);
        if (mIntent != null) {
            try {

                ActivityManager act1 = (ActivityManager) this
                        .getSystemService(ACTIVITY_SERVICE);
                Objects.requireNonNull(act1).killBackgroundProcesses(packageName);
                List<ActivityManager.RunningAppProcessInfo> processes;
                processes = act1.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : processes) {
                    for (int i = 0; i < info.pkgList.length; i++) {
                        if (info.pkgList[i].contains(packageName)) {
                            android.os.Process.killProcess(info.pid);
                        }
                    }
                }
            } catch (ActivityNotFoundException err) {
                err.printStackTrace();
                Toast t = Toast.makeText(getApplicationContext(),
                        R.string.app_not_found, Toast.LENGTH_SHORT);
                if (toasts)
                    t.show();
            }

        }
    }


    public void getIBluetoothA2dp(Context context) {

        Intent i = new Intent(IBluetoothA2dp.class.getName());

        String filter;
        filter = getPackageManager().resolveService(i, PackageManager.GET_RESOLVED_FILTER).serviceInfo.packageName;
        i.setPackage(filter);

        if (context.bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
            //Toast.makeText(context, "started service connection", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Bluetooth start service connection failed", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Could not bind to Bluetooth A2DP Service");
        }

    }

    public static ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mIsBound = true;
            ibta2 = IBluetoothA2dp.Stub.asInterface(service);
            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> pairedDevices = mBTA.getBondedDevices();
            BluetoothDevice device = null;
            for (BluetoothDevice dev : pairedDevices) {
                if (dev.getAddress().equalsIgnoreCase(DeviceToConnect))
                    device = dev;
            }
            if (device != null)
                try {
                    ibta2.connect(device);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Error connecting Bluetooth device " + e.getLocalizedMessage());
                }


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            doUnbind();
        }
    };

    static void doUnbind() {
        if (mIsBound) {
            try {
                application.unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // disable wifi is requested
    private void dowifi(boolean s) {
        try {
            wifiManager.setWifiEnabled(s);
        } catch (Exception e) {
            Toast.makeText(application,
                    "Unable to switch wifi: " + e.toString(), Toast.LENGTH_LONG)
                    .show();
            Log.e(LOG_TAG, "Enable to switch WiFi");
            e.printStackTrace();
        }
    }

    // Update the connected devices array
    private void getConnects() {
        connects = 0;
        mvolsLeft = 0;
        pvolsLeft = 0;
        for (int i = 0; i < btdConn.length; i++) {
            if (btdConn[i] != null) {
                connects++;
                if (btdConn[i].isSetV())
                    ++mvolsLeft;
                if (btdConn[i].isSetpv())
                    ++pvolsLeft;
            }
        }
        Log.i(LOG_TAG, "getConnects " + connects);
        if (connects < 1 && connecting && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            getOldvol();
        }
    }

    private AtomicInteger numToRead = new AtomicInteger(0);

    @TargetApi(Build.VERSION_CODES.O)
    public void TextReader(String rawinput) {

        boolean repeat = false;
        boolean mgood = false;

        if (rawinput == null) {
            Toast.makeText(application, "No input", Toast.LENGTH_LONG).show();
        } else {
            mgood = true;
        }

        // created to eliminate double reading same message
        if (LastMessage == null || LastMessage.isEmpty()) {
            LastMessage = rawinput;
        } else {
            if (rawinput.equals(LastMessage)) {
                Log.i(LOG_TAG, "repeat message received in message reader");
                repeat = true;
            }
            LastMessage = rawinput;
        }

        if (mTtsReady && !repeat && mgood) {
            myHash = new HashMap<>();

            // so the reader explains its a URL rather than spelling out http://
            String input = rawinput.replaceAll("http://", ", URL, ");

            myHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, A2DP_Vol);

            // trim off very long strings
            if (input.length() > MAX_MESSAGE_LENGTH) {
                input = input.substring(0, MAX_MESSAGE_LENGTH);
                input += " , , , message truncated";
            }

            musicWasPlaying = am2.isMusicActive();
            final Bundle b1 = new Bundle();

            switch (MessageStream) {
                case IN_CALL_STREAM:


                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        am2.requestAudioFocus(service.this,
                                AudioManager.STREAM_VOICE_CALL,
                                AudioManager.AUDIOFOCUS_GAIN);
                    } else {
                        AudioAttributes aa = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build();

                        afr = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                .setAudioAttributes(aa)
                                .build();
                        am2.requestAudioFocus(afr);
                    }
                    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                            String.valueOf(AudioManager.STREAM_VOICE_CALL));
                    b1.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
                    clearedTts = false;

                    if (am2.isBluetoothScoAvailableOffCall()) {
                        am2.startBluetoothSco();
                        am2.setSpeakerphoneOn(true);
                    }

                    break;

                case MUSIC_STREAM:
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        am2.requestAudioFocus(service.this,
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                    } else {
                        AudioAttributes aa = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();

                        afr = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(aa).build();
                        am2.requestAudioFocus(afr);
                    }
                   /* am2.requestAudioFocus(a2dp.Vol.service.this,
                            AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);*/
                    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                            String.valueOf(AudioManager.STREAM_MUSIC));
                    b1.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
                    clearedTts = false;
                    break;
                case ALARM_STREAM:
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                        am2.requestAudioFocus(service.this,
                                AudioManager.STREAM_ALARM,
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    } else {
                        AudioAttributes aa = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setLegacyStreamType(AudioManager.STREAM_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();

                        afr = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(aa).build();
                        am2.requestAudioFocus(afr);
                    }

                    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                            String.valueOf(AudioManager.STREAM_ALARM));
                    b1.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);

                    clearedTts = false;
                    break;
            }

            final String str = input;
            if (toasts)
                Toast.makeText(application, str, Toast.LENGTH_LONG).show();

            if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {

                new CountDownTimer(Message_delay, Message_delay / 2) {

                    @Override
                    public void onFinish() {
                        try {

                            numToRead.incrementAndGet();
                            String utt_id = A2DP_Vol; // + numToRead.toString();
                            Log.i(LOG_TAG, "Message reader numToRead = " + numToRead);

                            mTts.speak(str, TextToSpeech.QUEUE_ADD, b1, utt_id);
                        } catch (Exception e) {
                            Toast.makeText(application, R.string.TTSNotReady,
                                    Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                            Log.e(LOG_TAG, "TTS Not ready " + e.getLocalizedMessage());
                            numToRead.set(0);
                        }
                    }

                    @Override
                    public void onTick(long arg0) {

                    }

                }.start();
            } else {
                clearTts();
            }

        }
    }

    private void MusicCommand(int command) {
        // this works on most players like Spotify
        KeyEvent downevent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
        am2.dispatchMediaKeyEvent(downevent);
        KeyEvent upevent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY);
        am2.dispatchMediaKeyEvent(upevent);
        // play in the case where the above does not work
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "play");
        sendBroadcast(i);
        Log.i(LOG_TAG, "Music resumed");
    }

    public TextToSpeech.OnInitListener listenerStarted = new TextToSpeech.OnInitListener() {
        // TTS engine now running so start the message receivers

        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                mTtsReady = true;
                mTts.setOnUtteranceProgressListener(ul);
                Log.i(LOG_TAG, "TTS listener started");
            }
        }
    };

    public android.speech.tts.UtteranceProgressListener ul = new UtteranceProgressListener() {

        @Override
        public void onDone(String uttId) {

            int result = AudioManager.AUDIOFOCUS_REQUEST_FAILED;
            if (A2DP_Vol.equalsIgnoreCase(uttId)) {
                // unmute the stream

                Log.d("service", "utterance onDone numToRead = " + numToRead.get());
                int countRemaining = numToRead.decrementAndGet();
                if (countRemaining > 0) {
                    Log.i(LOG_TAG, "still more messages to read?");
                    return;
                } else {
                    Log.i(LOG_TAG, "all done - abandon audio focus");

                }

                switch (MessageStream) {
                    case IN_CALL_STREAM:

                        if (!clearedTts && numToRead.intValue() < 1) {
                            // clearTts();
                            Intent c = new Intent();
                            c.setAction("a2dp.vol.service.CLEAR");
                            application.sendBroadcast(c);
                            clearTts();
                        }
                        result = am2.abandonAudioFocus(service.this);
                        if (musicWasPlaying) {
                            // now toggle pause to resume
                            MusicCommand(1);
                        }
                        break;
                    case MUSIC_STREAM:
                        result = am2.abandonAudioFocus(service.this);
                        break;
                    case ALARM_STREAM:
                        if (!clearedTts && numToRead.intValue() < 1) {
                            // clearTts();
                            Intent c = new Intent();
                            c.setAction("a2dp.vol.service.CLEAR");
                            application.sendBroadcast(c);
                            clearTts();
                        }

                        result = am2.abandonAudioFocus(service.this);
                        break;
                }

                if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    result = am2.abandonAudioFocus(service.this);
                }
                am2.setMode(AudioManager.MODE_NORMAL);
            }
            if (FIX_STREAM.equalsIgnoreCase(uttId)) {
                result = am2.abandonAudioFocus(service.this);
            }

            if (numToRead.intValue() < 1) {
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    am2.abandonAudioFocusRequest(afr);
                }

                am2.setMode(AudioManager.MODE_NORMAL);
            }
        }

        @Override
        public void onError(String utteranceId) {
            Log.d("service", "onError numToRead = " + numToRead.get());
            numToRead.decrementAndGet();
            Log.d("service", "numToRead = " + numToRead.get());
        }

        @Override
        public void onStart(String utteranceId) { // TODO

        }

    };


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:

                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (musicWasPlaying && MessageStream == IN_CALL_STREAM) {
                    // now toggle pause to resume
                    MusicCommand(1);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:


                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // ... pausing or ducking depends on your app
                if (musicWasPlaying && MessageStream == IN_CALL_STREAM) {
                    // now toggle pause to resume
                    MusicCommand(1);
                }
                break;
        }
    }


    private void clearTts() {

        if (am2.isBluetoothScoAvailableOffCall()) {
            am2.stopBluetoothSco();
            am2.setBluetoothScoOn(false);
            am2.setSpeakerphoneOn(false);
        }

    }


    public BroadcastReceiver sco_change = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            int state = arg1.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0);

            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED && Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && arg0 == service.this) {
                if (afr == null) {
                    AudioAttributes aa = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build();

                    afr = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(aa)
                            .build();
                }
                am2.requestAudioFocus(afr);
            }

            if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED && !clearedTts && arg0 == service.this) {
                if (!mTtsReady)
                    mTts = new TextToSpeech(application, listenerStarted);

                HashMap<String, String> myHash2 = new HashMap<>();

                myHash2.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FIX_STREAM);
                am2.requestAudioFocus(service.this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                myHash2.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                        String.valueOf(AudioManager.STREAM_MUSIC));


                if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                    am2.abandonAudioFocus(service.this);
                } else {

                    am2.abandonAudioFocusRequest(afr);
                }

                am2.setMode(AudioManager.MODE_NORMAL);

/*                if (musicWasPlaying) {
                    MusicCommand(1);

                }*/
                clearedTts = true;
            }
        }

    };

    private void set_car_mode(boolean mode) {
        try {
            UiModeManager mm = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
            if (mode)
                Objects.requireNonNull(mm).enableCarMode(UiModeManager.ENABLE_CAR_MODE_GO_CAR_HOME);
            else
                Objects.requireNonNull(mm).disableCarMode(0);

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
