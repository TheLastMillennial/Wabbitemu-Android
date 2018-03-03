package io.github.angelsl.wabbitemu.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.fragment.BrowseFragment;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected;

public class BrowseActivity extends Activity implements OnBrowseItemSelected {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String regex = intent.getStringExtra(IntentConstants.EXTENSION_EXTRA_REGEX);
		final String description = intent.getStringExtra(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING);

		final Bundle bundle = new Bundle();
		bundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, regex);
		bundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, description);

		final BrowseFragment fragment = new BrowseFragment();
		fragment.setCallback(this);
		fragment.setArguments(bundle);

		setTitle(R.string.selectFile);
		getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
	}

	@Override
	public void onBrowseItemSelected(String fileName) {
		final Intent returnIntent = new Intent();
		returnIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName);
		setResult(Activity.RESULT_OK, returnIntent);
		finish();
	}
}
