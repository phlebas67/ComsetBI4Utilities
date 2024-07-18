package comset.boe4utilities;


import java.util.Iterator;

import com.businessobjects.bcm.BCM;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;

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
		
		// Logon Information
		// To pass the arguments to the main method in the Eclipse IDE, set the values in the configuration. 
		// (Right Click the .java file > Run As > Run configuration > Arguments tab) 
		
		userName = args[0];
		password = args[1];
		cmsName = args[2];
		authType = args[3];
		
		if (((args.length == 5 )||(args.length == 1 )) && args[0] != null)   
		{
			try 
			{
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
			System.out.println("The function was expecting the following arguments: <username> <cmsname> <password> <authtype> <groupid>");
			System.out.println("Note: If executed from within BO via a Job Server then only <groupid> is required");
			System.exit(1);
		}


	}

	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		//First, parse arguments
		String groupID = null;
		
		//Retrieve arguments if run from Command Line
		if (args.length == 5)
		{
			groupID = args[4];	
		}
			
		//Retrieve arguments if run from Job Server
		else if (args.length == 1)
		{
			groupID = args[0];	
		}
		
		//Delete the group
		DeleteGroup(boInfoStore, groupID);
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
								
				boGroupInfoObjects.delete(groupToDelete);
				System.out.println("Deleted Group "+groupID+": "+groupToDelete.getTitle());
			}
			//Commit any deletions that were made
			boInfoStore.commit(boGroupInfoObjects);
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
}
