package comset.boe4utilities;

import java.io.FileReader;
import java.util.List;
import java.util.Set;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;


import au.com.bytecode.opencsv.*;

public class BulkLoadUsersFromCSV implements IProgramBase{
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
		if ((args.length >= 6 ) && args[0] != null)   
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
			BulkLoadUsersFromCSV csv = new BulkLoadUsersFromCSV();
			csv.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement

	} //end of main method

	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
		
		//First, parse arguments
		String csvFile = null;
		String csvSeparator = null;
		String addToGroup = null;

		//Retrieve arguments if run from Command Line
		if (args.length == 6)
		{
			csvFile = args[4];	
			csvSeparator = args[5];
			addToGroup = "Test SAML Group";
		}
			
		//Retrieve arguments if run from Job Server
		else if (args.length == 3)
		{
			csvFile = args[0];	
			csvSeparator = args[1];
			addToGroup = "Test SAML Group";
		}
		else
		{
			System.out.println("An incorrect number of parameters was entered");
			System.exit(1);
		}
		
		//Retrieve CSV data into a list
        List<String[]> csvData = null;
        csvData = readCSVFile(csvFile, csvSeparator);
        
        //Process List Contents
        //processList(boEnterpriseSession, boInfoStore, csvData, addToGroup);
        processSAMLUserList(boInfoStore, csvData);
       
	}
	
	private static List<String[]> readCSVFile(String file,String separator)
	{
		System.out.println("File to read: " + file);
		System.out.println("CSV Separator: " + separator);
		
        List<String[]> csvDataList = null;
		
		try {
	        // Create an object of file reader class with CSV file as a parameter. 
	        FileReader filereader = new FileReader(file);
	        
	        // create csvParser object with custom separator
	        char delim = separator.charAt(0);
	        CSVReader csvReader = new CSVReader(filereader,delim);
	        
	        //Read the file into a list
	        csvDataList = csvReader.readAll();
	        
	        //Close the readers
	        filereader.close();
	        csvReader.close();
	        
	        //Print out the list
	        //printList(csvDataList);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		return csvDataList;
	}
	
	private static void printList (List<String[]> listContents)
	{
		System.out.println("The file has the following content:");
        for (String[] row : listContents) { 
            /*for (String cell : row) { 
                System.out.print(cell + "\t");*/
        	String userID = row[2];
        	String userName = row[3];
        	String userPassword = row[4];
        	String userEmail = row[5];
        	
        	System.out.print("userID = "+ userID + " ");
        	System.out.print("userName = "+ userName + " ");
        	System.out.print("userPassword = "+ userPassword + " ");
        	System.out.print("userEmail = "+ userEmail);
            System.out.println(); 
            } 
	}
	private static void processSAMLUserList (IInfoStore boInfoStore, List<String[]> listContents)
	{
		for (String[] userrow : listContents)
		{ //Loop row by row down the list
			
			// Print out Processing information
			System.out.println("\n");
			System.out.print("Processing ");
			for (String cell : userrow)
				System.out.print(cell+ "\t");
			System.out.println("");
			
			// Extract user and group information from the row
			String userName = userrow[0];
			String userDesc = userrow[1];
			String userPassword = userrow[2];
			String userEmail = userrow[3];
			String userGroup = userrow[4];
			
			//Create the relevant user group
			processSAMLGroup(boInfoStore, userGroup);
			
			//Add user to group
			processSAMLUser(boInfoStore, userName, userDesc, userEmail, userPassword, userGroup);
		}
	}
	
	private static void processSAMLGroup(IInfoStore boInfoStore, String samlGroupName)
	{
		try {
			System.out.println("Processing group "+samlGroupName);

			// See if group already exists
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + samlGroupName +"'";
		
			IInfoObjects boGroupInfoObjects=null;
			boGroupInfoObjects = boInfoStore.query(queryString);

			if (boGroupInfoObjects.isEmpty())
			{
				//Group doesn't exist, so create it
				IInfoObjects newGroups = boInfoStore.newInfoObjectCollection();
				IUserGroup newUserGroup = (IUserGroup) newGroups.add(IUserGroup.KIND);
				
				//Set Group Properties
				newUserGroup.setTitle(samlGroupName);
				newUserGroup.setDescription("Created automatically from Bulk Upload routine");
				
				//Commit the changes
				boInfoStore.commit(newGroups);
				boGroupInfoObjects = newGroups;
				System.out.println("Created group "+newUserGroup.getTitle());

			}
			else System.out.println("Group "+samlGroupName+" already exists, so no need to create it");
			// Fetch an instance of the retrieved / newly created user group
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	private static void processSAMLUser(IInfoStore boInfoStore, String userName, String userTitle, String userEmail, String userPassword, String samlGroup)
	{
		/* 
		 * This routine:
		 * Tests to see if the user already exists in the system;
		 * Creates the user (if they don't already exist)
		 * Tests to see if the user is already a member of the specified group
		 * Adds the user to the group if they are not already a member
		 */
		try {
			System.out.println("Processing user: "+ userName);
			
			// See if the user already exists
        	
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_NAME = '" + userName +"'";
			
			//Execute Query
			IInfoObjects boUserObjects = boInfoStore.query(queryString);
			
			if (boUserObjects.isEmpty())
			{
				// Create a users collection
				IInfoObjects newUsers = boInfoStore.newInfoObjectCollection();

				//User doesn't exist, so create a new user
				IUser newUser = (IUser) newUsers.add(IUser.KIND);
				
				//Set the relevant user properties
				newUser.setTitle(userName);
				newUser.setFullName(userTitle);
				newUser.setConnection(IUser.CONCURRENT);
				newUser.setEmailAddress(userEmail);
				newUser.setNewPassword(userPassword);
				
				//Commit User
				boInfoStore.commit(newUsers);
				System.out.println("User " +newUser.getTitle()+" does not exist, so created with ID "+newUser.getID());
				boUserObjects = newUsers;
			}
			else
				System.out.println("User " + userName + " already exists in repository, so skipped user creation process.");
			
			//Add User to Group
			addUserToGroup(boInfoStore, userName, samlGroup);			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	private static void addUserToGroup(IInfoStore boInfoStore, String userName, String groupName) {
		/* This routine
		 * 	Tests to see if the specified user is a member of the specified group
		 * 	Adds them if they are not already a member of the group
		 */
		try {
			//Retrieve Group Object
			String queryString = "SELECT SI_ID, SI_NAME, SI_GROUP_MEMBERS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + groupName +"'";
			IInfoObjects groupCollection = boInfoStore.query(queryString);
			IUserGroup group = (IUserGroup) groupCollection.get(0);	

			//Retrieve User Object
			queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_NAME = '" + userName +"'";
			IInfoObjects userCollection = boInfoStore.query(queryString);
			IUser user = (IUser) userCollection.get(0);
			
			//Get the set of group members
			Set usersOfGroup = group.getUsers();
			
			
			if (!usersOfGroup.contains(user.getID()))
			{
				//User is not already a member of the group, so add them
				Integer groupID = group.getID();
				user.getGroups().add(groupID);
				boInfoStore.commit(userCollection);
				System.out.println("User "+user.getTitle()+" (ID="+user.getID()+"), added to group "+ group.getTitle());
			}
			else System.out.println("User "+user.getTitle()+" is already a member of "+ group.getTitle()+", so no need to add to group.");
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}
}


