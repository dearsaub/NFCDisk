package com.example.liujinpeng.hcecard;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by liujinpeng on 16/3/22.
 */

public class MyApduService extends HostApduService {
    private static final byte APDU_OFFSET_INS = 1;
    private static final byte APDU_OFFSET_P1 = 2;
    private static final byte APDU_OFFSET_P2 = 3;
    private static final byte APDU_OFFSET_LC = 4;
    private static final byte APDU_OFFSET_Data = 5;

    private String filePath = "/sdcard/.SAUB/";
    private String fileName = ".SAUB_save_file.bin";
    private long filesize = 0;
    private FileInputStream storeFileStream = null;

    @Override
    public void onDeactivated(int reason) {
        filesize = 0;

    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        byte[] responce = {(byte)0x6E,0x00};;

        switch (commandApdu[APDU_OFFSET_INS]) {
            case 0x01:
                responce = InitForFileUpload(commandApdu);
                break;
            case 0x02:
                responce = UploadFile(commandApdu);
                break;
            case (byte)0x81:
                responce = InitForFileDownload(commandApdu);
                break;
            case (byte)0x82:
                responce = DownloadFile(commandApdu);
                break;
            case (byte)0xA4:
                responce[0] = (byte)0x90;
                break;
            default:
                break;
        }

        return responce;
    }

    private boolean selectAidApdu(byte[] apdu) {
        return true;
    }

    private byte[] InitForFileUpload(byte[] apdu){
        long size;
        byte [] ret = {(byte)0x90,0x00};
        if (apdu[APDU_OFFSET_LC] < 0x04) {
            ret[0] = 0x67;
            return ret;
        }
        size = apdu[APDU_OFFSET_Data]*0x01000000 + apdu[APDU_OFFSET_Data+1]*0x00010000
                + apdu[APDU_OFFSET_Data+2]*0x00000100 + apdu[APDU_OFFSET_Data+3];

        filesize = size;

        if (apdu[APDU_OFFSET_LC] > 0x04) {
            fileName = "";
            for (byte i=APDU_OFFSET_Data+4;i<apdu[APDU_OFFSET_LC] +APDU_OFFSET_Data ;i++) {
                fileName += String.format("%C", apdu[i]);
            }
        }

        try {
            File tmpfile = new File(filePath+fileName);
            if (tmpfile.exists()) {
                tmpfile.delete();
            }
        } catch (Exception e) {
            ret[0] = 0x6F;
            //return ret;
        }

        return ret;
    }

    /**
     * Load file from computer(Terminal) to cellPhone(SmartCard).
     * @param apdu
     * @return SW1Sw2
     */
    private byte[] UploadFile(byte[] apdu){
        byte [] ret = {(byte)0x90,0x00};
        short tmplength = (short)(0x00FF&apdu[APDU_OFFSET_LC]);
        try {
            FileOutputStream fout = new FileOutputStream(filePath+fileName, true);
            byte[] bytes = new byte[tmplength];
            System.arraycopy(apdu,APDU_OFFSET_Data,bytes,0,tmplength);
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            ret[0] = 0x6F;
        }
        return ret;
    }

    /**
     *
     * @param apdu
     * @return
     */
    private byte[] InitForFileDownload(byte[] apdu) {
        byte[] ret = {(byte)0x6F,0x00};
        byte[] rspdata;
        long filelength;

        try {
            File DF = new File(filePath);
            if (!DF.exists()) {
                ret[0] = 0x6A;
                ret[1] = (byte)0x82;
                return ret;
            }
            File[] files = DF.listFiles();
            if (files.length == 0) {
                ret[0] = 0x6A;
                ret[1] = (byte)0x82;
                return ret;
            }
            fileName = files[0].getName();
            //tmpfile = new File(filePath + fileName);

            filelength = files[0].length();

            storeFileStream = new FileInputStream(filePath+fileName);

            rspdata = new byte[6 + fileName.length()];
            rspdata[0] = (byte)((filelength&0xFF000000)>>24);
            rspdata[1] = (byte)((filelength&0x00FF0000)>>16);
            rspdata[2] = (byte)((filelength&0x0000FF00)>>8);
            rspdata[3] = (byte)(filelength&0x000000FF);
            System.arraycopy(fileName.getBytes(),0,rspdata,4,fileName.length());
            rspdata[fileName.length()+4] = (byte)0x90;
            rspdata[fileName.length()+5] = 0x00;

            return rspdata;

        } catch (Exception e) {
            return ret;
        }
        //return ret;
    }

    /**
     * Send file from cellPhone(SmartCard) to computer(Terminal).
     * @param apdu
     * @return buffer that contains the data
     */
    private byte[] DownloadFile(byte[] apdu){
        byte[] ret = {(byte)0x6F,0x00};
        byte[] filedata;
        int readsize;

        if (storeFileStream == null) return ret;

        try {
            filedata = new byte[200];
            readsize = storeFileStream.read(filedata);
            if (readsize == -1) {
                storeFileStream.close();
                storeFileStream = null;
                ret[0] = (byte)0x90;
                return ret;
            }

            ret = new byte[readsize+2];
            System.arraycopy(filedata,0,ret,0,readsize);
            ret[readsize] = (byte)0x90;
            ret[readsize+1] = (byte)0x00;
        } catch (Exception e) {
            return ret;
        }
        return ret;
    }


}
