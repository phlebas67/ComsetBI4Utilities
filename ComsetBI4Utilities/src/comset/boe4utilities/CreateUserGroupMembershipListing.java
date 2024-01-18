/**
 * 
 */
package comset.boe4utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
/**
 * @author Administrator
 *
 */
public class CreateUserGroupMembershipListing implements IProgramBase{
	FileWriter fw;
	public static void main(String[] args) {
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
					CreateUserGroupMembershipListing cugl = new CreateUserGroupMembershipListing();
					cugl.run(boEnterpriseSession, boInfoStore, args);
				
				}  //end of if statement
	}
@SuppressWarnings("rawtypes")
public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		// Declare variables for the output file
		
		int writtenRecords = 0;
		String resultString="";
		String filePath="";
		String delimiter="";

		//Retrieve arguments if run from Command Line
		if (args.length == 6)
		{
				filePath = args[4];
				delimiter = args[5];
		}
			
		//Retrieve arguments if run from Command Line
		if (args.length == 2)
		{
				filePath = args[0];
				delimiter = args[1];
		}

		try {
			fw = new FileWriter(filePath);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		// Declare Query Variables
		IInfoObjects boUserInfoObjects=null;
		
		//Main Processing Block
		try{

		// Instantiate the InfoStore object
		boInfoStore = (IInfoStore) boEnterpriseSession.getService("", "InfoStore");
	    	

		// Define the repository query
	    //    boInfoObjects = (IInfoObjects)boInfoStore.query("SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS where SI_KIND = 'User'");
		String userStr = "SELECT SI_ID, SI_NAME, SI_USERFULLNAME, SI_NAMEDUSER FROM CI_SYSTEMOBJECTS where SI_KIND = 'User'";
				
		boUserInfoObjects = (IInfoObjects)boInfoStore.query(userStr);
		
		// Loop through the collection until no reports remain
		Iterator boUsers = boUserInfoObjects.iterator();

		while (boUsers.hasNext())
		{

			// Retrieve report from the collection
			IInfoObject boUser;
			boUser = (IInfoObject) boUsers.next();
			
			// Declare Query Variables
			IInfoObjects boGroupInfoObjects=null;
			
			// Build the Group Query String
			String groupStr = "Select SI_NAME From CI_SYSTEMOBJECTS Where PARENTS"
					+"(\"SI_NAME='UserGroup-User'\",\"SI_NAME='" + boUser.getTitle() + "'\")";// and SI_NAME != 'Everyone'";


			boGroupInfoObjects = (IInfoObjects)boInfoStore.query(groupStr);
			
			// Loop through the Group collection until no reports remain
			Iterator boGroups = boGroupInfoObjects.iterator();
			
			while (boGroups.hasNext())
			{
				// Retrieve report from the collection
				IInfoObject boGroup;
				boGroup = (IInfoObject) boGroups.next();
				
				// Build the output string
				resultString = boUser.getID()+delimiter+boUser.getTitle()+delimiter+boUser.properties().getProperty("SI_USERFULLNAME")+delimiter+boUser.properties().getProperty("SI_NAMEDUSER")+delimiter+boGroup.getID()+delimiter+boGroup.getTitle();
				
				// Write it to the output file
				try {
					fw.write(resultString);
					System.out.println(resultString);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// If another group exists, write a newline
				if (boGroups.hasNext() || boUsers.hasNext())
					try {
						fw.write("\r\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				
				// Update the written record count
				writtenRecords = writtenRecords+1;
				
			}// End of boGroups While Loop
			
		} // End of boUsers While Loop
	

		} // Try   	
	    catch(SDKException e)
	    {
	        try {
				fw.write(e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	    }

	
	    finally
	    {
		    try {
				fw.close();
				System.out.println(writtenRecords + " lines were written to " + filePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
		
	}// End of Run
}
