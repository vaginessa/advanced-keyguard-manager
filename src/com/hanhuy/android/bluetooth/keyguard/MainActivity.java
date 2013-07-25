package com.hanhuy.android.bluetooth.keyguard;

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity {
    private final static int DIALOG_NO_PAIRED_DEVICES = 0;
    private Settings settings;

    final static String TAG = "BluetoothKeyguardMainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.getInstance(this);
        setContentView(R.layout.main);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new PagerAdapter());
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private PagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new OverviewFragment();
                case 1: return new WifiFragment();
                case 2: return new BluetoothFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = null;
            switch (position) {
                case 0: title = getString(R.string.overview);  break;
                case 1: title = getString(R.string.wifi);      break;
                case 2: title = getString(R.string.bluetooth); break;
            }
            return title;
        }
    }

    private BroadcastReceiver keyguardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (hasWindowFocus()) {
                NotificationManager nm = (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE);
                nm.cancel(KeyguardMediator.NOTIFICATION_TOGGLE);
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.cancel(KeyguardMediator.NOTIFICATION_TOGGLE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(KeyguardMediator.ACTION_STATE_CHANGED);
        registerReceiver(keyguardReceiver, filter);

        // yuck, but it seems there's no other way to toggle menu items after
        // enabling device admin
        supportInvalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(keyguardReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main, menu);
        MenuItem setPass = menu.findItem(R.id.set_password);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        setPass.setEnabled(dpm.isAdminActive(
                new ComponentName(this, AdminReceiver.class)));
        MenuItem showNotifications = menu.findItem(R.id.show_notifications);
        boolean showNotif = settings.get(Settings.SHOW_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            showNotifications.setTitle(showNotif ?
                    R.string.hide_notifications : R.string.show_notifications);
        }
        showNotifications.setChecked(showNotif);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.set_password:
                Intent setp = new Intent(this, PasswordActivity.class);
                startActivity(setp);
                return true;
            case R.id.show_notifications:
                boolean value = !item.isChecked();
                settings.set(Settings.SHOW_NOTIFICATIONS, value);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    item.setTitle(value ? R.string.hide_notifications :
                            R.string.show_notifications);
                }
                item.setChecked(value);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
