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

                    channel.queueDeclare("287aa9c8-a165-4662-8201-623dec1ab43a", true, false, false, null);

                    Consumer consumer = new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                                throws IOException {
                            String message = new String(body, "UTF-8");

                            Log.d("", "New message: " + message);

                            Message msg = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);

                            handler.sendMessage(msg);
                        }
                    };

                    channel.basicConsume("287aa9c8-a165-4662-8201-623dec1ab43a", true, consumer);
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
