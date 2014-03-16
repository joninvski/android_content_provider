package course.labs.contentproviderlab;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import course.labs.contentproviderlab.provider.PlaceBadgesContract;

public class PlaceViewActivity extends ListActivity implements LocationListener, LoaderCallbacks<Cursor> {
	private static final long FIVE_MINS = 5 * 60 * 1000;

	private static String TAG = "Lab-ContentProvider";

	// The last valid location reading
	private Location mLastLocationReading;

	// The ListView's adapter
	// private PlaceViewAdapter mAdapter;
	private PlaceViewAdapter mCursorAdapter;

	// default minimum time between new location readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// Reference to the LocationManager
	private LocationManager mLocationManager;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO - Set up the app's user interface
		// This class is a ListActivity, so it has its own ListView
		// ???

		// TODO2 - add a footerView to the ListView
		// You can use footer_view.xml to define the footer
		getListView().setFooterDividersEnabled(true);
		TextView footerView = null;
		footerView = (TextView) getLayoutInflater().inflate(R.layout.footer_view, null);
		this.getListView().addFooterView(footerView);

		// TODO2 - When the footerView's onClick() method is called, it must
		// issue the
		// following log call
		// log("Entered footerView.OnClickListener.onClick()");
		footerView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				log("Entered footerView.OnClickListener.onClick()");

				clickedFooter();
			}
		});

		// TODO - Create and set empty PlaceViewAdapter
		// ListView's adapter should be a PlaceViewAdapter called mCursorAdapter
		mCursorAdapter = new PlaceViewAdapter(getApplicationContext(), null, 0);
		setListAdapter(mCursorAdapter);

		// TODO - Initialize a CursorLoader
		int id = 0;
		Bundle bundle = null;
		LoaderCallbacks<Cursor> callback = this;
		getLoaderManager().initLoader(id, bundle, callback);

		// Initialize the location manager
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (mLocationManager == null)
			finish();
	}

	private void clickedFooter() {

		// footerView must respond to user clicks.
		// Must handle 3 cases:
		// 1) The current location is new - download new Place Badge. Issue the
		// following log call:
		// log("Starting Place Download");
		if (mLastLocationReading != null) {
			if (isNewLocation(mLastLocationReading)) {
				PlaceDownloaderTask downloader = new PlaceDownloaderTask(this);
				downloader.execute(mLastLocationReading);
				Toast.makeText(getApplicationContext(), "Checking new place data", Toast.LENGTH_SHORT).show();
				log("Starting Place Download");
			}

			// 2) The current location has been seen before - issue Toast
			// message.
			// Issue the following log call:
			// log("You already have this location badge");
			else {
				log("You already have this location badge");
				Toast.makeText(getApplicationContext(), "You already have this location badge", Toast.LENGTH_SHORT).show();
			}
		}

		// 3) There is no current location - response is up to you. The best
		// solution is to disable the footerView until you have a location.
		// Issue the following log call:
		// log("Location data is not available");
		else {
			log("Location data is not available");
			Toast.makeText(getApplicationContext(), "No location data is available", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean isNewLocation(Location newLocation) {
		List<PlaceRecord> places = mCursorAdapter.getList();
		for (PlaceRecord place : places) {
			Log.d("MINE", "Old location: " + place.getLocation().toString());
			Log.d("MINE", "New location: " + newLocation.toString());
			if (newLocation.distanceTo(place.getLocation()) < mMinDistance) {
				Log.d("MINE", "Not new");
				return false;
			}
			Log.d("MINE", "New");
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		mMockLocationProvider = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, this);

		// TODO2 - Check NETWORK_PROVIDER for an existing location reading.
		// Only keep this last reading if it is fresh - less than 5 minutes old.
		mLastLocationReading = getLastKnownLocation();

		// TODO2 - register to receive location updates from NETWORK_PROVIDER
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
	}

	private Location getLastKnownLocation() {
		List<String> matchingProviders = mLocationManager.getAllProviders();

		for (String provider : matchingProviders) {

			// Note: Uncomment these lines for coursera tests
			if (!provider.equals(LocationManager.NETWORK_PROVIDER))
				continue;

			Location location = mLocationManager.getLastKnownLocation(provider);

			if (location != null) {
				if (age(location) < FIVE_MINS) {
					Log.d("MINE", "The choosen provider was: " + provider);
					return location;
				}
			}
		}
		return null;
	}

	@Override
	protected void onPause() {

		mMockLocationProvider.shutdown();

		// TODO2 - Unregister for location updates
		mLocationManager.removeUpdates(this);

		super.onPause();
	}

	public void addNewPlace(PlaceRecord place) {

		log("Entered addNewPlace()");
		mCursorAdapter.add(place);

	}

	@Override
	public void onLocationChanged(Location currentLocation) {

		Log.d("MINE", "Location has changed to: " + currentLocation.toString());
		// TODO2 - Handle location updates
		// Cases to consider
		// 1) If there is no last location, keep the current location.
		if (mLastLocationReading == null) {
			mLastLocationReading = currentLocation;
		}

		// 2) If the current location is older than the last location, ignore
		// the current location
		else if (currentLocation.getTime() < mLastLocationReading.getTime())
			return;

		// 3) If the current location is newer than the last locations, keep the
		// current location.
		else {
			mLastLocationReading = currentLocation;
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		log("Entered onCreateLoader()");

		// TODO - Create a new CursorLoader and return it
		// String used to filter contacts with empty or missing names or are
		// unstarred
		String select = "((" + PlaceBadgesContract._ID + " NOTNULL))";

		String sortOrder = PlaceBadgesContract._ID + " ASC";
		String[] collumns_to_return = { PlaceBadgesContract.FLAG_BITMAP_PATH, PlaceBadgesContract.COUNTRY_NAME,
				PlaceBadgesContract.PLACE_NAME, PlaceBadgesContract.LAT, PlaceBadgesContract.LON }; // all columns

		return new CursorLoader(this, PlaceBadgesContract.CONTENT_URI, collumns_to_return, select, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> newLoader, Cursor newCursor) {

		// TODO - Swap in the newCursor

		// Swap the new cursor into the List adapter
		mCursorAdapter.swapCursor(newCursor);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> newLoader) {

		// TODO - Swap in a null Cursor
		// set List adapter's cursor to null
		mCursorAdapter.swapCursor(null);

	}

	private long age(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.print_badges:
			ArrayList<PlaceRecord> currData = mCursorAdapter.getList();
			for (int i = 0; i < currData.size(); i++) {
				log(currData.get(i).toString());
			}
			return true;
		case R.id.delete_badges:
			mCursorAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_invalid:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static void log(String msg) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i(TAG, msg);
	}
}
