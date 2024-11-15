package comset.boe4utilities;


import java.util.Iterator;
import java.util.List;

import com.businessobjects.bcm.BCM;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DeleteOrphanedGroups implements IProgramBase{

	public static void main(String[] args) {
		// TODO Auto-generated method stub
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
		
		// Parse the arguments
		System.out.println("The number of arguments passed to the function was "+ Integer.toString(args.length));
		for (int i=0; i < args.length; i++)
			System.out.println("args[" + Integer.toString(i) + "] = "+ args[i]);
		
		
		if ((args.length == 6 )||(args.length == 2 ))   
		{
			String runmode = args[args.length - 2].toUpperCase();
			
			if ( !"CF".contains(runmode))
			{
				System.out.println("The <run mode> parameter must be c,C,f or F");
				System.exit(1);
			}

			try 
			{
				userName = args[0];
				password = args[1];
				cmsName = args[2];
				authType = args[3];

				BCM.initializeSAPJCE();
				
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
			DeleteOrphanedGroups dog = new DeleteOrphanedGroups();
			dog.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement
		else
		{
			System.out.println("An incorrect number of parameters was entered");
			System.out.println("The function was expecting the following arguments: <username> <cmsname> <password> <authtype> <run mode> <groupid>");
			System.out.println("Note: If executed from within BO via a Job Server then only <run mode> and <groupid> is required");
			System.exit(1);
		}


	}

	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		//First, decipher which run mode has been specified - File or Command Line
		String runMode = args[args.length -2];
		
		// Check if Command-Line
		if (runMode.equalsIgnoreCase("C"))
		{
			// Retrieve Group ID from arguments
			String groupID = args[args.length -1];
			// Delete Group
			DeleteGroup(boInfoStore, groupID);
		}
		else // Otherwise, we are in File mode
		{
			// Retrieve the requested filename
			String txtFile = args[args.length -1];
			
			// Declare a list of strings to hold the file contents
			List<String> groupsToDelete = null;
			
			// Read the file and store it in our list
			groupsToDelete = readTXTFile(txtFile);
			
			// Check that the file was not empty
			if (!groupsToDelete.isEmpty())
			{
				System.out.println("Successfully read the contents of " + txtFile);
				// Iterate down each group id
				for (String groupId : groupsToDelete)
					// Delete the group
					DeleteGroup(boInfoStore, groupId);
			}
			else
				System.out.println(txtFile + " was empty!");
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static void DeleteGroup(IInfoStore boInfoStore, String groupID) {
		/*  This routine deletes the group with the passed ID
		 */
		try {
			System.out.println("");
			System.out.println("Checking to see if the group with ID " + groupID + " exists");
			
			//Retrieve All SAML Group Object
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_ID = " + groupID;
			
			IInfoObjects boGroupInfoObjects=null;
			boGroupInfoObjects = boInfoStore.query(queryString);
			
			//Get the set of group members
			Iterator groupsIterator = boGroupInfoObjects.iterator();
			
			if (!groupsIterator.hasNext()){
				System.out.println("Group not found!");
				return;
			}

			// Loop through the list of subgroups
			while (groupsIterator.hasNext())
			{
				//Retrieve Group
				IUserGroup groupToDelete = (IUserGroup) groupsIterator.next();
				String groupName = groupToDelete.getTitle();
								
				boGroupInfoObjects.delete(groupToDelete);
				System.out.println("Deleted Group "+groupID+": "+groupName);
			}
			//Commit any deletions that were made
			boInfoStore.commit(boGroupInfoObjects);
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	// Routine to read the contents of the specified filepath
	private static List<String> readTXTFile(String filepath){
		
		//Declare a list object to hold the file contents
		List<String> content = null;
		try {
			// Read the file
			content = Files.readAllLines(Paths.get(filepath));
 		} catch(IOException e) {
			System.out.print("The file " + filepath + " was not found!");
			System.exit(1);
		}
		return content;
	}
}
