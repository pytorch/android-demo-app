package org.pytorch.demo.vision;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.media.FaceDetector;
import android.os.Bundle;
import android.widget.Toast;

import com.serenegiant.usb.USBMonitor;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.FaceDetectionActivity;
import org.pytorch.demo.GlassLocalActivity;
import org.pytorch.demo.GlassRemoteActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;
import org.pytorch.demo.RemoteFaceDetectActivity;

import static android.hardware.usb.UsbConstants.USB_CLASS_MISC;
import static android.hardware.usb.UsbConstants.USB_CLASS_STILL_IMAGE;
import static android.hardware.usb.UsbConstants.USB_CLASS_VIDEO;

public class GlassListActivity extends AbstractListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO ask usb camera permissions here

        usbMonitor = new USBMonitor(this, onDeviceConnectListener);
        usbMonitor.register();


        findViewById(R.id.vision_card_face_recognition_glass_local_click_area).setOnClickListener(v -> {
            if(usbMonitor.hasPermission(USBDevice)){
                final Intent intent = new Intent(GlassListActivity.this, GlassLocalActivity.class);
                startActivity(intent);
            }else{
                Toast.makeText(GlassListActivity.this, "没有usb相机的权限！", Toast.LENGTH_SHORT).show();
                GetPermission();
            }

        });

        findViewById(R.id.vision_card_face_recognition_glass_remote_click_area).setOnClickListener(v -> {
            if(usbMonitor.hasPermission(USBDevice)){
                final Intent intent = new Intent(GlassListActivity.this, GlassRemoteActivity.class);
                startActivity(intent);
            }else{
                Toast.makeText(GlassListActivity.this, "没有usb相机的权限！", Toast.LENGTH_SHORT).show();
                GetPermission();
            }

        });
    }

    @Override
    protected int getListContentLayoutRes() {
        return R.layout.glass_list_content;
    }


    private boolean GetPermission(){
        if(USBDevice != null){
            Toast.makeText(GlassListActivity.this, "试图获取"+USBDevice.getDeviceName()+"的权限！", Toast.LENGTH_LONG).show();
            return usbMonitor.requestPermission(USBDevice);
        }
        else{
            Toast.makeText(GlassListActivity.this, "没有检测到usb相机！", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    private USBMonitor usbMonitor;
    private UsbDevice USBDevice;
    private final USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice usbDevice) {
            Toast.makeText(GlassListActivity.this, "usb设备已接入"+usbDevice.getDeviceClass(), Toast.LENGTH_SHORT).show();

            if(usbDevice.getDeviceClass() == USB_CLASS_MISC){
                USBDevice = usbDevice;
                usbMonitor.requestPermission(usbDevice);
            }


        }

        @Override
        public void onDettach(UsbDevice usbDevice) {
            Toast.makeText(GlassListActivity.this, "usb设备已移除", Toast.LENGTH_SHORT).show();
            USBDevice = null;
        }

        @Override
        public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
            Toast.makeText(GlassListActivity.this, "usb设备完成连接", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
            Toast.makeText(GlassListActivity.this, "usb设备断开连接", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(UsbDevice usbDevice) {
            Toast.makeText(GlassListActivity.this, "usb设备取消", Toast.LENGTH_SHORT).show();
        }
    };


}
