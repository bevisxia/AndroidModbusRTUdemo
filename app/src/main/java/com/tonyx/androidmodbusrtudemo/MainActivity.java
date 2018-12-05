package com.tonyx.androidmodbusrtudemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tonyx.androidmodbusrtudemo.modbus.ModbusMaster;

import java.io.File;
import java.util.Arrays;

import android_serialport_api.SerialPort;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            int[] result = new int[6];
            SerialPort serialPort = new SerialPort(new File("/dev/ttyS0"));
            ModbusMaster modbusMaster = new ModbusMaster(serialPort);
            while (true){
                result = modbusMaster.readHoldingRegisters(1,0,6);
                System.out.println("result: "+Arrays.toString(result));
                Thread.sleep(1000L);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
