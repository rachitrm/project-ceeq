/**
 * 
 * @author Rachit Mishra
 * @licence The MIT License (MIT) Copyright (c) <2013> <Rachit Mishra> 
 *
 */

package in.ceeq.activities;

import in.ceeq.CeeqApplication;
import in.ceeq.R;
import in.ceeq.actions.Choose;
import in.ceeq.actions.Phone;
import in.ceeq.actions.Reset;
import in.ceeq.helpers.Logger;
import in.ceeq.helpers.PreferencesHelper;
import in.ceeq.receivers.DeviceAdmin;

import java.util.ArrayList;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.cache.SimpleImageLoader;
import com.github.johnpersano.supertoasts.SuperToast;

public class Setup extends Activity {

	private static final int DEVICE_ADMIN_ACTIVATION_REQUEST = 9011;

	private boolean exit = false;
	private boolean isPinSet;
	private boolean isContactSet;
	private EditText pinNumber;
	private SimpleImageLoader imageLoader;
	private ImageView userImage;
	private TextView userName;
	private Button selectContact;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Logger.w("Startup");
		setContentView(R.layout.activity_firstrun);
		setupImageCache();
		setupUi();
		setupListeners();
		setupHelpers();
		// setupHelplist();
		Reset.getInstance(this).defaults();
		setupDevice();
		pinNumber = (EditText) findViewById(R.id.pinNumber);
		pinNumber.addTextChangedListener(new PinValidator());
	}

	private void setupUi() {
		userImage = (ImageView) findViewById(R.id.user_image);
		selectContact = (Button) findViewById(R.id.chooseContact);
		String imageUrl = PreferencesHelper.getInstance(this).getString(PreferencesHelper.ACCOUNT_USER_IMAGE_URL);
		imageLoader.get(imageUrl, userImage, getResources().getDrawable(R.drawable.ic_user));
		userName = (TextView) findViewById(R.id.user_name);
		String username = PreferencesHelper.getInstance(this).getString(PreferencesHelper.ACCOUNT_USER_NAME);
		userName.setText(" " + username);
	}

	private void setupListeners() {
		selectContact.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Choose.getInstance(Setup.this).contact();
			}
		});
	}

	private void setupDevice() {
		preferencesHelper.setString(PreferencesHelper.SIM_NUMBER, Phone.get(Phone.SIM_ID, this));
		preferencesHelper.setString(PreferencesHelper.IEMI_NUMBER, Phone.get(Phone.IEMI, this));
		preferencesHelper.setString(PreferencesHelper.DEVICE_ID, Phone.get(Phone.UNIQUE_ID, this));
	}

	private PreferencesHelper preferencesHelper;

	public void setupHelpers() {
		preferencesHelper = PreferencesHelper.getInstance(this);
	}

	public void setupImageCache() {
		imageLoader = CeeqApplication.getInstance().getImageCache();
	}

	private ExpandableListView helpList;

	public void setupHelplist() {
		// helpList = (ExpandableListView) findViewById(R.id.help_list);
		ArrayList<String> help_list = new ArrayList<String>();
		help_list.add(getString(R.string.help_note_23));
		help_list.add(getString(R.string.help_note_24));
		ExpandableListAdapter helplistAdapter = new ListAdapter(this, help_list);
		helpList.setAdapter(helplistAdapter);
	}

	private ToggleButton toggleDeviceAdmin;

	public void onButtonPressed(View v) {
		switch (v.getId()) {
		case R.id.toggle_admin:
			toggleDeviceAdmin = (ToggleButton) v.findViewById(R.id.toggle_admin);
			preferencesHelper.setBoolean(PreferencesHelper.DEVICE_ADMIN_STATUS, toggleDeviceAdmin.isChecked());
			if (toggleDeviceAdmin.isChecked()) {
				Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
				ComponentName deviceAdminComponentName = new ComponentName(this, DeviceAdmin.class);
				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponentName);
				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.help_note_25));
				startActivityForResult(intent, DEVICE_ADMIN_ACTIVATION_REQUEST);
			} else {
				preferencesHelper.setBoolean(PreferencesHelper.DEVICE_ADMIN_STATUS, false);
				Toast.makeText(Setup.this, R.string.toast_string_5, Toast.LENGTH_LONG).show();
			}
			break;
		}
	}

	public void completeSetup() {
		if (isPinSet & isContactSet) {
			preferencesHelper.setBoolean(PreferencesHelper.APP_INITIALIZATION_STATUS, true);
			Intent launchHome = new Intent(Setup.this, Home.class);
			launchHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			launchHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			launchHome.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			SuperToast.cancelAllSuperToasts();
			startActivity(launchHome);
			overridePendingTransition(0, 0);
		} else if (!isPinSet) {
			if (pinNumber.getText().length() == 0)
				pinNumber.setError(getString(R.string.toast_string_10));
			else if (pinNumber.getText().length() < 6)
				pinNumber.setError("PIN should atleast be of 6 digits.");
		} else if (!isContactSet)
			SuperToast.create(Setup.this, getResources().getString(R.string.toast_string_11), Toast.LENGTH_SHORT).show();
	}

	public class PinValidator implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
			isPinSet = false;
		}

		@Override
		public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
			if (text.length() >= 6) {
				pinNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_yes, 0);
				preferencesHelper.setString(PreferencesHelper.PIN_NUMBER, pinNumber.getText().toString());

				SuperToast.create(Setup.this, getResources().getText(R.string.toast_string_2), Toast.LENGTH_LONG)
						.show();
				isPinSet = true;
			} else {
				pinNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				isPinSet = false;
			}

		}

	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (exit)
			Setup.this.finish();
		else {
			Toast.makeText(getApplicationContext(), R.string.toast_string_3, Toast.LENGTH_SHORT).show();
			exit = true;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					exit = false;
				}
			}, 3 * 1000);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case DEVICE_ADMIN_ACTIVATION_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				preferencesHelper.setBoolean(PreferencesHelper.DEVICE_ADMIN_STATUS, true);
				Toast.makeText(this, R.string.toast_string_6, Toast.LENGTH_LONG).show();
			} else {
				toggleDeviceAdmin.setChecked(false);
				preferencesHelper.setBoolean(PreferencesHelper.DEVICE_ADMIN_STATUS, false);
				Toast.makeText(this, R.string.toast_string_7, Toast.LENGTH_LONG).show();
			}
			break;

		case Choose.CONTACT_ACTIVATION_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				Uri contactData = data.getData();
				Cursor c = getContentResolver().query(contactData, null, null, null, null);
				if (c.moveToFirst()) {
					String emergencyContactName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
					String emergencyContactNumber = c.getString(c
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					selectContact.setText(emergencyContactName + " (" + emergencyContactNumber + ") ");
					preferencesHelper.setString(PreferencesHelper.EMERGENCY_CONTACT_NAME, emergencyContactName);
					preferencesHelper.setString(PreferencesHelper.EMERGENCY_CONTACT_NUMBER, emergencyContactNumber);

					Toast.makeText(Setup.this, R.string.toast_string_8, Toast.LENGTH_SHORT).show();
					isContactSet = true;
				}
				c.close();
			} else
				Toast.makeText(Setup.this, R.string.toast_string_9, Toast.LENGTH_SHORT).show();
		}

	}

	public class ListAdapter extends BaseExpandableListAdapter {

		public ArrayList<String> helpList = new ArrayList<String>();
		public int notificationCounter;
		public LayoutInflater inflater;
		public Context context;
		public boolean status;

		public ListAdapter(Context context, ArrayList<String> h_list) {
			this.context = context;
			this.helpList = h_list;
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_help_inner, null);
			}

			text = (TextView) convertView.findViewById(R.id.n_text);
			text.setText(helpList.get(childPosition));
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(context, helpList.get(childPosition), Toast.LENGTH_SHORT).show();
				}
			});
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return helpList.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return 1;
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_help_outer, null);
			}
			TextView header = (TextView) convertView.findViewById(R.id.n_header);
			header.setText(R.string.help);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.setup, menu);
        final MenuItem item = menu.findItem(R.id.completeSetupAction);
        item.getActionView().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {   
                completeSetup();
            }
        });
		return true;
	}
}