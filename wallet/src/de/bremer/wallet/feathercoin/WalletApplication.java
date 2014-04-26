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

package de.schildbach.wallet.feathercoin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.feathercoin.core.Address;
import com.google.feathercoin.core.ECKey;
import com.google.feathercoin.core.Wallet;
import com.google.feathercoin.core.Wallet.AutosaveEventListener;
import com.google.feathercoin.store.WalletProtobufSerializer;

import de.schildbach.wallet.feathercoin.service.BlockchainService;
import de.schildbach.wallet.feathercoin.service.BlockchainServiceImpl;
import de.schildbach.wallet.feathercoin.util.CrashReporter;
import de.schildbach.wallet.feathercoin.util.LinuxSecureRandom;
import de.schildbach.wallet.feathercoin.util.StrictModeWrapper;
import de.schildbach.wallet.feathercoin.util.WalletUtils;
import de.schildbach.wallet.feathercoin.R;

/**
 * @author Andreas Schildbach
 */
public class WalletApplication extends Application
{
	private File walletFile;
	private Wallet wallet;
	private Intent blockchainServiceIntent;
	private Intent blockchainServiceCancelCoinsReceivedIntent;
	private Intent blockchainServiceResetBlockchainIntent;
	private ActivityManager activityManager;

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final String TAG = "Feathercoin"+WalletApplication.class.getSimpleName();

