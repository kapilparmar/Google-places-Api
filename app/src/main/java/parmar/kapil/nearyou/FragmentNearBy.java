package parmar.kapil.nearyou;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class FragmentNearBy extends Fragment {
    int PLACE_PICKER_REQUEST = 1;

	// flag for Internet connection status
	Boolean isInternetPresent = false;

	// Connection detector class
	ConnectionDetector cd;
	AlertDialogManager alert = new AlertDialogManager();
	GooglePlaces googlePlaces;
	PlacesList nearPlaces;

	GPSTracker gps;

	// Button
	Button btnShowOnMap;

	// Progress dialog
	ProgressDialog pDialog;
	
	// Places Listview
	ListView lv;
    TextView editTextSearch;
	// ListItems data
	ArrayList<HashMap<String, String>> placesListItems ;
	
	
	// KEY Strings
	public static String KEY_REFERENCE = "reference"; // id of the place
	public static String KEY_NAME = "name"; // name of the place
	public static String KEY_VICINITY = "vicinity"; // Place area name


    Context context;

    public FragmentNearBy() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.nearby_fragment, container, false);
        context = getActivity();
        cd = new ConnectionDetector(context);
//
//        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
//                getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

          editTextSearch = view.findViewById(R.id.search);
        editTextSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                    .build(getActivity());
                    startActivityForResult(intent, PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            }
        });
        ImageButton imgSearch = view.findViewById(R.id.img_search);
        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextSearch.setText("Current Location");
                new LoadPlaces(gps.getLatitude(), gps.getLongitude()).execute();

            }
        });


        // Check if Internet present
        isInternetPresent = cd.isConnectingToInternet();
        if (!isInternetPresent) {
            // Internet Connection is not present
            alert.showInternetGpsAlert(context, "Internet Connection Error", "Please connect to working Internet connection", getActivity());

            // stop executing code by return

            return null;
        }

        // creating GPS Class object
        gps = new GPSTracker(context);

        // check if GPS location can get
        if (gps.canGetLocation()) {
            Log.d("Your Location", "latitude:" + gps.getLatitude() + ", longitude: " + gps.getLongitude());
        } else {
            // Can't get user's current location
            alert.showInternetGpsAlert(context, "GPS Status", "Couldn't get location information. Please enable GPS", getActivity());
            // stop executing code by return
            return null;
        }

        // Getting listview
        lv = view.findViewById(R.id.list);

        // button show on map

        // calling background Async task to load Google Places
        // After getting places from Google all the data is shown in listview
        new LoadPlaces(gps.getLatitude(), gps.getLongitude()).execute();


        /**
         * ListItem click event
         * On selecting a listitem FragmentMaps is launched
         * */
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting values from selected ListItem
                String placeId = ((TextView) view.findViewById(R.id.reference)).getText().toString();


				FragmentMaps fragment = new FragmentMaps();
				Bundle arguments = new Bundle();
				arguments.putString( "placeId" , placeId);
				fragment.setArguments(arguments);
				final FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.replace(R.id.frame_container, fragment );
				ft.commit();
                // Starting new intent
          /*      Intent in = new Intent(context,
                        FragmentMaps.class);

                // Sending place refrence id to single place activity
                // place refrence id used to get "Place full details"
                in.putExtra(KEY_REFERENCE, reference);
                startActivity(in);*/
            }
        });
        return view;
    }

	/**
	 * Background Async Task to Load Google places
	 * */
	class LoadPlaces extends AsyncTask<String, String, String> {

	    double lat,lngl;
	    public  LoadPlaces(double latitude,double longitude){
	        lat= latitude;
	        lngl=longitude;
        }
		/**
		 * Before starting background thread Show Progress Dialog
		 * */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(context);
			pDialog.setMessage(Html.fromHtml("<b>Search</b><br/>Loading Places..."));
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		/**
		 * getting Places JSON
		 * */
		protected String doInBackground(String... args) {
			// creating Places class object
			googlePlaces = new GooglePlaces();
			
			try {
				// Separeate your place types by PIPE symbol "|"
				// If you want all types places make it as null
				// Check list of types supported by google
				// 
				String types = "cafe|restaurant"; // Listing places only cafes, restaurants
				
				// Radius in meters - increase this value if you don't find any places
				double radius = 1000; // 1000 meters 
				
				// get nearest places
                nearPlaces = googlePlaces.search(lat, lngl, radius, types);
				/*nearPlaces = googlePlaces.search(gps.getLatitude(),
						gps.getLongitude(), radius, types);
				*/

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * After completing background task Dismiss the progress dialog
		 * and show the data in UI
		 * Always use runOnUiThread(new Runnable()) to update UI from background
		 * thread, otherwise you will get error
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
					// Get json response status
					String status = nearPlaces.status;
					
					// Check for all possible status
					if(status.equals("OK")){
						// Successfully got places details
                        placesListItems = new ArrayList<HashMap<String,String>>();
						if (nearPlaces.results != null) {
							// loop through each place
							for (Place p : nearPlaces.results) {
								HashMap<String, String> map = new HashMap<String, String>();
								
								// Place reference won't display in listview - it will be hidden
								// Place reference is used to get "place full details"
								map.put(KEY_REFERENCE, p.reference);
								
								// Place name
								map.put(KEY_NAME, p.name);
								
								
								// adding HashMap to ArrayList
								placesListItems.add(map);
							}
							// list adapter
							ListAdapter adapter = new SimpleAdapter(context, placesListItems,
					                R.layout.list_item,
					                new String[] { KEY_REFERENCE, KEY_NAME}, new int[] {
					                        R.id.reference, R.id.name });
							
							// Adding data into listview
							lv.setAdapter(adapter);
						}
					}
					else if(status.equals("ZERO_RESULTS")){
						// Zero results found
						alert.showAlertDialog(context, "Near Places",
								"Sorry no places found. Try to change the types of places",
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
				}
			});

		}

	}

     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (requestCode == PLACE_PICKER_REQUEST) {
             if (resultCode == RESULT_OK) {
                 com.google.android.gms.location.places.Place place = PlaceAutocomplete.getPlace(context, data);
                 Log.i("place", "Place: " + place.getName());
                 editTextSearch.setText(place.getName());
                 new LoadPlaces(place.getLatLng().latitude,place.getLatLng().longitude).execute();


             } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                 Status status = PlaceAutocomplete.getStatus(context, data);
                 // TODO: Handle the error.
                 Log.i("place", status.getStatusMessage());

             } else if (resultCode == RESULT_CANCELED) {
                 // The user canceled the operation.
             }
         }
    }

/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nearby_fragment, menu);
		return true;
	}
*/

	

}
