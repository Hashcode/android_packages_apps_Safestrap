package com.hashcode.safestrap.ics;

import java.io.File;

import android.os.Handler;
import android.os.Message;

public class InstallDialogThread extends Thread {

	
	public Handler handler = null;
    public String packageCodePath = "";
    public File mAppRoot = null;
    public String LOGTAG = "";
 
        protected void pause(int milli) {
    		try {
    	    	Thread.sleep(milli);
    		}
    		catch(Exception ex) { }
    	}

        protected void reply(int arg1, int arg2, String text) {
        	Message msg = new Message();
        	msg.arg1 = arg1;
        	msg.arg2 = arg2;
        	msg.obj = (Object)text;
        	if (handler != null) { handler.sendMessage(msg); }
        }

        @Override
        public void run() {
        	try {
        		reply(1,0,"Preparing Installation...");
    		pause(2000);
    		AssetControl unzip = new AssetControl();
    		unzip.apkPath= packageCodePath;
    		unzip.mAppRoot = mAppRoot.toString();
    		unzip.LOGTAG = LOGTAG;
    		reply(1,0,"Unpacking Files...");
    		unzip.unzipAssets();
    		reply(1,50,"Checking Busybox...");
        	String filesDir = mAppRoot.getAbsolutePath();
        	ExecuteAsRootBase.executecmd("chmod 755 " + filesDir + "/busybox");
        	ExecuteAsRootBase.executecmd("chmod 755 " + filesDir + "/*.sh");
    		reply(1,60,"Running Installation Script...");
        	ExecuteAsRootBase.executecmd("sh " + filesDir + "/recovery-install.sh " + filesDir);
    		reply(1,90,"Cleaning Up...");
        	ExecuteAsRootBase.executecmd(filesDir + "/busybox rm -r " + filesDir + "/install-files");
        	pause(1000);
        	reply(0,0,"Installation Complete.");
    	}
    	catch(Exception ex) {
    		reply(0,1,ex.getMessage());
    	}
    }

}
