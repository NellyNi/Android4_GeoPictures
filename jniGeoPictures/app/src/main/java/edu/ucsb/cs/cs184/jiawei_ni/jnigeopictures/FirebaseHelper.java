//TODO Change your package name
package edu.ucsb.cs.cs184.jiawei_ni.jnigeopictures;

import android.content.Context;
import android.net.sip.SipSession;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Created by Donghao Ren on 03/11/2017.
 * Modified by Ehsan Sayyad on 11/9/2018
 * Modified by Jake Guida on 11/6/2019
 */

/**
 * This is a Firebase helper starter class we have created for you
 * In your Activity, FirebaseHelper.Initialize() is called to setup the Firebase
 * Put your application logic in OnDatabaseInitialized where you'll have the database object initialized
 */
public class FirebaseHelper {

    /** This is a message data structure that mirrors our Firebase data structure for your convenience */
    public static class Post implements Serializable {

        public Double longitude=0.0;
        public Double latitude=0.0;
        public Double title=0.0;
        public Double timestamp=0.0;
    }

    /** Keep track of initialized state, so we don't initialize multiple times */

    private static boolean initialized = false;

    /** The Firebase database object */
    private static FirebaseDatabase db;
    //private static DatabaseReference db_ref;
    /** Initialize the firebase instance */
    public static void Initialize(final Context context) {

        if (!initialized) {
            initialized = true;
            FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                    .setDatabaseUrl("https://geopic2.firebaseio.com/")
                    .setApiKey("AIzaSyBEqMXOi5m0N178WNLf9oGkJLEvoFxeJmg")
                    .setApplicationId("geopic2")
                    .build(),
                    "class_db"
            );

            // Call the OnDatabaseInitialized to setup application logic
            OnDatabaseInitialized();
            setTweetListener(context);
        }
    }

    public static ArrayList<Tweet> tweets;



    /** This is called once we initialize the firebase database object */
    private static void OnDatabaseInitialized() {
        db = FirebaseDatabase.getInstance(FirebaseApp.getInstance("class_db"));
        DatabaseReference db_ref = db.getReference("posts");
        tweets=new ArrayList<>();
        db_ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("DATABASE_TEST","First child added");

                Post message = dataSnapshot.getValue(Post.class);
                Tweet tweet = new Tweet(dataSnapshot.getKey(), message.title, message.timestamp, message.longitude, message.latitude);
                tweet.getPath().add(tweet.getLocation());
                tweets.add(tweet);
                tweetListener.onFirstTweetsAdded(tweet);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        db_ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d("DATABASE_TEST","child added");

                Post message = dataSnapshot.getValue(Post.class);
                Tweet tweet = new Tweet(dataSnapshot.getKey(), message.title, message.timestamp, message.longitude, message.latitude);
                tweet.getPath().add(tweet.getLocation());
                tweets.add(tweet);
                tweetListener.onTweetAdded(tweet);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d("DATABASE_TEST","child changed");

                Post message = dataSnapshot.getValue(Post.class);
                Tweet newTweet = new Tweet(dataSnapshot.getKey(), message.title, message.timestamp, message.longitude, message.latitude);
                for (Tweet tweet : tweets) {
                    if (tweet.getPostId().equals(newTweet.getPostId())) {
                        tweet.getPath().add(newTweet.getLocation());
                        tweet.setLastLocation(tweet.getLocation());
                        tweet.setLocation(newTweet.getLocation());
                        tweetListener.onTweetUpdated(tweet);
                    }
                }


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d("DATABASE_TEST","child removed");

                Post message = dataSnapshot.getValue(Post.class);
                Tweet deleteTweet = new Tweet(dataSnapshot.getKey(), message.title, message.timestamp, message.longitude, message.latitude);
                for (int i=0; i<tweets.size(); i++) {
                    if (tweets.get(i).getPostId().equals(deleteTweet.getPostId())) {
                        tweets.remove(i);
                    }
                }

                tweetListener.onTweetRemoved(tweets, deleteTweet);

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // TODO: Setup your callbacks to listen for /posts.
        // Your code should handle post added, post updated, and post deleted events.
        // When a post is added: add a marker to the map that when clicked displays the image and the time it was uploaded in a fragment
        // An image can be identified by its title, an integer between 0 and 9, which corresponds to the resource file name
        // When a post is deleted: remove the marker from the map
        // When a post is updated: update its position on the map and draw the path from its previous point


    }

    // TODO: You *may* (strongly encouraged) create a listener mechanism so that your Activity and Fragments can register callbacks to the database helper

    public interface TweetListener {
        void onFirstTweetsAdded(Tweet tweet);
        void onTweetAdded(Tweet tweet);
        void onTweetUpdated(Tweet tweet);
        void onTweetRemoved(ArrayList<Tweet> tweets, Tweet tweet);
    }

    private static TweetListener tweetListener;

    private static void setTweetListener(Context context) {
        tweetListener = (MapsActivity)context;
    }
}

