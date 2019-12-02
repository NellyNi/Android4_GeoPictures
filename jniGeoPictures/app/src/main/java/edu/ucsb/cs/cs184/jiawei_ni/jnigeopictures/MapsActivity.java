package edu.ucsb.cs.cs184.jiawei_ni.jnigeopictures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        FirebaseHelper.TweetListener{

    private static final LatLng UCSB = new LatLng(34.412936,-119.847863);

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;

    String pathToFile;
    String name;
    private static final int PERMISSION_ALL = 10;

    private String[] permissions= new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Marker redMark;
    private ArrayList<Marker> markers=new ArrayList<>();
    public ArrayList<Marker> redMarkers=new ArrayList<>();
    public static ArrayList<Tweet> Stweets=new ArrayList<>();
    public static ArrayList<Tweet> Ptweets=new ArrayList<>();

    int notificationId;
    private static final int REQ_CODE_TAKE_PICTURE = 1;
    public String photoFileName="";
    Uri takenPhotoUri;
    public static ArrayList<Uri> Uris=new ArrayList<>();
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = this.findViewById(R.id.fab);
            // set the icon to a speaker (needs to be in resources-->drawable):
            //fab.setImageResource(R.drawable.ic_keyboard_voice_24px);
        fab.show();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
            }
        });


        FirebaseHelper.Initialize(this);
            //markers = new ArrayList<>();
            //redMarkers=new ArrayList<>();

        notificationId = 0;

        OnDatabaseSet();
        FloatingActionButton cfab = this.findViewById(R.id.fab_cam);
        // set the icon to a speaker (needs to be in resources-->drawable):
        //fab.setImageResource(R.drawable.ic_keyboard_voice_24px);
        cfab.show();

        cfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });

            //  permissions  granted.
            //FirebaseHelper.Initialize(this);




    }
    private void OnDatabaseSet() {
        FirebaseApp.initializeApp(this, new FirebaseOptions.Builder()
                        .setDatabaseUrl("https://named-sequencer-258902.firebaseio.com")
                        .setApiKey("AIzaSyC_-amIXTu4iu_NUvRs6VYpVaeP9pR3ZmM")
                        .setApplicationId("1:293166256809:android:5a30edaf4c32e424738fb8")
                        .build(),
                "mybase"
        );

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance(FirebaseApp.getInstance("mybase"));
        myRef = database.getReference().child("test");

    }


    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(MapsActivity.this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),PERMISSION_ALL );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied
                    //updateViews();
                }
                return;
            }
        }
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public static class Send implements Serializable {
        public Double title=0.0;
        public Double timestamp=0.0;

        public Double longitude=0.0;
        public Double latitude=0.0;

    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();

            // Add a marker in Sydney and move the camera
            //mMap.addMarker(new MarkerOptions().position(UCSB).title("Marker in UCSB"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(UCSB, 15));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                //allPoints.add(point);
                long epoch = System.currentTimeMillis();
                String id = myRef.push().getKey();

                redMark = mMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title(id));
                redMarkers.add(redMark);

                Double[] pictures = new Double[10];
                for (int i = 0; i < 10; i++) {
                        pictures[i] = 1.573537538000e12 + i;
                }


                Tweet tweet = new Tweet(id, pictures[(int) (Math.random() * ((9 - 0) + 1))],
                        Double.parseDouble(String.valueOf(epoch)), redMark.getPosition().longitude, redMark.getPosition().latitude);
                myRef.child(id).setValue(tweet);

                myRef.child(id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Send value = dataSnapshot.getValue(Send.class);
                        Tweet tweet = new Tweet(dataSnapshot.getKey(), value.title, value.timestamp, value.longitude, value.latitude);
                        tweet.getPath().add(tweet.getLocation());
                        tweet.setLocation(tweet.getLocation());
                        Stweets.add(tweet);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        });


        if (checkPermissions()) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMarkerClickListener(this);


    }

    public void onFirstTweetsAdded(Tweet tweet) {
        addMarker(tweet);
    }

    public void onTweetAdded(Tweet tweet) {
        addMarker(tweet);
    }

    public void onTweetUpdated(Tweet tweet) {
        mMap.addPolyline((new PolylineOptions())
                .add(tweet.getLastLocation(), tweet.getLocation())
                .width(7)
                .color(Color.BLACK)
                .visible(true));
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(tweet.getLocation())
                .title(String.valueOf(tweet.getTimestamp())));
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        tweet.getMarker().remove();
        markers.remove(tweet.getMarker());
        tweet.setMarker(marker);
        markers.add(marker);

    }

    public void onTweetRemoved(ArrayList<Tweet> tweets, Tweet deleteTweet) {
        mMap.clear();
        //deleteTweet.getMarker().remove();

        for (Tweet tweet : tweets) {
            ArrayList<LatLng> path = tweet.getPath();
            if (path.size() > 1) {
                LatLng previous = path.get(0);
                LatLng current;
                for (int i = 1; i < path.size(); i++) {
                    current = path.get(i);
                    mMap.addPolyline((new PolylineOptions())
                            .add(previous, current)
                            .width(7)
                            .color(Color.BLACK)
                            .visible(true));
                    previous = current;
                }
            }
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(tweet.getLocation())
                    .title(String.valueOf(tweet.getTimestamp())));
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            tweet.setMarker(marker);
            markers.add(marker);
        }


        //deleteTweet.getMarker().remove();
        for (Marker m : redMarkers) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(m.getPosition())
                    .title(m.getTitle()));
            //tweet.setMarker(marker);
            //redMarkers.add(marker);

        }

        for (Marker m : greenMarkers) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(m.getPosition())
                    .title(m.getTitle()));
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        for (Tweet tweet:FirebaseHelper.tweets){
            if (String.valueOf(tweet.getTimestamp()).equals((marker.getTitle()))){
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.fragment_dialog_tweet, null);
                ImageView photoView = mView.findViewById(R.id.pic);
                TextView textView = mView.findViewById(R.id.contentText);
                //photoView.setImageResource(R.drawable.img);

                Date date = new Date(tweet.getTimestamp().longValue());
                android.text.format.DateFormat df = new android.text.format.DateFormat();

                textView.setText(df.format("MM-dd-yyyy kk:mm:ss", date));
                if(checkPermissions()) {
                    File file = new File(getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), String.format("%.0f", tweet.getTitle()) + ".jpg");
                    Picasso.get().load(file).into(photoView);
                }
                mBuilder.setView(mView);
                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
                return true;
            }
        }

        for(Tweet tweet:Stweets){
            if (String.valueOf(tweet.getPostId()).equals((marker.getTitle()))) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.fragment_dialog_tweet, null);
                ImageView photoView = mView.findViewById(R.id.pic);
                TextView textView = mView.findViewById(R.id.contentText);
                //photoView.setImageResource(R.drawable.img);

                Date date = new Date(tweet.getTimestamp().longValue());
                android.text.format.DateFormat df = new android.text.format.DateFormat();


                textView.setText(df.format("MM-dd-yyyy kk:mm:ss", date));
                File file = new File(getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        String.format("%.0f", tweet.getTitle()) + ".jpg");
                Picasso.get().load(file).into(photoView);
                mBuilder.setView(mView);
                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
                return true;
            }

        }

        for(Tweet tweet:Ptweets){
            if (String.valueOf(tweet.getPostId()).equals((marker.getTitle()))) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.fragment_dialog_tweet, null);
                ImageView photoView = mView.findViewById(R.id.pic);
                TextView textView = mView.findViewById(R.id.contentText);
                //photoView.setImageResource(R.drawable.img);

                Date date = new Date(tweet.getTimestamp().longValue());
                android.text.format.DateFormat df = new android.text.format.DateFormat();

                textView.setText(df.format("MM-dd-yyyy kk:mm:ss", date));
                Uri u=getPhotoFileUri(String.format("%.0f", tweet.getTitle())+".jpg");

                Bitmap bmp = BitmapFactory.decodeFile(u.getPath());
                //Log.d("DATABASE_TEST","Photo ERROR Uri"+bmp);
                photoView.setImageBitmap(bmp);

                mBuilder.setView(mView);
                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
                return true;
            }

        }


        return false;
    }

    public void addMarker(Tweet tweet) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(tweet.getLocation())
                .title(String.valueOf(tweet.getTimestamp())));
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        tweet.setMarker(marker);
        markers.add(marker);

    }



    //Getting current location
    public ArrayList<Marker> greenMarkers = new ArrayList<>();

    private void getCurrentLocation() {
        //mMap.clear();

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //Log.d("DATABASE_TEST",location.getLatitude()+"LON"+location.getLongitude());
        if (location != null) {
            //Getting longitude and latitude
            LatLng Glatlng = new LatLng(location.getLatitude(),location.getLongitude());
            String id="";
            if(checkPermissions()) {
                long epoch = System.currentTimeMillis();
                photoFileName = String.valueOf(epoch);
                id = myRef.push().getKey();

                Marker Gmarker = mMap.addMarker(new MarkerOptions()
                        .position(Glatlng)
                        .title(id));
                greenMarkers.add(Gmarker);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Glatlng, 15));
                Gmarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                takephoto(epoch);


                Tweet tweet = new Tweet(id, Double.parseDouble(photoFileName),
                        Double.parseDouble(String.valueOf(epoch)), Gmarker.getPosition().longitude, Gmarker.getPosition().latitude);
                myRef.child(id).setValue(tweet);

                myRef.child(id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Send value = dataSnapshot.getValue(Send.class);
                        Tweet tweet = new Tweet(dataSnapshot.getKey(), value.title, value.timestamp, value.longitude, value.latitude);
                        tweet.getPath().add(tweet.getLocation());
                        tweet.setLocation(tweet.getLocation());
                        Ptweets.add(tweet);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }


        }
    }

    public void takephoto(long e){
        Intent picIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        picIntent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri(photoFileName+".jpg"));
        if (picIntent.resolveActivity(getPackageManager())!=null){

            startActivityForResult(picIntent, REQ_CODE_TAKE_PICTURE);
            /**
            if(photofile != null) {
                pathToFile = photofile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(MapsActivity.this,"edu.ucsb.cs.cs184.jiawei_ni.jnigeopictures.fileprovider",photofile);
                picIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            }
             */

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQ_CODE_TAKE_PICTURE
                && resultCode == RESULT_OK) {
            takenPhotoUri = getPhotoFileUri(photoFileName+".jpg");
            Uris.add(takenPhotoUri);
            //Bitmap bmp = BitmapFactory.decodeFile(takenPhotoUri.getPath());
            //View pView = getLayoutInflater().inflate(R.layout.fragment_dialog_tweet, null);
            //ImageView photoView = pView.findViewById(R.id.pic);
            //ImageView img = (ImageView) findViewById(R.id.pic);
            //Log.d("DATABASE_TEST","Photo ERROR Uri"+bmp);
            //img.setImageBitmap(bmp);
        }
    }
    public final String APP_TAG = "MyCustomApp";

    public Uri getPhotoFileUri(String fileName) {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            // Use `getExternalFilesDir` on Context to access package-specific directories.
            // This way, we don't need to request external read/write runtime permissions.
            File mediaStorageDir = new
                    File( getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_TAG);
            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
                Log.d(APP_TAG, "failed to create directory");
            }

            // Return the file target for the photo based on filename
            File file = new File(mediaStorageDir + File.separator + fileName);
            Log.d("DATABASE_TEST","Photo ERROR"+file+"FILE: "+fileName);
            // wrap File object into a content provider, required for API >= 24
            return FileProvider.getUriForFile(MapsActivity.this, "edu.ucsb.cs.cs184.jiawei_ni.jnigeopictures.fileprovider", file);
        }
        return null;
    }
    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

}
