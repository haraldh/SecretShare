package org.surfsite.android.secretshare;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity
		implements
		GenerateFragment.OnFragmentInteractionListener,
		InputFragment.OnInputFragmentInteractionListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		new LinuxSecureRandom(); // init proper random number generator

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(R.id.container, new InputFragment());
			//transaction.addToBackStack(null);
			transaction.commit();
		}
		// QRCODE
		// IntentIntegrator test;
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void generateSecret(int n, int k, String cleartext) {
		Fragment generateFragment = new GenerateFragment().newInstance(n, k, cleartext);
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.container, generateFragment, "GENERATEFRAGMENT");
		transaction.addToBackStack(null);
		transaction.commit();
	}

	public void onFragmentInteraction(Uri uri) {
	}


	@Override
	public void onBackPressed() {
		try {
			final FragmentSupport fragment = (FragmentSupport) getFragmentManager().findFragmentByTag("GENERATEFRAGMENT");

			if (fragment == null || fragment.mayBackPress()) {
				super.onBackPressed();
			}
		} catch (ClassCastException e) {
			super.onBackPressed();
		}

	}
}
