package com.hashcode.safestrap.ics;

import com.hashcode.safestrap.R;
import com.hashcode.safestrap.ics.AssetControl;
import com.hashcode.safestrap.ics.ExecuteAsRootBase;
import com.hashcode.safestrap.ics.InstallDialogThread;
import com.hashcode.safestrap.ics.UninstallDialogThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Properties;

public class SafestrapActivity extends Activity {
	final static public String PREFS_NAME = "disclaimer";
	
	private TextView statusText = null;
	private TextView messageText = null;
	private TextView textSystemStatus = null;
	private Button buttonInstall = null;
	private Button buttonUninstall = null;
	private Button buttonReboot = null;
	private Button buttonRebootWriteProtect = null;
	private InstallDialogThread installDialogThread = null;
	private UninstallDialogThread uninstallDialogThread = null;
	private ProgressDialog pDialog = null;
	private Boolean rootCheck = false; 
	private Boolean writeProtect = false;

	/** Setup the Widgets */
	protected void setupControls() {
        /* Setup button */
        try {
            buttonRebootWriteProtect.setVisibility(View.GONE);
        	if (rootCheck) {
                /* Check Version */
                String inText = ExecuteAsRootBase.executecmd("sh " + getFilesDir().toString() + "/recovery-check.sh " + getFilesDir().toString());
                String parts[] = inText.split(":");
                String vers_s = null;
                float vers = 0;
                Boolean altbootmode = false;
                if (parts != null) {
                    if (parts.length >= 1) { vers_s = parts[0]; }
                    if (parts.length >= 2) {
                        if (parts[1].equals("1"))
                            altbootmode = true;
                    }
                    if (parts.length >=3) {
                    	if (parts[2].equals("1")) {
                    		writeProtect = true;
                    	}
                    }
                }
                if (vers_s != null) {
                    vers = Float.valueOf(vers_s);
                }
                buttonUninstall.setEnabled(true);
                buttonInstall.setEnabled(true);
                buttonReboot.setEnabled(true);
                if (vers == 0) {
                    statusText.setText("Not installed");
                    buttonReboot.setEnabled(false);
                } else {
                    float check_vers = Float.valueOf(this.getString(R.string.version));
                    if (vers == check_vers) {
                        statusText.setText("Installed");
                    } else if (vers > check_vers) {
                        statusText.setText("Newer Version Installed");
                        buttonInstall.setEnabled(false);
                    } else {
                        statusText.setText("Old Version");
                    }
                }
                textSystemStatus.setText("Not Active");
                /* setMessage(check); */
                if (altbootmode) {
                    textSystemStatus.setText("Active");
                }
                if (writeProtect) {
                    statusText.setText("Write Protect Enabled");
                    buttonUninstall.setEnabled(false);
                    buttonInstall.setEnabled(false);
                    buttonReboot.setEnabled(true);
                    buttonRebootWriteProtect.setVisibility(View.VISIBLE);
                }
        	}
        	else {
                statusText.setText("Not Rooted");
                buttonUninstall.setEnabled(false);
                buttonInstall.setEnabled(false);
                buttonReboot.setEnabled(false);
        	}
        }
        finally {
        }
	}

