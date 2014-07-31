package org.surfsite.android.secretshare;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link org.surfsite.android.secretshare.InputFragment.OnInputFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InputFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InputFragment extends Fragment {
	private static final String ARG_N = "n";
	private static final String ARG_K = "k";
	private static final String ARG_CLEARTEXT = "cleartext";
	private OnInputFragmentInteractionListener mListener;
	private int n = 3;
	private int k = 2;
	private String cleartext;

	public InputFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InputFragment.
	 */
	public static InputFragment newInstance(int n, int k, String cleartext) {
		InputFragment fragment = new InputFragment();
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
	public void onSaveInstanceState(Bundle args) {
		args.putInt(ARG_K, k);
		args.putInt(ARG_N, n);
		args.putString(ARG_CLEARTEXT, cleartext);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_input, container, false);
		final Button generateButton = (Button) rootView.findViewById(R.id.generate_button);
		assert (generateButton != null);
		generateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onGenerateButtonPressed();
			}
		});
		final EditText clearEditText = (EditText) rootView.findViewById(R.id.clearEditText);
		clearEditText.setText(cleartext);

		//set on focus to force keyboard
		clearEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					//open keyboard
					((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(v,
							InputMethodManager.SHOW_FORCED);
				} else {
					//close keyboard
					((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
							v.getWindowToken(), 0);
				}
			}
		});

		//Set on click listener to clear focus
		clearEditText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View clickedView) {
				clickedView.clearFocus();
				clickedView.requestFocus();
			}
		});

		return rootView;
	}

	public void onGenerateButtonPressed() {
		if (mListener != null) {
			final EditText clearEditText = (EditText) getActivity().findViewById(R.id.clearEditText);

			cleartext = clearEditText.getText().toString();
			if (cleartext.isEmpty())
				return;

			mListener.generateSecret(n, k, cleartext);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnInputFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnInputFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
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
	public interface OnInputFragmentInteractionListener {
		public void generateSecret(int n, int k, String cleartext);
	}

}
