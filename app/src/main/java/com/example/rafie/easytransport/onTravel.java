package com.example.rafie.easytransport;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.lang.*;

public class onTravel extends AppCompatActivity{
    DatabaseReference mDatabase= FirebaseDatabase.getInstance().getReference();
    DatabaseReference mref,mlat,mlong,mstatus;
    TextView textView;
    double newbal=0,dis=0,fare=0;
    GPSTracker gps;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.travel);
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String id = intent.getStringExtra(rfidreader.EXTRA_MESSAGE);
        // Capture the layout's TextView and set the string as its text
        textView = (TextView) findViewById(R.id.message);
        //textView.setText(id+" "+"signed in successfully");
        getData(id);
    }
    public void getData(final String input)
    {
        //String pssngrid=input;
        mref=mDatabase.child(input).child("Balance");
        mlat=mDatabase.child(input).child("Start").child("Lat");
        mlong=mDatabase.child(input).child("Start").child("Long");
        mstatus=mDatabase.child(input).child("Status");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Double currbal = dataSnapshot.child(input).child("Balance").getValue(Double.class);
                Integer status = dataSnapshot.child(input).child("Status").getValue(Integer.class);
                Double mlatitude = dataSnapshot.child(input).child("Start").child("Lat").getValue(Double.class);
                Double mlongitude = dataSnapshot.child(input).child("Start").child("Long").getValue(Double.class);
                if (currbal >= 10) {
                    gps = new GPSTracker(onTravel.this);
                    double slatitude = gps.getLatitude();
                    double slongitude = gps.getLongitude();
                    if (status == 0) {
                        mlat.setValue(slatitude);
                        mlong.setValue(slongitude);
                        mstatus.setValue(1);
                        textView.setText("Thank you,you are eligible for travelling....");
                        //Intent intent = new Intent(this, rfidreader.class);
                        // startActivity(intent);
                    } else if (status == 1) {
                        dis = HaverSineDistance(mlatitude, mlongitude, slatitude, slongitude);
                        fare = dis * 5;
                        newbal = currbal - fare;
                        mref.setValue(newbal);
                        mstatus.setValue(0);
                        textView.setText("Distance Traveled:" + dis + "\n" + "Current Balance:" + currbal + "\n" + "Fare:" + fare + "\n" + "Net Balance:" + newbal);
                        //Intent intent = new Intent(this, rfidreader.class);
                        // startActivity(intent);
                    }
                }
                else {
                    textView.setText("Sorry,you are not eligible for travelling this time.Please recharge your card..");
                    //Intent intent = new Intent(this, rfidreader.class);
                    // startActivity(intent);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public static double HaverSineDistance(double slatitude, double slongitude, double dlatitude, double dlongitude)
    {
        double EARTH_RADIUS = 6371.0;
        slatitude = Math.toRadians(slatitude);
        slongitude = Math.toRadians(slongitude);
        dlatitude = Math.toRadians(dlatitude);
        dlongitude = Math.toRadians(dlongitude);
        double dlon = dlongitude - slongitude;
        double dlat = dlatitude - slatitude;
        double a = Math.pow((Math.sin(dlat/2)),2) + Math.cos(slatitude) * Math.cos(dlatitude) * Math.pow(Math.sin(dlon/2),2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * c;
    }
}
