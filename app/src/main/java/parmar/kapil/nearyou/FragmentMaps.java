package parmar.kapil.nearyou;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

public class FragmentMaps extends Fragment implements OnMapReadyCallback {
    // flag for Internet connection status
    Boolean isInternetPresent = false;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // Google Places
    GooglePlaces googlePlaces;

    // Place Details
    PlaceDetails placeDetails;

    // Progress dialog
    ProgressDialog pDialog;
    Context context;
    MapView mapView;
    GoogleMap map;
    // KEY Strings
    public static String KEY_REFERENCE = "reference"; // id of the place
    View view;
    GPSTracker gps;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.map_fragment, container, false);
        context = getActivity();
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
//		String reference = i.getStringExtra(KEY_REFERENCE);
        if (getArguments() != null) {
            Bundle arguments = getArguments();
            String reference = arguments.getString("placeId");
            new LoadSinglePlaceDetails().execute(reference);
        }
        gps = new GPSTracker(context);
        // Calling a Async Background thread



        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);


    }


    /**
	 * Background Async Task to Load Google places
	 * */
	class LoadSinglePlaceDetails extends AsyncTask<String, String, String> {
        double latitude =0.0 ,longitude =0;

		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage("Loading profile ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting Profile JSON
		 * */
		protected String doInBackground(String... args) {
			String reference = args[0];
			
			// creating Places class object
			googlePlaces = new GooglePlaces();

			// Check if used is connected to Internet
			try {
				placeDetails = googlePlaces.getPlaceDetails(reference);

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * **/
		protected void onPostExecute(String file_url) {
			// dismiss the dialog after getting all products
			pDialog.dismiss();
			// updating UI from Background Thread
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					/**
					 * Updating parsed Places into LISTVIEW
					 * */
					if(placeDetails != null){
						String status = placeDetails.status;
						
						// check place deatils status
						// Check for all possible status
						if(status.equals("OK")){
							if (placeDetails.result != null) {
								String name ="Not Present" ;
								name= placeDetails.result.name;
								String address = "Not Present" ;
								address =placeDetails.result.formatted_address;
								String phone ="Not Present" ;
								phone = placeDetails.result.formatted_phone_number;
								latitude =(placeDetails.result.geometry.location.lat);
								longitude = (placeDetails.result.geometry.location.lng);
								String image = placeDetails.result.icon;
								LatLng position = new LatLng(latitude,longitude);


								Log.d("Place ", ""+ image);
								
								// Displaying all the details in the view
								// map_fragment.xml
								TextView lbl_name = view.findViewById(R.id.tvName);
								TextView lbl_address = view.findViewById(R.id.address);
								TextView lbl_phone = view.findViewById(R.id.tvNumber);
                               ImageView imgIcon = view.findViewById(R.id.img);
                                Picasso.with(context)
                                        .load(image)
                                        .into(imgIcon);
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12.0f));
                                map.addMarker(new MarkerOptions().position(position)).setTitle(name);

//								TextView lbl_location = view.findViewById(R.id.location);
								
								// Check for null data from google
								// Sometimes place details might missing
							/*	name = name == null ? "Not present" : name; // if name is null display as "Not present"
								address = address == null ? "Not present" : address;
								phone = phone == null ? "Not present" : phone;
								latitude = latitude == null ? "Not present" : latitude;
								longitude = longitude == null ? "Not present" : longitude;*/
								
								lbl_name.setText(name);
								lbl_address.setText(address);
								lbl_phone.setText(Html.fromHtml("<b>Phone:</b> " + phone));
                                ImageView imgDir =  view.findViewById(R.id.imgdir);
                                imgDir.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        String uri = "http://maps.google.com/maps?saddr="+gps.getLatitude()+","+ gps.getLongitude()+"&daddr="+latitude+","+longitude;
                                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
                                        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                                        startActivity(intent);
                                    }
                                });
//								lbl_location.setText(Html.fromHtml("<b>Latitude:</b> " + latitude + ", <b>Longitude:</b> " + longitude));
							}
						}
						else if(status.equals("ZERO_RESULTS")){
							alert.showAlertDialog(context, "Near Places",
									"Sorry no place found.",
									false);
						}
						else if(status.equals("UNKNOWN_ERROR"))
						{
							alert.showAlertDialog(context, "Places Error",
									"Sorry unknown error occured.",
									false);
						}
						else if(status.equals("OVER_QUERY_LIMIT"))
						{
							alert.showAlertDialog(context, "Places Error",
									"Sorry query limit to google places is reached",
									false);
						}
						else if(status.equals("REQUEST_DENIED"))
						{
							alert.showAlertDialog(context, "Places Error",
									"Sorry error occured. Request is denied",
									false);
						}
						else if(status.equals("INVALID_REQUEST"))
						{
							alert.showAlertDialog(context, "Places Error",
									"Sorry error occured. Invalid Request",
									false);
						}
						else
						{
							alert.showAlertDialog(context, "Places Error",
									"Sorry error occured.",
									false);
						}
					}else{
						alert.showAlertDialog(context, "Places Error",
								"Sorry error occured.",
								false);
					}
					
					
				}
			});

		}

	}


}
