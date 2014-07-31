package org.surfsite.android.secretshare;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tiemens.secretshare.engine.SecretShare;
import com.tiemens.secretshare.engine.SecretShare.ShareInfo;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Hashtable;
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
	private final static QRCodeWriter sQRCodeWriter = new QRCodeWriter();
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

	static BigInteger stringToBigInteger(String in) {
		BigInteger bigint;
		try {
			bigint = new BigInteger(in.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
			bigint = BigInteger.ZERO;
		}
		return bigint;
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

		TextView tv = (TextView) getActivity().findViewById(R.id.generate_status);

		generateSharesTask = new GenerateSharesTask(tv, n, k, cleartext);
		generateSharesTask.execute();
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
		private TextView tv;
		private List<SecretShare.ShareInfo> pieces;
		private int n;
		private int k;
		private String cleartext;
		private SecretShare.PublicInfo publicInfo;
		private boolean finished = false;

		public GenerateSharesTask(TextView tv, int n, int k, String cleartext) {
			this.tv = tv;
			this.n = n;
			this.k = k;
			this.cleartext = cleartext;
			this.tv.setText("Generating shared secrets for " + cleartext.length()
					+ " chars. Please Wait. This can take a long time.");
		}

		private Bitmap createBitmap(byte[] content, final int size) {
			final Hashtable<EncodeHintType, Object> hints =
					new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
			hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");
			BitMatrix result;

			String data;
			try {
				data = new String(content, "ISO-8859-1");
			} catch (Exception ex) {
				ex.printStackTrace();
				data = new String(content);
			}

			try {
				result = sQRCodeWriter.encode(data,
						BarcodeFormat.QR_CODE,
						size,
						size,
						hints);
			} catch (WriterException ex) {
				return null;
			}

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] =
							result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
				}
			}

			final Bitmap bitmap =
					Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

			return bitmap;
		}

		public boolean isFinished() {
			return finished;
		}

		@Override
		protected Void doInBackground(Void... params) {
			final BigInteger secretInteger = stringToBigInteger(cleartext);
			final BigInteger modulus;

			modulus = SecretShare.createAppropriateModulusForSecret(secretInteger);
			publicInfo = new SecretShare.PublicInfo(n,
					k,
					modulus,
					null);
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

				byte[] bmodulus = publicInfo.getPrimeModulus().toByteArray();
				byte[] bshare = piece.getShare().toByteArray();
				int blen = 4 + 4 + 4 + 4 + 4 + bmodulus.length + bshare.length;
				ByteBuffer bencoded = ByteBuffer.allocate(blen);
				bencoded.putInt(n).putInt(k).putInt(piece.getX());
				bencoded.putInt(bmodulus.length).put(bmodulus);
				bencoded.putInt(bshare.length).put(bshare);

				tv.append("Length: " + blen + "\n");

				out[i] = n + ":" + k + ":" + piece.getX() + ":"
						+ publicInfo.getPrimeModulus() + ":" + piece.getShare();

				tv.append("OutLen: " + out[i].length() + "\n");
				tv.append(out[i] + "\n");
				tv.append(bencoded + "\n0x");
				for (byte b : bencoded.array()) {
					tv.append(String.format("%x", b));
				}
				tv.append("\n");
				String bencoded64 = Base64.encodeToString(bencoded.array(), Base64.DEFAULT);
				tv.append("B64Len: " + bencoded64.length() + "\n");
				tv.append(bencoded64 + "\n");
				tv.append(bencoded + "\n\n");
			}
			this.finished = true;
		}
	}
}
