package comset.boe4utilities;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.CeKind;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import	com.crystaldecisions.sdk.framework.*;


public class UploadAgnosticFile implements IProgramBase{
	
	public static void main(String[] args) 
	{
		// *****************
		// the main method is only going to be called when the jar is run from the command line. 
		// If the jar is run a a program file within BI, the entire main method will be skipped.  Keep this in mind when 
		// adding additional code to the main method.
		// *****************
		
		IEnterpriseSession boEnterpriseSession = null;
		ISessionMgr boSessionMgr = null;
		IInfoStore boInfoStore = null;
		String userName = null;
		String cmsName = null;
		String password = null;
		String authType = null;
		
		// Logon Information
		// To pass the arguments to the main method in the Eclipse IDE, set the values in the configuration. 
		// (Right Click the .java file > Run As > Run configuration > Arguments tab) 
		
		userName = args[0];
		password = args[1];
		cmsName = args[2];
		authType = args[3];

		
		// Enforce all expected cmd line arguments have been submitted. 
		// **Remember this is a sample, change values as needed.
		if ((args.length >= 4 ) && args[0] != null)   
		{
			try 
			{
				// Initialize the Session Manager 
				boSessionMgr = CrystalEnterprise.getSessionMgr();
	
				// Logon to the Session Manager to create a new BOE session.
				boEnterpriseSession = boSessionMgr.logon(userName, password, cmsName, authType);
				System.out.println("user \"" + userName + "\" logged in via main() method");
				
				//Retrieve the InfoStore object
				boInfoStore = (IInfoStore) boEnterpriseSession.getService("", "InfoStore");

				
			}
			catch (SDKException sdke)
			{
				System.out.println(sdke.getMessage());
				System.exit(1);
			}
			
			//call the run() method
			UploadAgnosticFile uaf = new UploadAgnosticFile();
			uaf.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement
	} //end of main method
	
	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		// Declare variables for the output file
		
		String filename="";
		String filePath="";
		String enterpriseFolder="";
		String infoObjectType="";
		String queryString="";

		//Retrieve arguments if run from Command Line
		if (args.length == 8)
		{
			filename = args[4];	
			filePath = args[5];
			enterpriseFolder = args[6];
			infoObjectType = args[7];
		}
			
		//Retrieve arguments if run from Job Server
		if (args.length == 4)
		{
			filename = args[0];	
			filePath = args[1];
			enterpriseFolder = args[2];
			infoObjectType = args[3];
		}

		// Declare Query Variables
		queryString="Select TOP 1 SI_ID From CI_INFOOBJECTS Where SI_KIND='Folder' And SI_NAME='" + enterpriseFolder + "'";
			
		//Main Processing Block
		try{

				int folderID;
				IInfoObjects infoObjects;
				IInfoObject infoObject;
				
				// Instantiate the InfoStore object
				boInfoStore = (IInfoStore) boEnterpriseSession.getService("", "InfoStore");
			    	
				// Execute the repository query
				folderID = ((IInfoObject) boInfoStore.query(queryString).get(0)).getID();
				infoObjects = boInfoStore.newInfoObjectCollection();
				
			    infoObject = infoObjects.add(infoObjectType);
			    infoObject.setTitle(filename);
			    infoObject.getFiles().addFile(filePath);
			    infoObject.setParentID(folderID);
			    boInfoStore.commit(infoObjects);
		    
			}    	
		    catch(SDKException e)
		    {
	        	System.out.println(e.getMessage());
		    }
		    finally
		    {
				System.out.println("Added " + filePath + " to the BO folder " + enterpriseFolder);
		    }
		
	}

}
