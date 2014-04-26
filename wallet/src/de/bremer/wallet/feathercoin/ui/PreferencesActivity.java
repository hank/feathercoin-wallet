/*
 * Copyright 2011-2013 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.feathercoin.ui;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.schildbach.wallet.feathercoin.Constants;
import de.schildbach.wallet.feathercoin.WalletApplication;
import de.schildbach.wallet.feathercoin.util.CrashReporter;
import de.schildbach.wallet.feathercoin.R;

/**
 * @author Andreas Schildbach
 */
public final class PreferencesActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener
{
	private WalletApplication application;
	//private Preference trustedPeerPreference;
	//private Preference trustedPeerOnlyPreference;

	private static final String PREFS_KEY_REPORT_ISSUE = "report_issue";
	private static final String PREFS_KEY_INITIATE_RESET = "initiate_reset";

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		application = (WalletApplication) getApplication();

		addPreferencesFromResource(R.xml.preferences);
/*
		trustedPeerPreference = findPreference(Constants.PREFS_KEY_TRUSTED_PEER);
		trustedPeerPreference.setOnPreferenceChangeListener(this);

		trustedPeerOnlyPreference = findPreference(Constants.PREFS_KEY_TRUSTED_PEER_ONLY);
		trustedPeerOnlyPreference.setOnPreferenceChangeListener(this);
*/
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		//final String trustedPeer = prefs.getString(Constants.PREFS_KEY_TRUSTED_PEER, "").trim();
		//updateTrustedPeer(trustedPeer);
	}

	@Override
	protected void onDestroy()
	{
		//trustedPeerPreference.setOnPreferenceChangeListener(null);

		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference)
	{
		final String key = preference.getKey();

		if (PREFS_KEY_REPORT_ISSUE.equals(key))
		{
			final ReportIssueDialogBuilder dialog = new ReportIssueDialogBuilder(this, R.string.report_issue_dialog_title_issue,
					R.string.report_issue_dialog_message_issue)
			{
				@Override
				protected CharSequence subject()
				{
					return Constants.REPORT_SUBJECT_ISSUE + " " + application.applicationVersionName();
				}

				@Override
				protected CharSequence collectStackTrace()
				{
					return null;
				}

				@Override
				protected CharSequence collectDeviceInfo() throws IOException
				{
					final StringBuilder deviceInfo = new StringBuilder();
					CrashReporter.appendDeviceInfo(deviceInfo, PreferencesActivity.this);
					return deviceInfo;
				}

				@Override
				protected CharSequence collectApplicationLog() throws IOException
				{
					final StringBuilder applicationLog = new StringBuilder();
					CrashReporter.appendApplicationLog(applicationLog);
					if (applicationLog.length() > 0)
						return applicationLog;
					else
						return null;
				}

				@Override
				protected CharSequence collectWalletDump()
				{
					return application.getWallet().toString(false, null);
				}
			};

			dialog.show();

			return true;
		}
		else if (PREFS_KEY_INITIATE_RESET.equals(key))
		{
			final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.preferences_initiate_reset_title);
			dialog.setMessage(R.string.preferences_initiate_reset_dialog_message);
			dialog.setPositiveButton(R.string.preferences_initiate_reset_dialog_positive, new OnClickListener()
			{
				public void onClick(final DialogInterface dialog, final int which)
				{
					application.resetBlockchain();
					finish();
				}
			});
			dialog.setNegativeButton(R.string.button_dismiss, null);
			dialog.show();

			return true;
		}

		return false;
	}

	public boolean onPreferenceChange(final Preference preference, final Object newValue)
	{
/*		if (preference.equals(trustedPeerPreference))
		{
			application.stopBlockchainService();
			updateTrustedPeer((String) newValue);
		}
		else if (preference.equals(trustedPeerOnlyPreference))
		{
			application.stopBlockchainService();
		}*/

		return true;
	}

	private void updateTrustedPeer(final String trustedPeer)
	{
/*		if (trustedPeer.length() == 0)
		{
			trustedPeerPreference.setSummary(R.string.preferences_trusted_peer_summary);
			trustedPeerOnlyPreference.setEnabled(false);
		}
		else
		{
			trustedPeerPreference.setSummary(trustedPeer);
			trustedPeerOnlyPreference.setEnabled(true);
		}*/
	}
}
