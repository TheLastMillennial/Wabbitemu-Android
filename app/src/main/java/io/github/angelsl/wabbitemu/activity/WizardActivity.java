package io.github.angelsl.wabbitemu.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.ViewAnimator;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.calc.CalculatorManager;
import io.github.angelsl.wabbitemu.calc.FileLoadedCallback;
import io.github.angelsl.wabbitemu.utils.ErrorUtils;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.OSDownloader;
import io.github.angelsl.wabbitemu.utils.ViewUtils;
import io.github.angelsl.wabbitemu.wizard.OnWizardFinishedListener;
import io.github.angelsl.wabbitemu.wizard.WizardController;
import io.github.angelsl.wabbitemu.wizard.controller.BrowseOsPageController;
import io.github.angelsl.wabbitemu.wizard.controller.BrowseRomPageController;
import io.github.angelsl.wabbitemu.wizard.controller.CalcModelPageController;
import io.github.angelsl.wabbitemu.wizard.controller.ChooseOsPageController;
import io.github.angelsl.wabbitemu.wizard.controller.LandingPageController;
import io.github.angelsl.wabbitemu.wizard.controller.OsDownloadPageController;
import io.github.angelsl.wabbitemu.wizard.controller.OsPageController;
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData;
import io.github.angelsl.wabbitemu.wizard.view.BrowseOsPageView;
import io.github.angelsl.wabbitemu.wizard.view.BrowseRomPageView;
import io.github.angelsl.wabbitemu.wizard.view.ChooseOsPageView;
import io.github.angelsl.wabbitemu.wizard.view.LandingPageView;
import io.github.angelsl.wabbitemu.wizard.view.ModelPageView;
import io.github.angelsl.wabbitemu.wizard.view.OsDownloadPageView;
import io.github.angelsl.wabbitemu.wizard.view.OsPageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WizardActivity extends Activity {

    private final CalculatorManager mCalcManager = CalculatorManager.getInstance();

    private WizardController mWizardController;
	private String mCreatedFilePath;
	private boolean mIsWizardFinishing;

	private OSDownloader mOsDownloader;

    @Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.wizard);

		final ViewAnimator viewAnimator = ViewUtils.findViewById(this, R.id.viewFlipper, ViewAnimator.class);
		final ViewGroup navContainer = ViewUtils.findViewById(this, R.id.navContainer, ViewGroup.class);
		mWizardController = new WizardController(this, viewAnimator, navContainer, new OnWizardFinishedListener() {

			@Override
			public void onWizardFinishedListener(Object finalData) {
				if (mIsWizardFinishing) {
					return;
				}
				mIsWizardFinishing = true;

				final FinishWizardData finishInfo = (FinishWizardData) finalData;
				if (finishInfo == null) {
					ErrorUtils.showErrorDialog(WizardActivity.this, R.string.errorRomImage);
					return;
				}

				final CalcModel calcModel = finishInfo.getCalcModel();

				if (finishInfo.shouldDownloadOs()) {
					tryDownloadAndCreateRom(calcModel, finishInfo.getDownloadCode(), finishInfo.getOsDownloadUrl());
				} else if (calcModel == CalcModel.NO_CALC) {
					finishSuccess(finishInfo.getFilePath());
				} else {
					createRomCopyOs(calcModel, finishInfo.getFilePath());
				}
			}
		});

		final LandingPageView landingPageView = ViewUtils.findViewById(this, R.id.landing_page, LandingPageView.class);
		mWizardController.registerView(R.id.landing_page, new LandingPageController(landingPageView));

		final ModelPageView modelPageView = ViewUtils.findViewById(this, R.id.model_page, ModelPageView.class);
		mWizardController.registerView(R.id.model_page, new CalcModelPageController(modelPageView));

		final ChooseOsPageView chooseOsView = ViewUtils.findViewById(this, R.id.choose_os_page, ChooseOsPageView.class);
		mWizardController.registerView(R.id.choose_os_page, new ChooseOsPageController(chooseOsView));

		final OsPageView osPageView = ViewUtils.findViewById(this, R.id.os_page, OsPageView.class);
		mWizardController.registerView(R.id.os_page, new OsPageController(osPageView));

		final OsDownloadPageView osDownloadPageView = ViewUtils.findViewById(this, R.id.os_download_page,
				OsDownloadPageView.class);
		mWizardController.registerView(R.id.os_download_page, new OsDownloadPageController(osDownloadPageView));

		final BrowseOsPageView browseOsPageView = ViewUtils.findViewById(this, R.id.browse_os_page,
				BrowseOsPageView.class);
		mWizardController.registerView(R.id.browse_os_page, new BrowseOsPageController(browseOsPageView,
				getFragmentManager()));

		final BrowseRomPageView browseRomPageView = ViewUtils.findViewById(this, R.id.browse_rom_page,
				BrowseRomPageView.class);
		mWizardController.registerView(R.id.browse_rom_page, new BrowseRomPageController(browseRomPageView,
				getFragmentManager()));
	}

	@Override
	protected void onPause() {
		super.onPause();
		cancelDownloadTask();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancelDownloadTask();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.wizard, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.helpMenuItem:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog dialog = builder.setMessage(R.string.aboutRomDescription)
					.setTitle(R.string.aboutRomTitle)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							dialog.dismiss();
						}
					})
					.create();
			dialog.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		if (!mWizardController.movePreviousPage()) {
			super.onBackPressed();
		}
	}

	private void tryDownloadAndCreateRom(CalcModel calcModel, String downloadCode, String osDownloadUrl) {
		if (mOsDownloader != null && mOsDownloader.getStatus() == Status.RUNNING) {
			throw new IllegalStateException("Invalid state, download running");
		}

		if (!isOnline()) {
			final AlertDialog dialog = new AlertDialog.Builder(WizardActivity.this)
					.setMessage(getResources().getString(R.string.noNetwork))
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							mIsWizardFinishing = false;
							dialog.dismiss();
						}
					})
					.create();
			dialog.show();
			return;
		}

		createRomDownloadOs(calcModel, downloadCode, osDownloadUrl);
	}

	private boolean isOnline() {
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	private void createRomCopyOs(final CalcModel calcModel, final String osFilePath) {
		final String bootPagePath = extractBootpage(calcModel);
		if (bootPagePath == null) {
			finishRomError();
			return;
		}

        mCalcManager.createRom(osFilePath, bootPagePath, mCreatedFilePath, calcModel,
                new FileLoadedCallback() {
                    @Override
                    public void onFileLoaded(int error) {
                        if (error == 0) {
                            finishSuccess(mCreatedFilePath);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finishRomError();
                                }
                            });
                        }
                    }
                });
    }

	private String extractBootpage(CalcModel calcModel) {
		final Resources resources = getResources();
		final File cache = getCacheDir();
		mCreatedFilePath = cache.getAbsolutePath() + "/";

		final File bootPagePath;
		try {
			bootPagePath = File.createTempFile("boot", ".hex", cache);
		} catch (final IOException e) {
			return null;
		}

		final InputStream bootStream;
		switch (calcModel) {
		case TI_73:
			mCreatedFilePath += resources.getString(R.string.ti73);
			bootStream = resources.openRawResource(R.raw.bf73);
			break;
		default:
		case TI_83P:
			mCreatedFilePath += resources.getString(R.string.ti83p);
			bootStream = resources.openRawResource(R.raw.bf83pbe);
			break;
		case TI_83PSE:
			mCreatedFilePath += resources.getString(R.string.ti83pse);
			bootStream = resources.openRawResource(R.raw.bf83pse);
			break;
		case TI_84P:
			mCreatedFilePath += resources.getString(R.string.ti84p);
			bootStream = resources.openRawResource(R.raw.bf84pbe);
			break;
		case TI_84PSE:
			mCreatedFilePath += resources.getString(R.string.ti84pse);
			bootStream = resources.openRawResource(R.raw.bf84pse);
			break;
		case TI_84PCSE:
			mCreatedFilePath += resources.getString(R.string.ti84pcse);
			bootStream = resources.openRawResource(R.raw.bf84pcse);
			break;
		}

		mCreatedFilePath += ".rom";

		FileOutputStream outputStream = null;
		try {
			final byte[] buffer = new byte[4096];
			outputStream = new FileOutputStream(bootPagePath);
			while (bootStream.read(buffer) != -1) {
				outputStream.write(buffer, 0, 4096);
			}
		} catch (final IOException e) {
			finishRomError();
		} finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (final IOException e) {
				finishRomError();
			}
		}

		return bootPagePath.getAbsolutePath();
	}

	private void createRomDownloadOs(final CalcModel calcModel, final String downloadCode, final String osDownloadUrl) {
		final String bootPagePath = extractBootpage(calcModel);
		if (bootPagePath == null) {
			finishRomError();
			return;
		}

		final Spinner spinner = (Spinner) findViewById(R.id.osVersionSpinner);
		final int osVersion = spinner.getSelectedItemPosition();
		final File cache = getCacheDir();
		final File osDownloadPath;
		try {
			osDownloadPath = File.createTempFile("tios", ".8xu", cache);
		} catch (final IOException e) {
			return;
		}

		final String osFilePath = osDownloadPath.getAbsolutePath();
		mOsDownloader = new OSDownloader(this, osDownloadUrl, osFilePath, calcModel, downloadCode) {

			@Override
			protected void onPostExecute(final Boolean success) {
				super.onPostExecute(success);
				createRom(success, osFilePath, bootPagePath, calcModel);
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();

				mIsWizardFinishing = false;
			}
		};
		mOsDownloader.execute();
	}

	private void createRom(Boolean success, String osFilePath, String bootPagePath, final CalcModel calcModel) {
		if (success) {
			final CalculatorManager calculatorManager = CalculatorManager.getInstance();
			calculatorManager.createRom(osFilePath, bootPagePath, mCreatedFilePath, calcModel,
					new FileLoadedCallback() {
						@Override
						public void onFileLoaded(int error) {
							if (error == 0) {
								finishSuccess(mCreatedFilePath);
							} else {
								finishRomError();
							}
						}
					});
        } else {
            finishOsError();
        }
	}

	private void finishOsError() {
		showOsError();
	}

	private void finishRomError() {
		showRomError();
	}

	private void finishSuccess(String fileName) {
		final Intent resultIntent = new Intent();
		resultIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName);
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	private void showOsError() {
		mIsWizardFinishing = false;
		ErrorUtils.showErrorDialog(this, R.string.errorOsDownloadDescription);
	}

	private void showRomError() {
		mIsWizardFinishing = false;
		ErrorUtils.showErrorDialog(this, R.string.errorRomCreateDescription);
	}

	private void cancelDownloadTask() {
		if (mOsDownloader != null) {
			mOsDownloader.cancel(true);
			mOsDownloader = null;
		}
	}
}
