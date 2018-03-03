package io.github.angelsl.wabbitemu.wizard.controller;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.fragment.BrowseFragment;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData;
import io.github.angelsl.wabbitemu.wizard.view.BrowseRomPageView;

public class BrowseRomPageController implements WizardPageController {

	private final Context mContext;
	private final FragmentManager mFragmentManager;
	private final OnBrowseItemSelected mBrowseCallback = new OnBrowseItemSelected() {

		@Override
		public void onBrowseItemSelected(String fileName) {
			if (mNavController == null) {
				return;
			}

			mSelectedFileName = fileName;
			mNavController.finishWizard();
		}
	};

	private WizardNavigationController mNavController;
	private String mSelectedFileName;

	public BrowseRomPageController(@NonNull BrowseRomPageView view,
			@NonNull FragmentManager fragmentManager)
	{
		mContext = view.getContext();
		mFragmentManager = fragmentManager;
	}

	@Override
	public void configureButtons(@NonNull WizardNavigationController navController) {
		mNavController = navController;
		navController.hideNextButton();
	}

	@Override
	public boolean hasPreviousPage() {
		return true;
	}

	@Override
	public boolean hasNextPage() {
		return false;
	}

	@Override
	public boolean isFinalPage() {
		return true;
	}

	@Override
	public int getNextPage() {
		throw new IllegalStateException("No next page");
	}

	@Override
	public int getPreviousPage() {
		return R.id.landing_page;
	}

	@Override
	public void onHiding() {
		// no-op
	}

	@Override
	public void onShowing(Object previousData) {
		launchBrowseRom();
	}

	@Override
	public int getTitleId() {
		return R.string.browseRomTitle;
	}

	@Override
	public Object getControllerData() {
		if (mSelectedFileName == null) {
			return null;
		}

		return new FinishWizardData(mSelectedFileName);
	}

	private void launchBrowseRom() {
		final Bundle setupBundle = new Bundle();
		setupBundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, "\\.(rom|sav)");
		setupBundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING,
				mContext.getResources().getString(R.string.browseRomDescription));

		final BrowseFragment fragInfo = new BrowseFragment();
		fragInfo.setCallback(mBrowseCallback);
		fragInfo.setArguments(setupBundle);

		final FragmentTransaction transaction = mFragmentManager.beginTransaction();
		transaction.replace(R.id.browse_rom_page, fragInfo);
		transaction.commit();
	}
}