	@Override
	public void onCreate()
	{
        new LinuxSecureRandom();
		try
		{
			StrictModeWrapper.init();
		}
		catch (final Error x)
		{
			Log.i(TAG, "StrictMode not available");
		}

		Log.d(TAG, ".onCreate()");

		super.onCreate();

		CrashReporter.init(getCacheDir());

		activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		blockchainServiceIntent = new Intent(this, BlockchainServiceImpl.class);
		blockchainServiceCancelCoinsReceivedIntent = new Intent(BlockchainService.ACTION_CANCEL_COINS_RECEIVED, null, this,
				BlockchainServiceImpl.class);
		blockchainServiceResetBlockchainIntent = new Intent(BlockchainService.ACTION_RESET_BLOCKCHAIN, null, this, BlockchainServiceImpl.class);

		walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

		migrateWalletToProtobuf();

		loadWalletFromProtobuf();

		backupKeys();

		wallet.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, new WalletAutosaveEventListener());
	}

	private static final class WalletAutosaveEventListener implements AutosaveEventListener
	{
		public boolean caughtException(final Throwable t)
		{
			throw new Error(t);
		}

		public void onBeforeAutoSave(final File file)
		{
		}

		public void onAfterAutoSave(final File file)
		{
			// make wallets world accessible in test mode
			if (Constants.TEST)
				WalletUtils.chmod(file, 0777);
		}
	}

	public Wallet getWallet()
	{
		return wallet;
	}

	private void migrateWalletToProtobuf()
	{
		final File oldWalletFile = getFileStreamPath(Constants.WALLET_FILENAME);

		if (oldWalletFile.exists())
		{
			Log.i(TAG, "found wallet to migrate");

			final long start = System.currentTimeMillis();

			// read
			wallet = restoreWalletFromBackup();

			try
			{
				// write
				protobufSerializeWallet(wallet);

				// delete
				oldWalletFile.delete();

				Log.i(TAG, "wallet migrated: '" + oldWalletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
			}
			catch (final IOException x)
			{
				throw new Error("cannot migrate wallet", x);
			}
		}
	}

	private void loadWalletFromProtobuf()
	{
		if (walletFile.exists())
		{
			final long start = System.currentTimeMillis();

			FileInputStream walletStream = null;

			try
			{
				walletStream = new FileInputStream(walletFile);

				wallet = new WalletProtobufSerializer().readWallet(walletStream);

				Log.i(TAG, "wallet loaded from: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
			}
			catch (final IOException x)
			{
				x.printStackTrace();

				Toast.makeText(WalletApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

				wallet = restoreWalletFromBackup();
			}
			catch (final IllegalStateException x)
			{
				x.printStackTrace();

				Toast.makeText(WalletApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

				wallet = restoreWalletFromBackup();
			}
			finally
			{
				if (walletStream != null)
				{
					try
					{
						walletStream.close();
					}
					catch (final IOException x)
					{
						x.printStackTrace();
					}
				}
			}

			if (!wallet.isConsistent())
			{
				Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();

				wallet = restoreWalletFromBackup();
			}

			if (!wallet.getParams().equals(Constants.NETWORK_PARAMETERS))
				throw new Error("bad wallet network parameters: " + wallet.getParams().getId());
		}
		else
		{
			try
			{
				wallet = restoreWalletFromSnapshot();
			}
			catch (final FileNotFoundException x)
			{
				wallet = new Wallet(Constants.NETWORK_PARAMETERS);
				wallet.addKey(new ECKey());

				try
				{
					protobufSerializeWallet(wallet);
					Log.i(TAG, "wallet created: '" + walletFile + "'");
				}
				catch (final IOException x2)
				{
					throw new Error("wallet cannot be created", x2);
				}
			}
		}

		// this check is needed so encrypted wallets won't get their private keys removed accidently
		for (final ECKey key : wallet.getKeys())
			if (key.getPrivKeyBytes() == null)
				throw new Error("found read-only key, but wallet is likely an encrypted wallet from the future");
	}

	private Wallet restoreWalletFromBackup()
	{
		try
		{
			final Wallet wallet = readKeys(openFileInput(Constants.WALLET_KEY_BACKUP_BASE58));
			resetBlockchain();

			Toast.makeText(this, R.string.toast_wallet_reset, Toast.LENGTH_LONG).show();

			Log.i(TAG, "wallet restored from backup: '" + Constants.WALLET_KEY_BACKUP_BASE58 + "'");

			return wallet;
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	private Wallet restoreWalletFromSnapshot() throws FileNotFoundException
	{
		try
		{
			final Wallet wallet = readKeys(getAssets().open(Constants.WALLET_KEY_BACKUP_SNAPSHOT));

			Log.i(TAG, "wallet restored from snapshot: '" + Constants.WALLET_KEY_BACKUP_SNAPSHOT + "'");

			return wallet;
		}
		catch (final FileNotFoundException x)
		{
			throw x;
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	private static Wallet readKeys(final InputStream is) throws IOException
	{
		final BufferedReader in = new BufferedReader(new InputStreamReader(is, UTF_8));
		final List<ECKey> keys = WalletUtils.readKeys(in);
		in.close();

		final Wallet wallet = new Wallet(Constants.NETWORK_PARAMETERS);
		for (final ECKey key : keys)
			wallet.addKey(key);

		return wallet;
	}

	public void addNewKeyToWallet()
	{
		wallet.addKey(new ECKey());

		backupKeys();
	}

	public void saveWallet()
	{
		try
		{
			protobufSerializeWallet(wallet);
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	private void protobufSerializeWallet(final Wallet wallet) throws IOException
	{
		final long start = System.currentTimeMillis();

		wallet.saveToFile(walletFile);

		// make wallets world accessible in test mode
		if (Constants.TEST)
			WalletUtils.chmod(walletFile, 0777);

		Log.d(TAG, "wallet saved to: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
	}


	private void backupKeys()
	{
		try
		{
			writeKeys(openFileOutput(Constants.WALLET_KEY_BACKUP_BASE58, Context.MODE_PRIVATE));
		}
		catch (final IOException x)
		{
			x.printStackTrace();
		}

		try
		{
			final String filename = String.format(Locale.US, "%s.%02d", Constants.WALLET_KEY_BACKUP_BASE58,
					(System.currentTimeMillis() / DateUtils.DAY_IN_MILLIS) % 100l);
			writeKeys(openFileOutput(filename, Context.MODE_PRIVATE));
		}
		catch (final IOException x)
		{
			x.printStackTrace();
		}
	}

	private void writeKeys(final OutputStream os) throws IOException
	{
		final Writer out = new OutputStreamWriter(os, UTF_8);
		WalletUtils.writeKeys(out, wallet.keychain);
		out.close();
	}

	public Address determineSelectedAddress()
	{
		final ArrayList<ECKey> keychain = wallet.keychain;

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String selectedAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);

		if (selectedAddress != null)
		{
			for (final ECKey key : keychain)
			{
				final Address address = key.toAddress(Constants.NETWORK_PARAMETERS);
				if (address.toString().equals(selectedAddress))
					return address;
			}
		}

		return keychain.get(0).toAddress(Constants.NETWORK_PARAMETERS);
	}

	public void startBlockchainService(final boolean cancelCoinsReceived)
	{
		if (cancelCoinsReceived)
			startService(blockchainServiceCancelCoinsReceivedIntent);
		else
			startService(blockchainServiceIntent);
	}

	public void stopBlockchainService()
	{
		stopService(blockchainServiceIntent);
	}

	public void resetBlockchain()
	{
		// actually stops the service
        Log.d("Feathercoin", "Sending blockchain service reset intent");
		startService(blockchainServiceResetBlockchainIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
	}


	public final int applicationVersionCode()
	{
		try
		{
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException x)
		{
			return 0;
		}
	}

	public final String applicationVersionName()
	{
		try
		{
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException x)
		{
			return "unknown";
		}
	}

	public int maxConnectedPeers()
	{
		final int memoryClass = activityManager.getMemoryClass();
		if (memoryClass <= 32)
			return 4;
		else
			return 6;
	}
}
