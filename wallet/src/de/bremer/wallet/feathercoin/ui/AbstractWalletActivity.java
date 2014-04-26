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

import java.util.Locale;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.schildbach.wallet.feathercoin.Constants;
import de.schildbach.wallet.feathercoin.WalletApplication;
import de.schildbach.wallet.feathercoin.R;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractWalletActivity extends SherlockFragmentActivity
{
	private WalletApplication application;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		application = (WalletApplication) getApplication();
	}

	protected WalletApplication getWalletApplication()
	{
		return application;
	}

	protected final void toast(final String text, final Object... formatArgs)
	{
		toast(text, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	protected final void longToast(final String text, final Object... formatArgs)
	{
		toast(text, 0, Toast.LENGTH_LONG, formatArgs);
	}

	protected final void toast(final String text, final int imageResId, final int duration, final Object... formatArgs)
	{
		final View view = getLayoutInflater().inflate(R.layout.transient_notification, null);
		TextView tv = (TextView) view.findViewById(R.id.transient_notification_text);
		tv.setText(String.format(text, formatArgs));
		tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

		final Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(duration);
		toast.show();
	}

	protected final void toast(final int textResId, final Object... formatArgs)
	{
		toast(textResId, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	protected final void longToast(final int textResId, final Object... formatArgs)
	{
		toast(textResId, 0, Toast.LENGTH_LONG, formatArgs);
	}

	protected final void toast(final int textResId, final int imageResId, final int duration, final Object... formatArgs)
	{
		final View view = getLayoutInflater().inflate(R.layout.transient_notification, null);
		TextView tv = (TextView) view.findViewById(R.id.transient_notification_text);
		tv.setText(getString(textResId, formatArgs));
		tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

		final Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(duration);
		toast.show();
	}

	protected void parseErrorDialog(final String uri)
	{
		final Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.send_coins_uri_parse_error_title);
		dialog.setMessage(uri);
		dialog.setNeutralButton(R.string.button_dismiss, null);
		dialog.show();
	}

	protected final static String languagePrefix()
	{
		final String language = Locale.getDefault().getLanguage();
		if ("de".equals(language))
			return "_de";
		else if ("cs".equals(language))
			return "_cs";
		else if ("el".equals(language))
			return "_el";
		else if ("es".equals(language))
			return "_es";
		else if ("fr".equals(language))
			return "_fr";
		else if ("it".equals(language))
			return "_it";
		else if ("nl".equals(language))
			return "_nl";
		else if ("pl".equals(language))
			return "_pl";
		else if ("ru".equals(language))
			return "_ru";
		else if ("sv".equals(language))
			return "_sv";
		else if ("tr".equals(language))
			return "_tr";
		else if ("zh".equals(language))
			return "_zh";
		else
			return "";
	}

	protected void touchLastUsed()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putLong(Constants.PREFS_KEY_LAST_USED, System.currentTimeMillis()).commit();
	}
}
