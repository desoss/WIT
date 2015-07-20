package it.polimi.dmw.wit;


import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.pkmmte.view.CircularImageView;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.listeners.OnLoginListener;
import com.sromku.simple.fb.listeners.OnLogoutListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;


import it.polimi.dmw.wit.database.DbAdapter;

public class WitFacebookLogin extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    /**
     * Tag per il log
     */
    private final static String LOG_TAG = "WitFacebokLogin";
    private LoginButton mButtonLogin;


    private ImageView mainImage;
    private CircularImageView circularImageView;

    private String name;
    private String surname;
    private long id;

    private DbAdapter dbAdapter;
    private byte[] img=null;
    private Cursor cursor;

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private  Profile profile;




    private TextView mTextDetails;
    private CallbackManager mCallbackManager;
    private CallbackManager callbackManager;
    private AccessToken token;
    private ProfileTracker mProfileTracker;
    private FacebookCallback<LoginResult> mFacebookCallback;
    private FloatingActionButton loginB;
    private FloatingActionButton logoutB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(getApplicationContext());
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_wit_login);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_Facebook_Login);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
        //  displayView(0);

        mButtonLogin = (LoginButton) findViewById(R.id.login_button);
        loginB = (FloatingActionButton) findViewById(R.id.loginB);
        loginB.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        logoutB = (FloatingActionButton) findViewById(R.id.logoutB);
        logoutB.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        mTextDetails = (TextView) findViewById(R.id.text_details);
        profile = Profile.getCurrentProfile();

        if (profile!=null) {
            loginB.setVisibility(View.GONE);
            logoutB.setVisibility(View.VISIBLE);
            mTextDetails.setText(constructWelcomeMessage(profile));

        }



        //mainImage = (ImageView)findViewById(R.id.profile_img);


        circularImageView = (CircularImageView)findViewById(R.id.profile_img);
        circularImageView.setBorderColor(getResources().getColor(R.color.colorPrimary));
        circularImageView.setBorderWidth(7);
       // circularImageView.setSelectorColor(getResources().getColor(R.color.colorPrimary));
        //circularImageView.setSelectorStrokeColor(getResources().getColor(R.color.colorPrimaryDark));
        circularImageView.setSelectorStrokeWidth(10);
        circularImageView.addShadow();
        loadProfileImage();


/*


        mButtonLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call private method
                onFbLogin();




            }
        });


*/

        loginB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call private method
                login();
            }
        });

        logoutB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Call private method
                logout();
            }
        });

    }




    private void login(){

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "user_friends"));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        AccessToken a = loginResult.getAccessToken();
                        Log.d("Success", "Login");
                        profile = Profile.getCurrentProfile();
                        name = profile.getFirstName();
                        surname = profile.getLastName();
                        id = Long.valueOf(profile.getId());


                        mTextDetails.setText(constructWelcomeMessage(profile));
                        URL photoURL = null;
                        try {
                            photoURL = new URL("https://graph.facebook.com/" + profile.getId() + "/picture?type=large");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                        new DownloadImageTask().execute(photoURL);

                        loginB.setVisibility(View.GONE);
                        logoutB.setVisibility(View.VISIBLE);


                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });



    }

    private void logout(){
        logoutUser(id);
        LoginManager.getInstance().logOut();
        loginB.setVisibility(View.VISIBLE);
        logoutB.setVisibility(View.GONE);
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        dbAdapter.updateUSER(id, name, surname, img, false, false);
        dbAdapter.close();
        profile = null;
        Intent i = new Intent(this, WitFacebookLogin.class);
        startActivity(i);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

        @Override
        protected void onResume() {
        // Always call superclass method first
        super.onResume();
    }

    @Override
    public void onDrawerItemSelected(View view, int position) {
        displayView(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startSettingPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private String constructWelcomeMessage(Profile profile) {
        StringBuffer stringBuffer = new StringBuffer();
        if (profile != null) {
            stringBuffer.append("Welcome " + profile.getName());
        }
        return stringBuffer.toString();
    }


    private void onFbLogin(){
        mCallbackManager = CallbackManager.Factory.create();
        mButtonLogin.setReadPermissions("user_friends");
        mFacebookCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("WitFacebookLogin", "onSuccess");
                token = loginResult.getAccessToken();
                Profile profile = Profile.getCurrentProfile();
                name = profile.getFirstName();
                surname = profile.getLastName();
                id =  Long.valueOf(profile.getId());


                mTextDetails.setText(constructWelcomeMessage(profile));
                URL photoURL=null;
                try {
                    photoURL = new URL("https://graph.facebook.com/" + profile.getId() + "/picture?type=large");
                }
                    catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                 new DownloadImageTask().execute(photoURL);


            }
            @Override
            public void onCancel() {
                Log.d("WitFacebookLogin", "onCancel");
            }

            @Override
            public void onError(FacebookException e) {
                Log.d("WitFacebookLogin", "onError " + e);
            }
        };
        mButtonLogin.registerCallback(mCallbackManager, mFacebookCallback);




    }


private class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {

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
            circularImageView.setImageBitmap(result);
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            result.compress(Bitmap.CompressFormat.PNG, 100, bos);
            img=bos.toByteArray();
            saveUser(id, name, surname, img);
        }
    }


    private void saveUser(long id, String name, String surname, byte[] img) {
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        cursor = dbAdapter.fetchUserByID(id);
        if(cursor.moveToNext()){
            int i = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_FB));
            boolean fb;
            if(i==0) fb = false;
            else fb = true;
            dbAdapter.updateUSER(id, name, surname, img, fb, true);
            Log.d(LOG_TAG, "UPDATE");

        }
        else {
            dbAdapter.saveUser(id, name, surname, img, false, true);
            Log.d(LOG_TAG, "NEW");
        }
        dbAdapter.close();
    }

    private void logoutUser(long id) {
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        dbAdapter.logoutUSER(id);
        dbAdapter.close();
    }

    private void loadProfileImage() {
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        cursor = dbAdapter.fetchAllUSERS();
        Log.d(LOG_TAG, "LOAD PROFILE IMAGE");
        while (cursor.moveToNext()) {
            int b = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ISLOGGED));
            Log.d(LOG_TAG, " ="+b);
            if (b == 1) {
                byte[] img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_IMAGE));
                circularImageView.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
                break;
            }
        }
        dbAdapter.close();
    }


    private void displayView(int position) {
        Fragment fragment = null;
        Intent i = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case 0:
                i = new Intent(this, WitMainActivity.class);
                startActivity(i);
                break;
            case 1:
                i = new Intent(this, WitPOIsList.class);
                startActivity(i);
                break;
            case 2:
               // i = new Intent(this, WitFacebookLogin.class);
              //  startActivity(i);
                break;


            default:
                break;
        }

        if (fragment != null) {
          /*  FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();  */

            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }

    private void startSettingPage(){
        Intent i = new Intent(this, WitSettings.class);
        startActivity(i);

    }



    }
