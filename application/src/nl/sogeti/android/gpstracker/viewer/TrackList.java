/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.viewer;

import android.widget.*;
import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.DescribeTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.adapter.SectionedListAdapter;
import nl.sogeti.android.gpstracker.db.DatabaseHelper;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Show a list view of all tracks, also doubles for showing search results
 * 
 * @version $Id: TrackList.java 1144 2011-10-30 11:19:53Z rcgroot $
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackList extends ListActivity implements ProgressListener
{

   private static final String TAG = "OGT.TrackList";
   private static final int MENU_DETELE = Menu.FIRST + 0;
   private static final int MENU_SHARE = Menu.FIRST + 1;
   private static final int MENU_RENAME = Menu.FIRST + 2;
   private static final int MENU_STATS = Menu.FIRST + 3;
   private static final int MENU_SEARCH = Menu.FIRST + 4;
   private static final int MENU_VACUUM = Menu.FIRST + 5;
   private static final int MENU_PICKER = Menu.FIRST + 6;
   private static final int MENU_BREADCRUMBS = Menu.FIRST + 7;

   public static final int DIALOG_FILENAME = Menu.FIRST + 22;
   private static final int DIALOG_RENAME = Menu.FIRST + 23;
   private static final int DIALOG_DELETE = Menu.FIRST + 24;
   private static final int DIALOG_VACUUM = Menu.FIRST + 25;
   private static final int DIALOG_IMPORT = Menu.FIRST + 26;
   private static final int DIALOG_INSTALL = Menu.FIRST + 27;
   protected static final int DIALOG_ERROR = Menu.FIRST + 28;

   private static final int PICKER_OI = Menu.FIRST + 29;
   private static final int DESCRIBE = Menu.FIRST + 30;

   private BreadcrumbsAdapter mBreadcrumbAdapter;
   private EditText mTrackNameView;     //See how it relates to NameTrack.mTrackNameView
    //f8f_change
    private CheckBox mTrackHelmetView;
   private Spinner mOriginReasonSpinner;

    private ArrayAdapter<CharSequence> mOriginReasonAdapter;

    //private static final int REASON_HOME = 0;
    //private static final int REASON_WORK = 1;
    //private static final int REASON_ELSE = 2;
    //private static final int REASON_DESCRIBE = 3;
    //f8f_end

   private Uri mDialogUri;
   private String mDialogCurrentName = "";
   private String mErrorDialogMessage;
   private Exception mErrorDialogException;

   private Runnable mImportAction;

   private OnClickListener mDeleteOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         getContentResolver().delete(mDialogUri, null, null);
      }
   };
   private OnClickListener mRenameOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         //         Log.d( TAG, "Context item selected: "+mDialogUri+" with name "+mDialogCurrentName );

          //TODO: Input code somewhere around to have a correct rename dialog

          ContentValues values = new ContentValues();

         String trackName = mTrackNameView.getText().toString();
                   // This is where data is retrieved from the interface to be passed on to the provider (through the resolver)
          //Note : the relationship with NameTrack activity is not clear at the moment 08/02/12
         values.put(Tracks.NAME, trackName);

          if (mTrackHelmetView.isChecked())
          {
              values.put(Tracks.WITH_HELMET, "1");
          }
          else
          {
              values.put(Tracks.WITH_HELMET, "0");
          }

          String startReason = mOriginReasonSpinner.getSelectedItem().toString();
          
          values.put(Tracks.START_REASON, startReason);

              
         TrackList.this.getContentResolver().update(mDialogUri, values, null, null);
      }
   };
   private OnClickListener mVacuumOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         DatabaseHelper helper = new DatabaseHelper(TrackList.this);
         helper.vacuum();
      }
   };
   private OnClickListener mImportOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         mImportAction.run();
      }
   };
   private final DialogInterface.OnClickListener mOiPickerDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         Uri oiDownload = Uri.parse("market://details?id=org.openintents.filemanager");
         Intent oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
         try
         {
            startActivity(oiAboutIntent);
         }
         catch (ActivityNotFoundException e)
         {
            oiDownload = Uri.parse("http://openintents.googlecode.com/files/FileManager-1.1.3.apk");
            oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
            startActivity(oiAboutIntent);
         }
      }
   };
   private String mImportTrackName;
   private String mErrorTask;
   /**
    * Progress listener for the background tasks uploading to gobreadcrumbs
    */
   private ProgressListener mExportListener;
   private int mPausePosition;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      getWindow().requestFeature(Window.FEATURE_PROGRESS);
      this.setContentView(R.layout.tracklist);

      displayIntent(getIntent());

      ListView listView = getListView();
      listView.setItemsCanFocus(true);
      // Add the context menu (the long press thing)
      registerForContextMenu(listView);

      if( savedInstanceState != null )
      {
         getListView().setSelection(savedInstanceState.getInt("POSITION"));
      }
   }
   
   @Override
   protected void onResume()
   {
      if( mPausePosition != 0 )
      {
         getListView().setSelection(mPausePosition);
      }
      super.onResume();
   }
   @Override
   protected void onPause()
   {
      mPausePosition = getListView().getFirstVisiblePosition();
      super.onPause();
   }
   
   @Override
   protected void onDestroy()
   {
      if (mBreadcrumbAdapter != null && isFinishing())
      {
         mBreadcrumbAdapter.shutdown();
      }
      super.onDestroy();
   }

   @Override
   public Object onRetainNonConfigurationInstance()
   {
      return mBreadcrumbAdapter;
   }

   @Override
   public void onNewIntent(Intent newIntent)
   {
      displayIntent(newIntent);
   }

   /*
    * (non-Javadoc)
    * @see android.app.ListActivity#onRestoreInstanceState(android.os.Bundle)
    */
   @Override
   protected void onRestoreInstanceState(Bundle state)
   {
      super.onRestoreInstanceState(state);
      mDialogUri = state.getParcelable("URI");
      mDialogCurrentName = state.getString("NAME");
      mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
      getListView().setSelection(state.getInt("POSITION"));
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
    */
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);
      outState.putParcelable("URI", mDialogUri);
      outState.putString("NAME", mDialogCurrentName);
      outState.putInt("POSITION",getListView().getFirstVisiblePosition());
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      boolean result = super.onCreateOptionsMenu(menu);

      menu.add(ContextMenu.NONE, MENU_SEARCH, ContextMenu.NONE, android.R.string.search_go).setIcon(android.R.drawable.ic_search_category_default)
            .setAlphabeticShortcut(SearchManager.MENU_KEY);
      menu.add(ContextMenu.NONE, MENU_VACUUM, ContextMenu.NONE, R.string.menu_vacuum).setIcon(android.R.drawable.ic_menu_crop);
      menu.add(ContextMenu.NONE, MENU_PICKER, ContextMenu.NONE, R.string.menu_picker).setIcon(android.R.drawable.ic_menu_add);
      menu.add(ContextMenu.NONE, MENU_BREADCRUMBS, ContextMenu.NONE, R.string.dialog_breadcrumbsconnect).setIcon(android.R.drawable.ic_menu_revert);
      return result;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;
      switch (item.getItemId())
      {
         case MENU_SEARCH:
            onSearchRequested();
            handled = true;
            break;
         case MENU_VACUUM:
            showDialog(DIALOG_VACUUM);
            break;
         case MENU_PICKER:
            try
            {
               Intent intent = new Intent("org.openintents.action.PICK_FILE");
               intent.putExtra("org.openintents.extra.TITLE", getString(R.string.dialog_import_picker));
               intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.menu_picker));
               startActivityForResult(intent, PICKER_OI);
            }
            catch (ActivityNotFoundException e)
            {
               showDialog(DIALOG_INSTALL);
            }
            break;
         case MENU_BREADCRUMBS:
            mBreadcrumbAdapter.removeAuthentication();
            mBreadcrumbAdapter.getBreadcrumbsTracks().clearAllCache(this);
            mBreadcrumbAdapter.requestBreadcrumbsOauthToken(this);
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   @Override
   protected void onListItemClick(ListView listView, View view, int position, long id)
   {
      super.onListItemClick(listView, view, position, id);

      Object item = listView.getItemAtPosition(position);
      if (item instanceof String)
      {
         if (Constants.BREADCRUMBS_CONNECT.equals(item))
         {
            mBreadcrumbAdapter.requestBreadcrumbsOauthToken(this);
         }
      }
      else if (item instanceof Pair< ? , ? >)
      {
         @SuppressWarnings("unchecked")
         final Pair<Integer, Integer> track = (Pair<Integer, Integer>) item;
         if (track.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE)
         {
            TextView tv = (TextView) view.findViewById(R.id.listitem_name);
            mImportTrackName = tv.getText().toString();
            mImportAction = new Runnable()
            {
               public void run()
               {
                  mBreadcrumbAdapter.startDownloadTask(TrackList.this, TrackList.this, track);
               }
            };
            showDialog(DIALOG_IMPORT);
         }
      }
      else
      {
         Intent intent = new Intent();
         Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, id);
         intent.setData(trackUri);
         ComponentName caller = this.getCallingActivity();
         if (caller != null)
         {
            setResult(RESULT_OK, intent);
            finish();
         }
         else
         {
            intent.setClass(this, LoggerMap.class);
            startActivity(intent);
         }
      }
   }

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
   {
      if (menuInfo instanceof AdapterView.AdapterContextMenuInfo)
      {
         AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
         TextView textView = (TextView) itemInfo.targetView.findViewById(R.id.listitem_name);
         if (textView != null)
         {
            menu.setHeaderTitle(textView.getText());
         }
         
         Object listItem = getListAdapter().getItem(itemInfo.position);
         if( listItem instanceof Cursor)
         {
            menu.add(0, MENU_STATS, 0, R.string.menu_statistics);
            menu.add(0, MENU_SHARE, 0, R.string.menu_shareTrack);
            menu.add(0, MENU_RENAME, 0, R.string.menu_renameTrack);
            menu.add(0, MENU_DETELE, 0, R.string.menu_deleteTrack);
         }
      }
   }

   @Override
   public boolean onContextItemSelected(MenuItem item)
   {
      boolean handled = false;
      AdapterView.AdapterContextMenuInfo info;
      try
      {
         info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      }
      catch (ClassCastException e)
      {
         Log.e(TAG, "Bad menuInfo", e);
         return handled;
      }
      
      Object listItem = getListAdapter().getItem(info.position);
      if( listItem instanceof Cursor)
      {
         Cursor cursor = (Cursor) listItem;
         mDialogUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, cursor.getLong(0));
         mDialogCurrentName = cursor.getString(1);
         mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
         switch (item.getItemId())
         {
            case MENU_DETELE:
            {
               showDialog(DIALOG_DELETE);
               handled = true;
               break;
            }
            case MENU_SHARE:
            {
               Intent actionIntent = new Intent(Intent.ACTION_RUN);
               actionIntent.setDataAndType(mDialogUri, Tracks.CONTENT_ITEM_TYPE);
               actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
               startActivity(Intent.createChooser(actionIntent, getString(R.string.share_track)));
               handled = true;
               break;
            }
            case MENU_RENAME:
            {
                //TODO : move this into it's own Intent like share ?
                //Note : this is the long press menu on the track


               showDialog(DIALOG_RENAME);
               handled = true;
               break;
            }
            case MENU_STATS:
            {
               Intent actionIntent = new Intent(this, Statistics.class);
               actionIntent.setData(mDialogUri);
               startActivity(actionIntent);
               handled = true;
               break;
            }
            default:
               handled = super.onContextItemSelected(item);
               break;
         }
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_RENAME:
             //Code to create the 'rename dialog' which now includes additional data for Bixi tracking
            LayoutInflater factory = LayoutInflater.from(this);
            View view = factory.inflate(R.layout.namedialog, null);
            mTrackNameView = (EditText) view.findViewById(R.id.nameField);
            mTrackHelmetView = (CheckBox) view.findViewById(R.id.helmetCheck);
            mOriginReasonSpinner = (Spinner) view.findViewById(R.id.originReasonSpinner);
             mOriginReasonAdapter = ArrayAdapter.createFromResource(this, R.array.Reason_choices, android.R.layout.simple_spinner_item);
             mOriginReasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

             mOriginReasonSpinner.setAdapter(mOriginReasonAdapter);

             mOriginReasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
             {
                 //          public void onItemSelected(AdapterView<?> parent,
//                                     View view, int pos, long id) {
                 public void onItemSelected(AdapterView< ? > arg0, View arg1, int position, long arg3)
                 {
                     //adjustTargetToType(position);
                 }

                 public void onNothingSelected(AdapterView< ? > arg0)
                 { /* NOOP */
                 }
             });


            builder = new AlertDialog.Builder(this).setTitle(R.string.dialog_routename_title).setMessage(R.string.dialog_routename_message)
                  .setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(R.string.btn_okay, mRenameOnClickListener)
                  .setNegativeButton(R.string.btn_cancel, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_DELETE:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_delete_title).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, mDeleteOnClickListener);
            dialog = builder.create();
            String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
            String message = String.format(messageFormat, "");
            ((AlertDialog) dialog).setMessage(message);
            return dialog;
         case DIALOG_VACUUM:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_vacuum_title).setMessage(R.string.dialog_vacuum_message)
                  .setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(android.R.string.cancel, null)
                  .setPositiveButton(android.R.string.ok, mVacuumOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_IMPORT:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_import_title)
                  .setMessage(getString(R.string.dialog_import_message, mImportTrackName)).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, mImportOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_nooipicker).setMessage(R.string.dialog_nooipicker_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_install, mOiPickerDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_ERROR:
            builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title).setMessage(mErrorDialogMessage)
                  .setNeutralButton(android.R.string.cancel, null);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      super.onPrepareDialog(id, dialog);
      AlertDialog alert;
      String message;
      switch (id)
      {
         case DIALOG_RENAME:
            mTrackNameView.setText(mDialogCurrentName);
            mTrackNameView.setSelection(0, mDialogCurrentName.length());

             //I need to setup data such as dialog reflects database content
             //I have the track URI so damn fucking fuck I'll get that row
             Cursor trackCursor = null;
             trackCursor = TrackList.this.getContentResolver().query(mDialogUri, new String[] { Tracks._ID, Tracks.WITH_HELMET, Tracks.START_REASON}, null, null,null);

             //This should always contains at most 1 row, given we request for a particular track ID
             if (trackCursor.moveToFirst())
             {
                 int helmetIdx = trackCursor.getColumnIndex(Tracks.WITH_HELMET);
                 boolean isnull = trackCursor.isNull(helmetIdx);

                 int helmetValue = trackCursor.getInt(helmetIdx);

                 //if (trackCursor.getInt(trackCursor.getColumnIndex(Tracks.WITH_HELMET)) == 1)
                 if (helmetValue == 1)
                 {
                     mTrackHelmetView.setChecked(true);
                 }
                 else
                 {
                     mTrackHelmetView.setChecked(false);
                 }
                 
                 int startReasonIdx = trackCursor.getColumnIndex(Tracks.START_REASON);
                 String startReason = trackCursor.getString(startReasonIdx);
                 
                 //beurk !
                 mOriginReasonSpinner.setSelection(mOriginReasonAdapter.getPosition(startReason));
             }

             //TODO: Retrieve stuff from the database to link the dialog state to data
             //Check hos this is done in the track lst view for example
             trackCursor.close();
            break;
         case DIALOG_DELETE:
            alert = (AlertDialog) dialog;
            String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
            message = String.format(messageFormat, mDialogCurrentName);
            alert.setMessage(message);
            break;
         case DIALOG_ERROR:
            alert = (AlertDialog) dialog;
            message = "Failed task:\n" + mErrorTask;
            message += "\n\n";
            message += "Reason:\n" + mErrorDialogMessage;
            if (mErrorDialogException != null)
            {
               message += " (" + mErrorDialogException.getMessage() + ") ";
            }
            alert.setMessage(message);
            break;
         case DIALOG_IMPORT:
            alert = (AlertDialog) dialog;
            alert.setMessage(getString(R.string.dialog_import_message, mImportTrackName));
            break;
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      if (resultCode != RESULT_CANCELED)
      {
         switch (requestCode)
         {
            case PICKER_OI:
               new GpxParser(TrackList.this, TrackList.this).execute(data.getData());
               break;
            case DESCRIBE:
               Uri trackUri = data.getData();
               String name;
               if (data.getExtras() != null && data.getExtras().containsKey(Constants.NAME))
               {
                  name = data.getExtras().getString(Constants.NAME);
               }
               else
               {
                  name = "shareToGobreadcrumbs";
               }
               mBreadcrumbAdapter.startUploadTask(TrackList.this, mExportListener, trackUri, name);
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
      else
      {
         if (requestCode == DESCRIBE)
         {
            mBreadcrumbAdapter.notifyDataSetChanged();
         }
      }
   }

   private void displayIntent(Intent intent)
   {
      final String queryAction = intent.getAction();
      final String orderby = Tracks.CREATION_TIME + " DESC";
      Cursor tracksCursor = null;
      if (Intent.ACTION_SEARCH.equals(queryAction))
      {
         // Got to SEARCH a query for tracks, make a list
         tracksCursor = doSearchWithIntent(intent);
      }
      else if (Intent.ACTION_VIEW.equals(queryAction))
      {
         final Uri uri = intent.getData();
         if ("content".equals(uri.getScheme()) && GPStracking.AUTHORITY.equals(uri.getAuthority()))
         {
            // Got to VIEW a single track, instead hand it of to the LoggerMap
            Intent notificationIntent = new Intent(this, LoggerMap.class);
            notificationIntent.setData(uri);
            startActivity(notificationIntent);
            finish();
         }
         else if (uri.getScheme().equals("file") || uri.getScheme().equals("content"))
         {

            mImportTrackName = uri.getLastPathSegment();
            // Got to VIEW a GPX filename
            mImportAction = new Runnable()
            {
               public void run()
               {
                  new GpxParser(TrackList.this, TrackList.this).execute(uri);
               }
            };
            showDialog(DIALOG_IMPORT);
            tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, orderby);
         }
         else
         {
            Log.e(TAG, "Unable to VIEW " + uri);
         }
      }
      else
      {
         // Got to nothing, make a list of everything
         tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, orderby);
      }
      displayCursor(tracksCursor);

   }

   private void displayCursor(Cursor tracksCursor)
   {
      SectionedListAdapter sectionedAdapter = new SectionedListAdapter(this);

      String[] fromColumns = new String[] { Tracks.NAME, Tracks.CREATION_TIME, Tracks._ID };
      int[] toItems = new int[] { R.id.listitem_name, R.id.listitem_from, R.id.bcSyncedCheckBox };
      SimpleCursorAdapter trackAdapter = new SimpleCursorAdapter(this, R.layout.trackitem, tracksCursor, fromColumns, toItems);

      sectionedAdapter.addSection("Local", trackAdapter);

      mBreadcrumbAdapter = (BreadcrumbsAdapter) getLastNonConfigurationInstance();
      if (mBreadcrumbAdapter == null)
      {
         mBreadcrumbAdapter = new BreadcrumbsAdapter(this, this);
         mBreadcrumbAdapter.connectionSetup();
      }

      sectionedAdapter.addSection("GoBreadcrumbs", mBreadcrumbAdapter);

      // Enrich the track adapter with Breadcrumbs adapter data 
      trackAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
      {
         public boolean setViewValue(View view, final Cursor cursor, int columnIndex)
         {
            if (columnIndex == 0)
            {
               final long trackId = cursor.getLong(0);
               final String trackName = cursor.getString(1);
               // Show the check if Breadcrumbs is online
               final CheckBox checkbox = (CheckBox) view;
               final ProgressBar progressbar = (ProgressBar) ((View)view.getParent()).findViewById(R.id.bcExportProgress);
               if (mBreadcrumbAdapter.isOnline())
               {
                  checkbox.setVisibility(View.VISIBLE);

                  // Disable the checkbox if marked online
                  BreadcrumbsTracks tracks = mBreadcrumbAdapter.getBreadcrumbsTracks();
                  boolean isOnline = tracks.isLocalTrackSynced(trackId);
                  checkbox.setEnabled(!isOnline);

                  // Check the checkbox if determined synced
                  boolean isSynced = tracks.isLocalTrackSynced(trackId);
                  checkbox.setOnCheckedChangeListener(null);
                  checkbox.setChecked(isSynced);
                  checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
                  {
                     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                     {
                        if (isChecked)
                        {
                           // Start a description of the track
                           Intent namingIntent = new Intent(TrackList.this, DescribeTrack.class);
                           namingIntent.setData(ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId));
                           namingIntent.putExtra(Constants.NAME, trackName);
                           mExportListener = new ProgressListener()
                           {
                              public void setIndeterminate(boolean indeterminate)
                              {
                                 progressbar.setIndeterminate(indeterminate);
                              }

                              public void started()
                              {
                                 checkbox.setVisibility(View.INVISIBLE);
                                 progressbar.setVisibility(View.VISIBLE);
                              }

                              public void finished(Uri result)
                              {
                                 checkbox.setVisibility(View.VISIBLE);
                                 progressbar.setVisibility(View.INVISIBLE);
                                 progressbar.setIndeterminate(false);
                              }

                              public void setProgress(int value)
                              {
                                 progressbar.setProgress(value);
                              }

                              public void showError(String task, String errorMessage, Exception exception)
                              {
                                 TrackList.this.showError(task, errorMessage, exception);
                              }
                           };
                           startActivityForResult(namingIntent, DESCRIBE);
                        }
                     }
                  });
               }
               else
               {
                  checkbox.setVisibility(View.INVISIBLE);
                  checkbox.setOnCheckedChangeListener(null);
               }
               return true;
            }
            return false;
         }
      });

      setListAdapter(sectionedAdapter);
   }

   private Cursor doSearchWithIntent(final Intent queryIntent)
   {
      final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
      Cursor cursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, "name LIKE ?", new String[] { "%"
            + queryString + "%" }, null);
      return cursor;
   }

   /*******************************************************************/
   /** ProgressListener interface and UI actions (non-Javadoc) **/
   /*******************************************************************/

   public void setIndeterminate(boolean indeterminate)
   {
      setProgressBarIndeterminate(indeterminate);
   }

   public void started()
   {
      setProgressBarVisibility(true);
      setProgress(Window.PROGRESS_START);
   }
   
   public void finished(Uri result)
   {
      setProgressBarVisibility(false);
      setProgressBarIndeterminate(false);
   }

   public void showError(String task, String errorDialogMessage, Exception errorDialogException)
   {
      mErrorTask = task;
      mErrorDialogMessage = errorDialogMessage;
      mErrorDialogException = errorDialogException;
      Log.e(TAG, errorDialogMessage, errorDialogException);
      if (!isFinishing())
      {
         showDialog(DIALOG_ERROR);
      }
      setProgressBarVisibility(false);
      setProgressBarIndeterminate(false);
   }

}