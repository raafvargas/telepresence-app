package com.app.telepresence;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Thread subscribeThread;
    ConnectionFactory factory = new ConnectionFactory();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler incomingMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");

                Log.d("", "Handler message: " + message);

                TextView tv = (TextView) findViewById(R.id.textView);
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
                tv.append(ft.format(now) + ' ' + message + '\n');
            }
        };

        subscribe(incomingMessageHandler);
    }

    private void setupFactory() {
        String uri = "amqp://dxaelhin:VnmfOgBrGhIJk2bKb6UrpPhRcKh6FtMy@reindeer.rmq.cloudamqp.com/dxaelhin";
        try {
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(10000);
            factory.setTopologyRecoveryEnabled(true);
            factory.setUri(uri);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    void subscribe(final Handler handler)
    {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                setupFactory();
                try {
                    Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel();

                    AMQP.Queue.DeclareOk q = channel.queueDeclare("robot-287aa9c8-a165-4662-8201-623dec1ab43a", true, true, false, null);
                    channel.queueBind("robot-287aa9c8-a165-4662-8201-623dec1ab43a", "287aa9c8-a165-4662-8201-623dec1ab43a", "");

                    QueueingConsumer consumer = new QueueingConsumer(channel);
                    channel.basicConsume(q.getQueue(), true, consumer);

                    channel.basicConsume("robot-287aa9c8-a165-4662-8201-623dec1ab43a", true, consumer);

                    while (true) {
                        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                        String message = new String(delivery.getBody());
                        Log.d("","[r] " + message);
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putString("msg", message);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                }
                catch (Exception e) {
                    Log.d("", "Connection broken: " + e.getClass().getName());
                    e.printStackTrace();
                    try {
                        Thread.sleep(5000); //sleep and then try again
                    }
                    catch (InterruptedException ie) {
                    }
                 }
            }
        });
        subscribeThread.start();
    }
}