	protected Dialog onCreateDialog(int id) {
    	// show disclaimer....
        // for example, you can show a dialog box... 
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.disclaimer);
        builder.setCancelable(false);
        builder.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // and, if the user accept, you can execute something like this:
                       // We need an Editor object to make preference changes.
                       // All objects are from android.context.Context
                       SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                       SharedPreferences.Editor editor = settings.edit();
                       editor.putBoolean("accepted", true);
                       // Commit the edits!
                       editor.commit();                    
                   }
               })
               .setNegativeButton("Disagree", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       //nm.cancel(R.notification.running); // cancel the NotificationManager (icon)
                        System.exit(0);
                   }
               });
        AlertDialog alert = builder.create();
        return alert;
    }
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	statusText = (TextView)findViewById(R.id.textInstallStatus);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if ( settings.getBoolean("accepted", false) == false ) {
            showDialog(0);
        }
        
        buttonInstall = (Button)findViewById(R.id.buttonInstall);
        buttonUninstall = (Button)findViewById(R.id.buttonUninstall);
        buttonReboot = (Button)findViewById(R.id.buttonReboot);
        buttonRebootWriteProtect = (Button)findViewById(R.id.buttonRebootWriteProtect);
        statusText = (TextView)findViewById(R.id.textInstallStatus);
        messageText = (TextView)findViewById(R.id.textView1);
        textSystemStatus = (TextView)findViewById(R.id.textSystemStatus);

        rootCheck = ExecuteAsRootBase.canRunRootCommands(); 
        if (rootCheck) {
        	/* For new MotoX we need to check write_protect=1 in cmdline */
            AssetControl unzip = new AssetControl();
    		unzip.apkPath= getPackageCodePath();
    		unzip.mAppRoot = getFilesDir().toString();
    		unzip.unzipAsset("/busybox");
    		unzip.unzipAsset("/recovery-check.sh");
    		unzip.unzipAsset("/recovery-reboot.sh");
    		unzip.unzipAsset("/ss_function.sh");
    		unzip.unzipAsset("/ss.config");
    		unzip = null;
        	ExecuteAsRootBase.executecmd("chmod 755 " + getFilesDir() + "/busybox");
        	ExecuteAsRootBase.executecmd("chmod 755 " + getFilesDir() + "/*.sh");
        	
            buttonInstall.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                	try {
                    	pDialog = new ProgressDialog(v.getRootView().getContext());
                    	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    	pDialog.setMessage("Installing...");
                    	pDialog.setCancelable(false);
                    	pDialog.show();
                    	pDialog.getCurrentFocus();
                    	installDialogThread = new InstallDialogThread();
                    	installDialogThread.packageCodePath = getPackageCodePath();
                    	installDialogThread.mAppRoot = getFilesDir();
                    	installDialogThread.LOGTAG = "Safestrap";
                    	installDialogThread.handler = new Handler() {
    	                    @Override
    	                    public void handleMessage(Message msg) {
    	                        if (msg.arg1 == 0) {
    	                        	pDialog.dismiss();
    	                            pDialog = null;
    	                            Toast.makeText(buttonInstall.getRootView().getContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
    		                        setupControls();
    	                        } else {
    	                            pDialog.setProgress(msg.arg2);
    	                        	pDialog.setMessage((String)msg.obj);
    	                        }
    	                    }
                    	};
                    	installDialogThread.start();
                	} catch(Exception ex) {
                		messageText.setText(ex.getMessage());
                	}
                }
            });

            buttonUninstall.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                	try {
    	            	pDialog = new ProgressDialog(v.getRootView().getContext());
    	            	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	            	pDialog.setMessage("Loading...");
    	            	pDialog.setCancelable(false);
    	            	pDialog.show();
    	            	pDialog.getCurrentFocus();
    	            	uninstallDialogThread = new UninstallDialogThread();
                    	uninstallDialogThread.packageCodePath = getPackageCodePath();
                    	uninstallDialogThread.mAppRoot = getFilesDir();
                    	uninstallDialogThread.LOGTAG = "Safestrap";
    	            	uninstallDialogThread.handler = new Handler() {
    	                    @Override
    	                    public void handleMessage(Message msg) {
    	                        if (msg.arg1 == 0) {
    	                        	pDialog.dismiss();
    	                            pDialog = null;
    	                            Toast.makeText(buttonUninstall.getRootView().getContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
    		                        setupControls();
    	                        } else {
    	                            pDialog.setProgress(msg.arg2);
    	                        	pDialog.setMessage((String)msg.obj);
    	                        }
    	                    }
    	            	};
    	            	uninstallDialogThread.start();
    	        	} catch(Exception ex) {
    	        		messageText.setText(ex.getMessage());
    	        	}
                }
            });

            buttonReboot.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
                	try {
    	            	ExecuteAsRootBase.executecmd("sh " + getFilesDir().toString() + "/recovery-reboot.sh " + getFilesDir().toString());
                    	Runtime.getRuntime().exec(new String[]{"su","-c","reboot now"});
    	        	} catch(Exception ex) {
    	        		messageText.setText(ex.getMessage());
    	        	}
                }
            });

            buttonRebootWriteProtect.setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
                	try {
    	            	ExecuteAsRootBase.executecmd("reboot recovery");
    	        	} catch(Exception ex) {
    	        		messageText.setText(ex.getMessage());
    	        	}
                }
            });
        }

        setupControls();
    }
}