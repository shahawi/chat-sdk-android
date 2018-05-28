/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import co.chatsdk.core.Tab;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.session.InterfaceManager;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.ui.R;

public class MainActivity extends BaseActivity {

    protected TabLayout tabLayout;
    protected ViewPager viewPager;
    protected PagerAdapterTabs adapter;

    protected DisposableList disposables = new DisposableList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed context
            finish();
            return;
        }

        setContentView(R.layout.chat_sdk_activity_view_pager);

        initViews();

//        requestPermissionSafely(requestExternalStorage().doFinally(() -> requestPermissionSafely(requestReadContacts().doFinally(() -> {
//            //requestVideoAccess().subscribe();
//        }))));

        launchFromPush(getIntent().getExtras());

    }

    public void launchFromPush (Bundle bundle) {
        if (bundle != null) {
            String threadID = bundle.getString(InterfaceManager.THREAD_ENTITY_ID);
            if (threadID != null && !threadID.isEmpty()) {
                ChatSDK.ui().startChatActivityForID(getBaseContext(), threadID);
            }
        }
    }

//    public void requestPermissionSafely (Completable c) {
//        c.subscribe(new CrashReportingCompletableObserver());
//    }
//
//    public Completable requestMicrophoneAccess () {
//        if(ChatSDK.audioMessage() != null) {
//            return PermissionRequestHandler.shared().requestRecordAudio(this);
//        }
//        return Completable.complete();
//    }
//
//    public Completable requestExternalStorage () {
////        if(ChatSDK.audioMessage() != null) {
//            return PermissionRequestHandler.shared().requestReadExternalStorage(this);
////        }
////        return Completable.complete();
//    }
//
//    public Completable requestVideoAccess () {
//        if(ChatSDK.videoMessage() != null) {
//            return PermissionRequestHandler.shared().requestVideoAccess(this);
//        }
//        return Completable.complete();
//    }
//
//    public Completable requestReadContacts () {
//        if(ChatSDK.contact() != null) {
//            return PermissionRequestHandler.shared().requestReadContact(this);
//        }
//        return Completable.complete();
//    }


//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        PermissionRequestHandler.shared().onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }

    @Override
    protected void onResume() {
        super.onResume();

        disposables.dispose();

        disposables.add(ChatSDK.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.Logout))
                .subscribe(networkEvent -> clearData()));

        reloadData();
    }

    @Override
    protected void onPause () {
        super.onPause();
        disposables.dispose();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        launchFromPush(intent.getExtras());

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void initViews() {

        viewPager = findViewById(R.id.pager);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        // Only creates the adapter if it wasn't initiated already
        if (adapter == null) {
            adapter = new PagerAdapterTabs(getSupportFragmentManager());
        }

        final List<Tab> tabs = adapter.getTabs();
        for (Tab tab : tabs) {
            tabLayout.addTab(tabLayout.newTab().setText(tab.title));
        }

        ((BaseFragment) tabs.get(0).fragment).setTabVisibility(true);

        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());

                updateLocalNotificationsForTab();

                // We mark the tab as visible. This lets us be more efficient with updates
                // because we only
                for(int i = 0; i < tabs.size(); i++) {
                    ((BaseFragment) tabs.get(i).fragment).setTabVisibility(i == tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        updateLocalNotificationsForTab();


//        tabLayout.setViewPager(viewPager);
//
//        // TODO: Check this - whenever we change tabLayout, we set the user online
//        tabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//                ChatSDK.core().setUserOnline();
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });

        viewPager.setOffscreenPageLimit(3);

    }

    public void updateLocalNotificationsForTab () {
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        ChatSDK.ui(). setShowLocalNotifications(showLocalNotificationsForTab(tab));

    }

    public boolean showLocalNotificationsForTab (TabLayout.Tab tab) {
        // Don't show notifications on the threads tabs
        Tab t = adapter.getTabs().get(tab.getPosition());

        Class privateThreadsFragmentClass = ChatSDK.ui().privateThreadsFragment().getClass();

        return !t.fragment.getClass().isAssignableFrom(privateThreadsFragmentClass);
    }

    public void clearData () {
        for(Tab t : adapter.getTabs()) {
            if(t.fragment instanceof BaseFragment) {
                ((BaseFragment) t.fragment).clearData();
            }
        }
    }

    public void reloadData () {
        for(Tab t : adapter.getTabs()) {
            if(t.fragment instanceof BaseFragment) {
                ((BaseFragment) t.fragment).safeReloadData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.contact_developer) {

            String emailAddress = ChatSDK.config().contactDeveloperEmailAddress;
            String subject = ChatSDK.config().contactDeveloperEmailSubject;
            String dialogTitle = ChatSDK.config().contactDeveloperDialogTitle;

            if(StringUtils.isNotEmpty(emailAddress))
            {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", emailAddress, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                startActivity(Intent.createChooser(emailIntent, dialogTitle));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
