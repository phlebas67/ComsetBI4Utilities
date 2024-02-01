package comset.boe4utilities;

import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
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
		
		//Retrieve arguments if run from Command Line
		if (args.length == 6)
		{
			csvFile = args[4];	
			csvSeparator = args[5];
		}
			
		//Retrieve arguments if run from Job Server
		else if (args.length == 3)
		{
			csvFile = args[0];	
			csvSeparator = args[1];
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
        processSAMLList(boInfoStore, csvData, "SAMLGROUP_","SAML - All User Groups","SAML - Non-Mapped User Groups");
       
	}
	private static void processSAMLList(IInfoStore boInfoStore, List<String[]> listContents, String groupPrefix, String samlParentGroupName, String samlUnmatchedParentGroupName) {

		//Create top-level SAML group (if it doesn't already exist
		createTopLevelSAMLGroup(boInfoStore, samlParentGroupName);
		
		//Create a top-level SAML group to capture all non-matched SAML Groups
		createTopLevelSAMLGroup(boInfoStore, samlUnmatchedParentGroupName);

		
		//Create a set of the unique group names in the file
		HashSet<String> processedGroups = new HashSet<String>();

		//Loop down each row of the file
		for (String[] userrow : listContents)
		{
			//Build group name
			String groupName = userrow[4];
			String samlGroupName = groupPrefix+groupName;
			
			//Check to see if group has already been processed
			if (!processedGroups.contains(groupName))
			{
				//Create the relevant user group
				processSAMLGroup(boInfoStore, groupPrefix, groupName, samlParentGroupName,samlUnmatchedParentGroupName);
				
				//Add group to the set of processed groups
				processedGroups.add(groupName);
			}
		}
		
		//Create top-level SAML group (if it doesn't already exist
		//CreateTopLevelSAMLGroup(boInfoStore, samlParentGroupName);
		
		//Create a top-level SAML group to capture all non-matched SAML Groups
		//createTopLevelSAMLGroup(boInfoStore, samlUnmatchedParentGroupName);
		
		


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
	
	@SuppressWarnings("unused")
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
		String groupPrefix ="SAMLGROUP_";
		String samlParentGroupName ="SAML - All User Groups";
		String samlUnmatchedParentGroupName ="SAML - Non-Mapped User Groups";

		//Create top-level SAML group (if it doesn't already exist
		createTopLevelSAMLGroup(boInfoStore, samlParentGroupName);
		//Create a top-level SAML group to capture all non-matched SAML Groups
		createTopLevelSAMLGroup(boInfoStore, samlUnmatchedParentGroupName);
		
		//Loop row by row down the list
		for (String[] userrow : listContents)
		{
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
			
			String samlUserGroupName = groupPrefix+userGroup;
			
			//Create the relevant user group
			processSAMLGroup(boInfoStore, groupPrefix, userGroup, samlParentGroupName,samlUnmatchedParentGroupName);
			
			//Add user to group
			processSAMLUser(boInfoStore, userName, userDesc, userEmail, userPassword, samlUserGroupName);
		}
	}
	
	private static void processSAMLGroup(IInfoStore boInfoStore, String groupPrefix, String samlGroupName, String toplevelSAMLGroupName, String toplevelUnmappedSAMLGroupName)
	{
		try {
			String userGroupWithPrefix = groupPrefix+samlGroupName;

			System.out.println("Processing group "+userGroupWithPrefix);

			// See if group already exists
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + userGroupWithPrefix +"'";
		
			IInfoObjects boGroupInfoObjects=null;
			boGroupInfoObjects = boInfoStore.query(queryString);

			if (boGroupInfoObjects.isEmpty())
			{
				//Group doesn't exist, so create it
				IInfoObjects newGroups = boInfoStore.newInfoObjectCollection();
				IUserGroup newUserGroup = (IUserGroup) newGroups.add(IUserGroup.KIND);
				
				//Set Group Properties
				newUserGroup.setTitle(userGroupWithPrefix);
				newUserGroup.setDescription("Created automatically from Bulk Upload routine");
				
				//Commit the changes
				boInfoStore.commit(newGroups);
				boGroupInfoObjects = newGroups;
				System.out.println("Created group "+newUserGroup.getTitle());

				//Add group to top-level Parent Group
				addSubgroupToParentGroup(boInfoStore, userGroupWithPrefix, toplevelSAMLGroupName);
				
				//Find sibling groups
				matchSiblingGroups(boInfoStore, samlGroupName, groupPrefix, toplevelUnmappedSAMLGroupName);

			}
			else System.out.println("Group "+userGroupWithPrefix+" already exists, so no need to create it");
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
				newUser.setDescription("Created automatically from Bulk Upload routine");
				
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
	
	private static void createTopLevelSAMLGroup(IInfoStore boInfoStore,String samlParentGroup) {
	// This routine checks / creates the parent group for all the other groups
		try {
			System.out.println("Checking / Creating Top Level SAML GroupProcessing group "+samlParentGroup+"\n");

			// See if group already exists
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + samlParentGroup +"'";
		
			IInfoObjects boGroupInfoObjects=null;
			boGroupInfoObjects = boInfoStore.query(queryString);

			if (boGroupInfoObjects.isEmpty())
			{
				//Group doesn't exist, so create it
				IInfoObjects newGroups = boInfoStore.newInfoObjectCollection();
				IUserGroup newUserGroup = (IUserGroup) newGroups.add(IUserGroup.KIND);
				
				//Set Group Properties
				newUserGroup.setTitle(samlParentGroup);
				newUserGroup.setDescription("Top-Level Group containing all automatically bulk-created SAML groups");
				
				//Commit the changes
				boInfoStore.commit(newGroups);
				boGroupInfoObjects = newGroups;
				System.out.println("Created group "+newUserGroup.getTitle());

			}
			else System.out.println("Group "+samlParentGroup+" already exists, so no need to create it");
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	@SuppressWarnings("unchecked")
	private static void addSubgroupToParentGroup(IInfoStore boInfoStore, String subGroupName, String parentGroupName) {
		try {
			
			//Retrieve Sub-group Object
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + subGroupName +"'";
			IInfoObjects subgroupCollection = boInfoStore.query(queryString);
			IUserGroup subgroup = (IUserGroup) subgroupCollection.get(0);	

			//Retrieve Parent-group Object
			queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + parentGroupName +"'";
			IInfoObjects parentgroupCollection = boInfoStore.query(queryString);
			IUserGroup parentgroup = (IUserGroup) parentgroupCollection.get(0);	
			
			//Add subgroup to parent group
			parentgroup.getSubGroups().add(subgroup.getID());
			boInfoStore.commit(parentgroupCollection);
			
			System.out.println("Added group "+subGroupName+" to group "+parentGroupName);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	@SuppressWarnings({ "rawtypes"})
	private static void matchSiblingGroups(IInfoStore boInfoStore, String groupName, String samlGroupPrefix, String samlUnmatchedParentGroupName) {
		
		try {
			//First Find similar Group Names
			String matchqueryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME Like '%" + groupName +"%' AND SI_NAME != '"+ samlGroupPrefix+groupName+"'";
			IInfoObjects matchgroupCollection = boInfoStore.query(matchqueryString);
			
			// Loop through the collection until no reports remain
			Iterator matchingGroupIter = matchgroupCollection.iterator();
			
			if (!matchingGroupIter.hasNext()) 
				//No matching groups found, so add this group to the Unamtched SAML group
				addSubgroupToParentGroup(boInfoStore, samlGroupPrefix+groupName, samlUnmatchedParentGroupName);

			while (matchingGroupIter.hasNext()) {
				IInfoObject	matchinggroup = (IInfoObject) matchingGroupIter.next();
				
				//Find Parents of Matching Group
				String parentsqueryString = "SELECT SI_ID, SI_NAME, SI_USERGROUPS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + matchinggroup.getTitle() +"'";
				IUserGroup group = (IUserGroup) boInfoStore.query(parentsqueryString).get(0);
				
				//Retrieve the set of Parent Groups
				Set parentGroups =group.getParentGroups();
				
				//Loop through the parents
				Iterator parentGroupIter = parentGroups.iterator();
				
				while (parentGroupIter.hasNext()) {
					Integer parentGroupID = (Integer) parentGroupIter.next();

					//Retrieve Parent Group
					parentsqueryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_ID =" +parentGroupID;
					IUserGroup parentGroup = (IUserGroup) boInfoStore.query(parentsqueryString).get(0);
					
					//Add group to the matched group's parent
					addSubgroupToParentGroup(boInfoStore, samlGroupPrefix+groupName, parentGroup.getTitle());
				}

			}
			
		}
		catch (SDKException e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

}


