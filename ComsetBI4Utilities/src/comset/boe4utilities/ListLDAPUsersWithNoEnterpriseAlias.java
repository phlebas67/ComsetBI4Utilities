package comset.boe4utilities;

import com.businessobjects.bcm.BCM;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;

public class ListLDAPUsersWithNoEnterpriseAlias implements IProgramBase{
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
		
		if ((args.length == 4 )||(args.length == 0 ))   
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
			ListLDAPUsersWithNoEnterpriseAlias ldapNoEnterprise = new ListLDAPUsersWithNoEnterpriseAlias();
			ldapNoEnterprise.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement
		else
		{
			System.out.println("An incorrect number of parameters was entered");
			System.out.println("The function was expecting the following arguments: <username> <cmsname> <password>");
			System.out.println("Note: If executed from within BO via a Job Server then no arguments are required");
			System.exit(1);
		}


	}

	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		System.out.println("In the Run method of ListLDAPUsersWithNoEnterpriseAlias");
		
		// Run query to get list of users
		
		// Iterate through list of users
		
			// Set LDAP and Enterprise flags to False
		
			// Store UserId and Username
		
			// Check to see if user has an LDAP alias
				// If so, set LDAP flag to true
		
				// Now check to see if user has an Enterprise alias
					// If Enterprise alias, set flag to true
		
				// If LDAP flag = True and Enterprise Flag = False
					// Write UserID and Username to System Output
		
			
	}

}
