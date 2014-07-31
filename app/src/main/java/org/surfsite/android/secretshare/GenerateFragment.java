package org.surfsite.android.secretshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.print.PrintHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tiemens.secretshare.engine.SecretShare;
import com.tiemens.secretshare.engine.SecretShare.ShareInfo;

import java.math.BigInteger;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GenerateFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GenerateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GenerateFragment extends Fragment implements FragmentSupport {
	private static final String ARG_N = "n";
	private static final String ARG_K = "k";
	private static final String ARG_CLEARTEXT = "cleartext";
	private int n;
	private int k;
	private String cleartext;
	private GenerateSharesTask generateSharesTask = null;
	private OnFragmentInteractionListener mListener;

	public GenerateFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param n         number of secrets.
	 * @param k         number of needed secrets.
	 * @param cleartext text to split.
	 * @return A new instance of fragment GenerateFragment.
	 */
	public static GenerateFragment newInstance(int n, int k, String cleartext) {
		GenerateFragment fragment = new GenerateFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_K, k);
		args.putInt(ARG_N, n);
		args.putString(ARG_CLEARTEXT, cleartext);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			n = getArguments().getInt(ARG_N);
			k = getArguments().getInt(ARG_K);
			cleartext = getArguments().getString(ARG_CLEARTEXT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_generate, container, false);
		return rootView;
	}

	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (generateSharesTask != null)
			generateSharesTask.cancel(true);
		else {
			generateSharesTask = new GenerateSharesTask(n, k, cleartext);
			generateSharesTask.execute();
		}
	}

	@Override
	public void onStop() {
		if (generateSharesTask != null)
			generateSharesTask.cancel(false);
		generateSharesTask = null;
		super.onStop();
	}

	public boolean mayBackPress() {
		if (generateSharesTask != null) {
			return generateSharesTask.isFinished();

//			generateSharesTask.cancel(false);
//			generateSharesTask = null;
		}
		return true;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		public void onFragmentInteraction(Uri uri);
	}

	private class GenerateSharesTask extends AsyncTask<Void, Void, Void> {
		private final Activity activity;
		private TextView tv;
		private List<SecretShare.ShareInfo> pieces;
		private int n;
		private int k;
		private String cleartext;
		private SecretShare.PublicInfo publicInfo;
		private boolean finished = false;
		private Bitmap qrCodeBitmap;
		private ImageView qrCodeView;
		private BigInteger secretInteger;

		public GenerateSharesTask(int n, int k, String cleartext) {
			activity = getActivity();
			this.tv = (TextView) activity.findViewById(R.id.generate_status);
			this.n = n;
			this.k = k;
			this.cleartext = cleartext;
			secretInteger = Renderer.stringToSecret(cleartext);
			this.tv.setText("Generating shared secrets for " + secretInteger.bitLength() / 8
					+ " bytes. Please Wait. This can take a long time.");
		}

		public boolean isFinished() {
			return finished;
		}

		@Override
		protected Void doInBackground(Void... params) {
			final BigInteger secretInteger = Renderer.stringToSecret(cleartext);
			final BigInteger modulus;

			modulus = SecretShare.createAppropriateModulusForSecret(secretInteger);
			publicInfo = new SecretShare.PublicInfo(n,
					k,
					modulus,
					"test");
			final SecretShare.SplitSecretOutput splitSecretOutput = new SecretShare(publicInfo)
					.split(secretInteger);
			pieces = splitSecretOutput.getShareInfos();
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			super.onPostExecute(nothing);
			String[] out = new String[pieces.size()];
			tv.setText("");
			for (int i = 0; i < out.length; i++) {
				final ShareInfo piece = pieces.get(i);

				final String data = Renderer.encodeShareInfo(piece);

				out[i] = "ssss-android:" + piece.getX() + "/" + k + ":" + n + "-"
						+ publicInfo.getDescription()
						+ publicInfo.getPrimeModulus() + ":" + piece.getShare();

				tv.append(out[i] + "\n");
				tv.append("B64Len: " + data.length() + "\n");
				tv.append(data + "\n");

				if (i == 0) {
					View view = activity.getLayoutInflater().inflate(R.layout.address_qr, null);
					final TextView tv = (TextView) view.findViewById(R.id.secret_text);
					tv.setText(out[i]);

					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setTitle("ssss-1");
					builder.setView(view);
					if (PrintHelper.systemSupportsPrint()) {
						builder.setPositiveButton(R.string.print, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Renderer.printCode(activity, "sss-1", data);
							}
						});
						builder.setNegativeButton(android.R.string.cancel, null);
					} else {
						builder.setPositiveButton(android.R.string.ok, null);
					}

					builder.show();
				}

			}
			this.finished = true;
		}
	}
}
