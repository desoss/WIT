package it.polimi.dmw.wit.utilities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import it.polimi.dmw.wit.activities.WitFacebookLogin;
import it.polimi.dmw.wit.activities.WitFinalResult;
import it.polimi.dmw.wit.activities.WitInfo;


public class WitDownloadImageTask extends AsyncTask<URL, Void, Bitmap> {

    private final static String LOG_TAG = "WitDownloadImageTask";
    private byte[] img=null;
    private byte[] thumbnail = null;
    Activity activity;
    Fragment fragment;
    WitFacebookLogin facebookL;
    WitFinalResult finalR;
    WitInfo info;
    public static final int FACEBOOK = 0, POIDETAIL = 1, CITY = 2, IMAGEPROFILE = 4, POIBEST = 3;
    private int c;
    private int posB5;


    public WitDownloadImageTask(Activity activity, Fragment fragment, int c){
        this.activity = activity;
        this.fragment = fragment;
        this.c = c;

    }



    protected Bitmap doInBackground(URL... urls) {
        Bitmap downloadBitmap = null;
        try {
            InputStream in = urls[0].openStream();
            downloadBitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
        return downloadBitmap;
    }

    protected void onPostExecute(Bitmap result) {
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        if(result!=null){
        result.compress(Bitmap.CompressFormat.JPEG, 60, bos);
        img = bos.toByteArray();
            bos=new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 13, bos);
        thumbnail = bos.toByteArray();
        }
        else{
            img = null;
            thumbnail = null;
        }

        Log.d(LOG_TAG, "image downloaded"+result);
        switch (c){
            case FACEBOOK:
                facebookL = (WitFacebookLogin)activity;
                facebookL.setImage(result, img);
                break;
            case POIDETAIL:
                finalR = (WitFinalResult)activity;
                finalR.setImage(result, img, thumbnail);
                break;
            case CITY:
                info = (WitInfo) fragment;
                info.setImageCity(result, img, thumbnail);
                break;
            case IMAGEPROFILE:
                finalR = (WitFinalResult)activity;
                finalR.saveImage(img);
                break;
            case POIBEST:
                info = (WitInfo) fragment;
                info.saveImagePoi(img,posB5);
                break;


        }

    }

    public void setPosB5(int p){
        posB5 = p;
    }


}
