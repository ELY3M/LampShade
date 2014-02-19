package com.kuxhausen.huemore.nfc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.kuxhausen.huemore.HelpActivity;
import com.kuxhausen.huemore.MainActivity;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.SerializedEditorActivity;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.InternalArguments;

public class NfcWriterActivity extends SerializedEditorActivity implements
		OnClickListener {
	private Button sendButton;
	private NfcAdapter nfcAdapter;
	PendingIntent pendingIntent;
	IntentFilter writeTagFilters[];
	boolean writeMode;
	Tag myTag;
	Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.nfc_writer);
		super.onCreate(savedInstanceState);
		context = this;
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);

		sendButton = (Button) this.findViewById(R.id.writeToTagButton);
		sendButton.setOnClickListener(this);

		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
		writeTagFilters = new IntentFilter[] { tagDetected };

		setTitle(R.string.nfc);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.action_write_nfc, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				this.startActivity(new Intent(this,MainActivity.class));
				return true;
			case R.id.action_help:
				Intent i = new Intent(this, HelpActivity.class);
				i.putExtra(InternalArguments.HELP_PAGE, this.getResources().getString(R.string.help_title_nfc));
				this.startActivity(i);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.writeToTagButton:
			preview();
			try {
				if (myTag == null) {
					Toast.makeText(context,
							context.getString(R.string.nfc_tag_not_detected),
							Toast.LENGTH_SHORT).show();
				} else {
					write(getSerializedByValue(), myTag);
					Toast.makeText(context,
							context.getString(R.string.nfc_tag_write_success),
							Toast.LENGTH_SHORT).show();
				}
			} catch (IOException e) {
				Toast.makeText(context,
						context.getString(R.string.nfc_tag_write_fail),
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			} catch (FormatException e) {
				Toast.makeText(context,
						context.getString(R.string.nfc_tag_write_fail),
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			break;
		}
	}

	private void write(String text, Tag tag) throws IOException,
			FormatException {
		try {
			NdefRecord[] records = { createRecord(text) };
			NdefMessage message = new NdefMessage(records);
			// Get an instance of Ndef for the tag.
			Ndef ndef = Ndef.get(tag);
			// Enable I/O
			ndef.connect();
			// Write the message
			ndef.writeNdefMessage(message);
			// Close the connection
			ndef.close();
		} catch (java.lang.NullPointerException e) {
			Toast.makeText(context,
					context.getString(R.string.nfc_tag_not_supported),
					Toast.LENGTH_LONG).show();
		}
	}

	private NdefRecord createRecord(String text)
			throws UnsupportedEncodingException {
		byte[] uriField = text.getBytes(Charset.forName("US-ASCII"));
		byte[] payload = new byte[uriField.length + 1]; // add 1 for the URI
														// Prefix
		payload[0] = 0x01; // prefixes http://www. to the URI
		System.arraycopy(uriField, 0, payload, 1, uriField.length); // appends
																	// URI to
																	// payload
		NdefRecord rtdUriRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_URI, new byte[0], payload);

		return rtdUriRecord;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Toast.makeText(this, this.getString(R.string.nfc_tag_detected),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		WriteModeOff();
	}

	@Override
	public void onResume() {
		super.onResume();
		WriteModeOn();
	}

	private void WriteModeOn() {
		writeMode = true;
		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					writeTagFilters, null);
	}

	private void WriteModeOff() {
		writeMode = false;
		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

}
