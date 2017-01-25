package org.mistercyb.wifiinfo;


import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

/**
 *
 * Created by danno on 10/20/16.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

public class MainActivity extends AppCompatActivity {

    private static final String SELECTED_INDEX = "selectedIndex";

    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private ActionBarDrawerToggle drawerToggle;
    private int selectedItemId;
    private WifiHelper wifiHelper;
    private WifiInfoRetainer retainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
        {
            selectedItemId = savedInstanceState.getInt(SELECTED_INDEX);
        } else
        {
            selectedItemId = -1;
        }

        retainer = WifiInfoRetainer.getInstance();

        if (retainer.wifiHelper == null)
        {
            retainer.wifiHelper = new WifiHelper(this);
        }

        wifiHelper = retainer.wifiHelper;

        // Set a Toolbar to replace the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbarTitle = (TextView)findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView nvDrawer = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);


        assert nvDrawer != null;
        assert nvDrawer.getMenu() != null;
        nvDrawer.getMenu().getItem(0).setChecked(true);

        if (selectedItemId != -1)
        {
            selectDrawerItem(nvDrawer.getMenu().findItem(selectedItemId));
        } else
        {
            selectDrawerItem(nvDrawer.getMenu().getItem(0));
        }

        // Find our drawer view
        drawerToggle = setupDrawerToggle();

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.addDrawerListener(drawerToggle);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        this.wifiHelper.processGrantedPermissions(requestCode);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SELECTED_INDEX, selectedItemId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (retainer.scannerFragment != null && retainer.scannerFragment.receiverRegistered)
        {
            this.unregisterReceiver(retainer.scannerFragment.getBroadcastReceiver());
            retainer.scannerFragment.receiverRegistered = false;
        }
    }

    @Override
    public void onBackPressed()
    {
        if (this.mDrawer.isDrawerOpen(GravityCompat.START)) {
            this.mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    private void selectDrawerItem(MenuItem menuItem) {

        // Create a new fragment and specify the fragment to show based on nav item clicked
        int itemID = menuItem.getItemId();
        updateToolbarTitle(menuItem);

        if (itemID == selectedItemId)
        {
            mDrawer.closeDrawers();
            return; //It's the same item.
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        selectedItemId = itemID;
        Fragment fragment = null;
        Class fragmentClass;
        String fragmentTag = "";

        try {
            switch(itemID) {
                case R.id.nav_scanner_fragment:
                    fragmentClass = ScannerFragment.class;
                    fragmentTag = "Scanner";
                    if (retainer.scannerFragment == null) {
                        retainer.scannerFragment =  (ScannerFragment)fragmentClass.newInstance();
                        retainer.scannerFragment.wifiHelper = wifiHelper;
                        wifiHelper.addEventReceiver(retainer.scannerFragment);
                    }
                    fragment = retainer.scannerFragment;
                    break;
                case R.id.nav_saved_fragment:
                    fragmentClass = DiscoverDevicesFragment.class;
                    fragmentTag = "Discover";
                    if (retainer.discoverFragment == null) {
                        retainer.discoverFragment =  (DiscoverDevicesFragment)fragmentClass.newInstance();
                        retainer.discoverFragment.wifiHelper = wifiHelper;
                        wifiHelper.addEventReceiver(retainer.discoverFragment);
                    }
                    fragment = retainer.discoverFragment;
                    break;
                case R.id.nav_about_fragment:
                    fragmentClass = AboutFragment.class;
                    fragmentTag = "About";
                    break;
                case R.id.nav_current_connection: {
                    fragmentClass = CurrentConnectionFragment.class;
                    fragmentTag = "Current";
                    if (retainer.connectionFragment == null) {
                        retainer.connectionFragment =  (CurrentConnectionFragment)fragmentClass.newInstance();
                        retainer.connectionFragment.wifiHelper = wifiHelper;
                        wifiHelper.addEventReceiver(retainer.connectionFragment);
                    }
                    fragment = retainer.connectionFragment;

                }
                    break;
                default:
                    fragmentClass = ScannerFragment.class;
                    fragmentTag = "Scanner";
            }
            if (fragment == null) {
                fragment = (Fragment)fragmentClass.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment, fragmentTag).commit();

        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);

    }

    private void updateToolbarTitle(MenuItem menuItem)
    {
        setTitle(menuItem.getTitle());
        setTitle("");
        toolbarTitle.setText(menuItem.getTitle());
        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // The action bar home/up action should open or close the drawer.
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                mDrawer.openDrawer(GravityCompat.START);
//                return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    // `onPostCreate` called when activity start-up is complete after `onStart()`
    // NOTE! Make sure to override the method with only a single `Bundle` argument
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }


}