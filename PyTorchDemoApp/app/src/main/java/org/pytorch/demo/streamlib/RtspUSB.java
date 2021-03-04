package org.pytorch.demo.streamlib;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.pedro.encoder.utils.CodecUtil;
import com.pedro.rtplibrary.view.LightOpenGlView;
import com.pedro.rtplibrary.view.OpenGlView;
import com.pedro.rtsp.rtsp.Protocol;
import com.pedro.rtsp.rtsp.RtspClient;
import com.pedro.rtsp.rtsp.VideoCodec;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.nio.ByteBuffer;

/**
 * More documentation see:
 * {@link com.pedro.rtplibrary.base.Camera1Base}
 *
 * Created by pedro on 10/02/17.
 */

public class RtspUSB extends USBBase {

    private RtspClient rtspClient;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtspUSB(OpenGlView openGlView, ConnectCheckerRtsp connectCheckerRtsp) {
        super(openGlView);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtspUSB(LightOpenGlView lightOpenGlView, ConnectCheckerRtsp connectCheckerRtsp) {
        super(lightOpenGlView);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtspUSB(Context context, ConnectCheckerRtsp connectCheckerRtsp) {
        super(context);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    /**
     * Internet protocol used.
     *
     * @param protocol Could be Protocol.TCP or Protocol.UDP.
     */
    public void setProtocol(Protocol protocol) {
        rtspClient.setProtocol(protocol);
    }

    public void setVideoCodec(VideoCodec videoCodec) {
        videoEncoder.setType(videoCodec == VideoCodec.H265 ? CodecUtil.H265_MIME : CodecUtil.H264_MIME);
    }

    @Override
    public void setAuthorization(String user, String password) {
        rtspClient.setAuthorization(user, password);
    }

    @Override
    protected void prepareAudioRtp(boolean isStereo, int sampleRate) {
        rtspClient.setIsStereo(isStereo);
        rtspClient.setSampleRate(sampleRate);
    }

    @Override
    protected void startStreamRtp(String url) {
        rtspClient.setUrl(url);
    }

    @Override
    protected void stopStreamRtp() {
        rtspClient.disconnect();
    }

    @Override
    protected void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
        rtspClient.sendAudio(aacBuffer, info);
    }

    @Override
    protected void onSpsPpsVpsRtp(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
        ByteBuffer newSps = sps.duplicate();
        ByteBuffer newPps = pps.duplicate();
        ByteBuffer newVps = vps != null ? vps.duplicate() : null;
        rtspClient.setSPSandPPS(newSps, newPps, newVps);
        rtspClient.connect();
    }

    @Override
    protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
        rtspClient.sendVideo(h264Buffer, info);
    }
}

