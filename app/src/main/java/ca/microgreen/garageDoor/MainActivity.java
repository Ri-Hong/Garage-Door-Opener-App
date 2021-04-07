package ca.microgreen.garageDoor;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.util.Log;
import android.os.CountDownTimer;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {


    public static final int ACTION_STATUS_INIT = 0;
    public static final int ACTION_STATUS_READY = 1;
    public static final int ACTION_STATUS_GO = 2;

    MqttAndroidClient mqttAndroidClient;
    String mqttAndroidClientId = "";


    private Button btnOpenClose1, btnOpenClose2;
    private TextView tvAck, tvHeartbeat, tvRequest;
    private TextView tvInstruction1;
    private TextView tvClock, tvIpAddress;

    CountDownTimer clockTimer = null;
    CountDownTimer actionTimer = null;

    private int actionStatus = ACTION_STATUS_INIT;
    private int debugN = 0;
    String topic = ""; //FILL IN YOUR OWN MQTT TOPIC. Should match the one you set for the ESP01 module

    //------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setWidgetId();
        setTrace("OnCreate");

        btnOpenClose1.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if( actionStatus==ACTION_STATUS_INIT) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            Button view = (Button) v;
                            view.getBackground().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                            v.invalidate();
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            actionStatus = ACTION_STATUS_READY;
                            actionTimerStart( 2000 );  // timeout in 2 seconds.  user should click GO within this period

                        }
                        case MotionEvent.ACTION_CANCEL: {
                            Button view = (Button) v;
                            view.getBackground().clearColorFilter();
                            view.invalidate();
                            break;
                        }
                    }
                }
                else
                {
                    actionTimerStart( 2000);
                }
                return true;
            }
        });

        btnOpenClose2.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if( actionStatus==ACTION_STATUS_READY) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            Button view = (Button) v;
                            view.getBackground().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                            v.invalidate();
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            publishRequest();
                            Date date = new Date();
                            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd E hh:mm:ss a");
                            String strDate = formatter.format(date);
                            tvRequest.setText(strDate);
                            actionStatus = ACTION_STATUS_GO;
                            actionTimerStart( 3000 );  // user can click again after this period
                        }
                        case MotionEvent.ACTION_CANCEL: {
                            Button view = (Button) v;
                            view.getBackground().clearColorFilter();
                            view.invalidate();
                            break;
                        }
                    }
                }
                return true;
            }
        });
        setStatus("onCreate().  MQTT Not connected.");
        garageDoorStart();

        return;
    } // OnCreate()

    public void publishRequest() {
        setTrace("publishRequest()");
        mqttPublish("38");
    } // publishRequest()

    public void garageDoorStart() {
        setTrace( "garageDoorStart()");
        showIpAddress();
        clockTimerStart();
        mqttStart();
    } // powerpakStart()

    public void mqttStart() {
        setTrace( "mqttStart()");
        mqttSetup();
        mqttConnect();
    }

    //------------ MQTT setup
    public void mqttSetup() {
        setTrace( "mqttSetup()");
        //-------- setup the MQTT connection
        String mqttUri = "tcp://broker.hivemq.com:1883";
        mqttAndroidClientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(this.getApplicationContext(), mqttUri, mqttAndroidClientId);

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                setTrace( "connectionLost()");
                // restart the mqtt connection after some delay
                // handleMqttConnectionLost();

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                setTrace( "messageArrived()");

                showToast("rcvd!");
                String msgPayload = new String(message.getPayload());
                setTrace( msgPayload );
                handleMqttMsg(msgPayload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                setTrace( "deliveryComplete()");
            }
        });

    } // mqttSetup()

    public void mqttConnect() {
        setTrace( "mqttConnect()");
        Log.d("my MQTT", "mqttConnect()");
        try {
            IMqttToken token = mqttAndroidClient.connect();
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    setTrace( "onSucess()");
                    setSubscription();
                } // onSuccess()

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    setTrace( "onFailure()");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }  // mqttConnect()

    // ----------- MQTT ---
    // Msg to publish should be a string of Json format
    public void mqttPublish(String msgStr) {
        setTrace( "mqttPublish(): " + msgStr);
        // String message = "{'MsgType':'ping'}";
        try {
            mqttAndroidClient.publish(topic, msgStr.getBytes(), 0, false);
            showToast( "Published Message");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSubscription() {
        setTrace( "setSubscription()");
        try {

            mqttAndroidClient.subscribe(topic, 0);


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void conn(View v) {
        setTrace( "8()");
        try {
            IMqttToken token = mqttAndroidClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    showToast("connected!!");
                    setSubscription();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    showToast( "connection failed!!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void mqttDisconnect(View v) {
        setTrace( "9()");
        try {
            IMqttToken token = mqttAndroidClient.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    showToast( "Disconnected!!");


                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    showToast("Could not disconnect!!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    //--------------------


    private void setWidgetId() {
        btnOpenClose1 = (Button) findViewById(R.id.btnOpenClose1);
        btnOpenClose2 = (Button) findViewById(R.id.btnOpenClose2);

        tvAck = (TextView) findViewById(R.id.tvAck );
        tvHeartbeat = (TextView) findViewById(R.id.tvHeartbeat );
        tvRequest  = (TextView) findViewById(R.id.tvRequest );

        tvInstruction1 = (TextView) findViewById(R.id.tvInstruction1 );
        tvInstruction1.bringToFront();
        tvInstruction1.invalidate();
        btnOpenClose1.invalidate();


        tvClock = (TextView) findViewById(R.id.tvClock );
        tvIpAddress = (TextView) findViewById(R.id.tvIpAddress );

    } // setWidgetId()



    private void handleMqttMsg(String msgStr) {
        setTrace( "handleMqttMsg() ");
        String errMsg = "";

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd E hh:mm:ss a");
        String strDate = formatter.format(date);

        if (msgStr.equals("1")){

            tvHeartbeat.setText(strDate);
            tvAck.setText(strDate + "    Successed");
        }
        else if(msgStr.equals("0")){
            tvAck.setText(strDate + "    Failed");
        }
        else if(msgStr.equals("38")){
        }
        else if(msgStr.equals("heartbeat")){
            handleHeartbeat();
        }
        else{
            tvAck.setText(strDate + "    Unknown response: " + msgStr);
            Log.d("myTag", msgStr); // change tag------------
        }

    }  // handleMqttMsg()


    private void handleHeartbeat(){

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd E hh:mm:ss a");
        String strDate = formatter.format(date);

        tvHeartbeat.setText(strDate);


    }// handleHeartbeat()

    void clockTimerStart() {

        if( clockTimer == null ) {
            clockTimer = new CountDownTimer(1000, 1000) {
                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    Date date = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("MMM dd E hh:mm:ss a");
                    String strDate = formatter.format(date);

                    tvClock.setText(strDate);
                    clockTimerStart();
                }
            };
        }
        clockTimer.start();
    }


    void actionTimerStart( int timeoutPeriod ) {

        if( actionTimer == null ) {
            actionTimer = new CountDownTimer(timeoutPeriod, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                // when timeout occurs, always goes back to INIT state
                public void onFinish() {
                    actionStatus = ACTION_STATUS_INIT;
                }
            };
        }

        // cancel the existing timer and start a new one
        actionTimer.cancel();
        actionTimer.start();
    } // actionTimerStart()


    private final void setStatus(int resId) {
        setTrace( "setStatus() ");

        return;
    }

    private final void setStatus(CharSequence subTitleMsg) {
        setTrace( "setStatus() ");
        //final ActionBar actionBar = getSupportActionBar();
        //actionBar.setSubtitle(subTitleMsg);
        return;
    }


    private void setTrace( String msg){
        debugN++;
        Log.d( "mgPowerPak", String.valueOf(debugN)+": "+ msg);
        //tvMsg.append( String.valueOf(debugN)+": "+ msg+" ");
    }

    private void showIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        int ipAddress = info.getIpAddress();


        final String formattedIpAddress = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
        setTrace( formattedIpAddress);
        tvIpAddress.setText(formattedIpAddress );
    }


    @Override
    public void onStart() {
        super.onStart();
        garageDoorStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        garageDoorStart();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void showToast(String msg) {
//        Toast.makeText(getApplicationContext(),
//                Integer.toString(msgCount++) + ": MainActivity: " + msg,
//                Toast.LENGTH_SHORT).show();
        //TextView tvMsg = (TextView) findViewById(R.id.tvMsg);
        //tvMsg.setText(msg);
    }
}

