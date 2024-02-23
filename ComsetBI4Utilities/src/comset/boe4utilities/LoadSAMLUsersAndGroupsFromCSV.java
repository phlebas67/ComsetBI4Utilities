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
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAlias;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroup;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.IUserGroupAlias;

import au.com.bytecode.opencsv.*;

import com.businessobjects.bcm.BCM;

public class LoadSAMLUsersAndGroupsFromCSV implements IProgramBase{
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
		
		if (((args.length == 6 )||(args.length == 2 )) && args[0] != null)   
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
			LoadSAMLUsersAndGroupsFromCSV csv = new LoadSAMLUsersAndGroupsFromCSV();
			csv.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement
		else
		{
			System.out.println("An incorrect number of parameters was entered");
			System.out.println("The function was expecting the following arguments: <username> <cmsname> <password> <authtype> <csvfile> <csv separator char>");
			System.out.println("Note: If executed from within BO via a Job Server then only <csvfile> <csv separator char> are required");
			System.exit(1);
		}

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
		else if (args.length == 2)
		{
			csvFile = args[0];	
			csvSeparator = args[1];
		}
		
		//Retrieve CSV data into a list
        List<String[]> csvData = null;
        csvData = readCSVFile(csvFile, csvSeparator);
        
        //Process list if not empty
        if (!csvData.isEmpty())
            //Process List Contents
            processSAMLList(boInfoStore, csvData);
        else
        	System.out.println("\"" + csvFile + "\" was empty, so nothing was processed!");
       
	}
	private static void processSAMLList(IInfoStore boInfoStore, List<String[]> listContents) {

		//Define positions of fields in file
		final int groupNameField = 0;
		final int userNameField = 1;
		final int userEmailField = 2;
		final int userFullNameField = 3;
		
		//Define a constant Password for the user
		final String userPasswordValue = "Passw0rd!";
		
		//Build top-level group names
		final String groupPrefix = "NON-MFIL_SAMLGROUP_";
		final String AllParentGroupSuffix = "All User Groups";
		final String UnmatchedParentGroupSuffix = "Non-Mapped User Groups";
		final String AllUsersGroupSuffix = "All Users";
		
		String samlParentGroupName = groupPrefix+AllParentGroupSuffix;
		String samlUnmatchedParentGroupName = groupPrefix+UnmatchedParentGroupSuffix;
		String samlAllUsersGroupName = groupPrefix+AllUsersGroupSuffix;
	
		
		//Create top-level SAML group (if it doesn't already exist
		createTopLevelSAMLGroup(boInfoStore, samlParentGroupName);
		
		//Create a top-level SAML group to capture all non-matched SAML Groups
		createTopLevelSAMLGroup(boInfoStore, samlUnmatchedParentGroupName);

		//Create a top-level SAML group to contain all SAML Users
		createTopLevelSAMLGroup(boInfoStore, samlAllUsersGroupName);

		
		//Create a set of the unique group names in the file
		HashSet<String> processedGroups = new HashSet<String>();

		int rownumber=0;
		
		//Loop down each row of the file
		for (String[] userrow : listContents)
		{
			//Increment rownumber
			rownumber++;

			//Build group name
			String groupName = userrow[groupNameField];
			
			if (groupName.isEmpty()){
				System.out.println("");
				System.out.println("ROW " + rownumber + " HAS AN EMPTY GROUPNAME, SO SKIPPING IT!");
				System.out.println("");
				continue;
			}

			
			//Check to see if group has already been processed
			if (!processedGroups.contains(groupName))
			{
				//Create the relevant user group
				processSAMLGroup(boInfoStore, groupPrefix, groupName, samlParentGroupName,samlUnmatchedParentGroupName);
				
				//Add group to the set of processed groups
				processedGroups.add(groupName);
			}
		}
		
		//Process Users in File
		//Create a set of the unique usernames in the file
		HashSet<String> processedUsers = new HashSet<String>();

		//Rest row number
		rownumber=0;
		
		//Loop down each row of the file
		for (String[] userrow : listContents)
		{
			//Increment rownumber
			rownumber++;
			
			//Extract User Attributes
			String userName = userrow[userNameField];
			
			if (userName.isEmpty()){
				System.out.println("");
				System.out.println("ROW " + rownumber + " HAS AN EMPTY USERNAME, SO SKIPPING IT!");
				System.out.println("");
				continue;
			}
			
			//Check to see if user has already been created
			if (!processedUsers.contains(userName))
			{
				String userTitle = userrow[userFullNameField];
				String userPassword = userPasswordValue;
				String userEmail = userrow[userEmailField];
				

				//Create the relevant user
				createUser(boInfoStore, userName, userTitle, userEmail, userPassword);

				//Add user to all SAML Users group
				addUserToGroup(boInfoStore, userName, samlAllUsersGroupName);

				//Add user to the set of processed users
				processedUsers.add(userName);
			}
			String userGroup= userrow[groupNameField];
			
			//Add user to user group
			addUserToGroup(boInfoStore, userName, groupPrefix+userGroup);
			
		}
		
		// Perform Tidy-up routines to handle removed users / groups
		disableRemovedUsers(boInfoStore, processedUsers, samlAllUsersGroupName);
		removeOldGroups(boInfoStore, processedGroups, samlParentGroupName, groupPrefix);
		cleanGroupMembership(boInfoStore,samlParentGroupName, groupPrefix, listContents);
		
		
	}
	
	private static List<String[]> readCSVFile(String file,String separator)
	{
		System.out.println("\nReading file: \"" + file +"\" with separator \""+ separator+"\"\n");
		
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
	
	private static void processSAMLGroup(IInfoStore boInfoStore, String groupPrefix, String samlGroupName, String toplevelSAMLGroupName, String toplevelUnmappedSAMLGroupName)
	{
		try {
			String userGroupWithPrefix = groupPrefix+samlGroupName;

			System.out.println("\nProcessing group "+userGroupWithPrefix);

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

			}
			else System.out.println("Group "+userGroupWithPrefix+" already exists, so no need to create it");
			// Fetch an instance of the retrieved / newly created user group
			
			//Find sibling groups
			//matchSiblingGroups(boInfoStore, samlGroupName, groupPrefix, toplevelUnmappedSAMLGroupName);
			matchSiblingLDAPGroups(boInfoStore, samlGroupName, groupPrefix, toplevelUnmappedSAMLGroupName);
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
		
	private static void createUser(IInfoStore boInfoStore, String userName, String userTitle, String userEmail, String userPassword)
	{
		/* 
		 * This routine:
		 * Tests to see if the user already exists in the system;
		 * Creates the user (if they don't already exist)
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
				System.out.println("User "+user.getTitle()+" added to group "+ group.getTitle());
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
			System.out.println("Checking / Creating Top Level SAML Group "+samlParentGroup);

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addSubgroupToParentGroup(IInfoStore boInfoStore, String subGroupName, String parentGroupName) {
		try {
			
			//Retrieve Sub-group Object
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + subGroupName +"'";
			IInfoObjects subgroupCollection = boInfoStore.query(queryString);
			IUserGroup subgroup = (IUserGroup) subgroupCollection.get(0);	

			//Retrieve Parent-group Object
			queryString = "SELECT SI_ID, SI_NAME, SI_SUBGROUPS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + parentGroupName +"'";
			IInfoObjects parentgroupCollection = boInfoStore.query(queryString);
			IUserGroup parentgroup = (IUserGroup) parentgroupCollection.get(0);	
			
			//Add subgroup to parent group
			Set subGroupsSet = parentgroup.getSubGroups();
			
			//See if subgroup is already a subgroup of the parent
			if (subGroupsSet.contains(subgroup.getID()))
				System.out.println(subGroupName + " is already a subgroup of " + parentGroupName + ", so no need to add it again.");
			else {
				// Add subgroup to parent group
				parentgroup.getSubGroups().add(subgroup.getID());
				boInfoStore.commit(parentgroupCollection);
				
				System.out.println("Added group "+subGroupName+" to group "+parentGroupName);
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	@SuppressWarnings("rawtypes")
	private static void matchSiblingLDAPGroups(IInfoStore boInfoStore, String groupName, String samlGroupPrefix, String samlUnmatchedParentGroupName) {
		
		try {
			//Find similar Group Names
			System.out.println("Looking for matching LDAP groups to " + groupName);
			String matchqueryString = "SELECT SI_ID, SI_NAME, SI_ALIASES FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME Like '%CN=" + groupName +",CN%' AND SI_NAME != '"+ samlGroupPrefix+groupName+"'";
			IInfoObjects matchgroupCollection = boInfoStore.query(matchqueryString);
			
			// Loop through the matching group collection
			Iterator matchingGroupIter = matchgroupCollection.iterator();
			
			if (!matchingGroupIter.hasNext()) {
				//No matching groups found, so add this group to the Unmatched SAML group
				System.out.println("No matching groups found, so adding group to the unmatched groups bucket!");
				addSubgroupToParentGroup(boInfoStore, samlGroupPrefix+groupName, samlUnmatchedParentGroupName);
			}

			Boolean ldapAliasFound = false;
			
			while (matchingGroupIter.hasNext()) {
				// A matching group name has been found. Now check to see if it is an LDAP alias
				IUserGroup	matchinggroup = (IUserGroup) matchingGroupIter.next();
				
				//Check Aliases associated with Group
				Iterator groupAliases = (Iterator) matchinggroup.getAliases().iterator();
				
				String aliasName="";
				
				while (groupAliases.hasNext()) {
					
					IUserGroupAlias alias = (IUserGroupAlias) groupAliases.next();
					aliasName = alias.getName();
					
					//Test to see if this is a matching LDAP alias
					if (alias.getType() == IUserGroupAlias.THIRD_PARTY && aliasName.contains("secLDAP:cn="+groupName.toLowerCase()+",")) {
						System.out.println("Matching LDAP alias found: "+aliasName);
						ldapAliasFound = true;
					}
				}
				if (!ldapAliasFound) {
					System.out.println("A group with a matching name was found " + aliasName + ", but it wasn't an LDAP alias, so ignoring it!");
					addSubgroupToParentGroup(boInfoStore, samlGroupPrefix+groupName, samlUnmatchedParentGroupName);
				}
				else
				{
					//Find Parents of Matching Group
					System.out.println("Looking for parent groups of: " + matchinggroup.getTitle());
					String parentsqueryString = "SELECT SI_ID, SI_NAME, SI_USERGROUPS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + matchinggroup.getTitle() +"'";
					IUserGroup group = (IUserGroup) boInfoStore.query(parentsqueryString).get(0);
					
					//Retrieve the set of Parent Groups
					Set parentGroups =group.getParentGroups();
					
					//Loop through the parents
					Iterator parentGroupIter = parentGroups.iterator();
					
					while (parentGroupIter.hasNext()) {
						Integer parentGroupID = (Integer) parentGroupIter.next();

						//Retrieve Parent Group
						parentsqueryString = "SELECT SI_ID, SI_NAME, SI_SUBGROUPS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_ID =" +parentGroupID;
						IUserGroup parentGroup = (IUserGroup) boInfoStore.query(parentsqueryString).get(0);
						System.out.println("Found parent group: " + parentGroup.getTitle());
						
						//Add group to the matched group's parent
						addSubgroupToParentGroup(boInfoStore, samlGroupPrefix+groupName, parentGroup.getTitle());
					}
					
				}	
			}
		}
		catch (SDKException e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static void disableRemovedUsers(IInfoStore boInfoStore, HashSet<String> processedUsers, String allUsersGroupName) {
		/*  This routine disables any users that are members of the all SAML users group
		 * 	but who do not appear in the processed CSV file
		 */
		try {
			System.out.println("");
			System.out.println("Checking for any SAML users who need to be disabled, as they don't appear in the current CSV file..");
			
			//Retrieve Group Object
			String queryString = "SELECT SI_ID, SI_NAME, SI_GROUP_MEMBERS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + allUsersGroupName +"'";
			IInfoObjects groupCollection = boInfoStore.query(queryString);
			IUserGroup group = (IUserGroup) groupCollection.get(0);	
		
			//Get the set of group members
			Iterator allSAMLUsersIterator = group.getUsers().iterator();
			
			// Loop through the list of all SAML Users
			while (allSAMLUsersIterator.hasNext())
			{
				// Retrieve User Object
				Integer samlUserID = (Integer) allSAMLUsersIterator.next();
				queryString = "SELECT SI_ID, SI_NAME, SI_ALIASES FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_ID = " + samlUserID;
				IInfoObjects userCollection = boInfoStore.query(queryString);
				IUser samlUser = (IUser) userCollection.get(0);
				
				//Check whether username is in the list of Processed Users
				if (!processedUsers.contains(samlUser.getTitle()))
				{
					//Retrieve list of aliases
					Iterator aliasIterator = samlUser.getAliases().iterator();
					while (aliasIterator.hasNext())
					{
						IUserAlias userAlias = (IUserAlias) aliasIterator.next();
						if (userAlias.getType() == IUserAlias.ENTERPRISE)
							userAlias.setDisabled(true);
					}
					
					boInfoStore.commit(userCollection);
					System.out.println("User " + samlUser.getTitle() + " does not appear in the current CSV file, so the user has been disabled");
				}
			}
						
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

	@SuppressWarnings("rawtypes")
	private static void removeOldGroups(IInfoStore boInfoStore, HashSet<String> processedGroups, String allGroupsName, String groupPrefix) {
		/*  This routine removes any SAML groups that were created in previous runs of the utility
		 * 	but who do not appear in the current  CSV file
		 */
		try {
			System.out.println("");
			System.out.println("Checking for any SAML users groups that need to be deleted, as they were previously created in historic runs of the utility, but don't appear in the current CSV file..");
			
			//Retrieve All SAML Group Object
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS where children (\"si_name = 'usergroup-user'\", \"si_name = '" + allGroupsName + "'\") and si_kind = 'UserGroup'";
			IInfoObjects groupCollection = boInfoStore.query(queryString);
		
			//Get the set of group members
			Iterator groupsIterator = groupCollection.iterator();
			
			// Loop through the list of subgroups
			while (groupsIterator.hasNext())
			{
				//Retrieve sub-group
				IUserGroup subgroup = (IUserGroup) groupsIterator.next();
				
				//Extract relevant part of group name
				String groupName = subgroup.getTitle().substring(groupPrefix.length());
				
				//Check to see if this group is the list of processed groups in this CSV
				if (!processedGroups.contains(groupName)) {
					System.out.println(groupName + " was not found in this execution of the CSV file, so it will be deleted!");
					groupCollection.delete(subgroup);
				}
			}
			//Commit any deletions that were made
			boInfoStore.commit(groupCollection);
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

	@SuppressWarnings("rawtypes")
	private static void cleanGroupMembership(IInfoStore boInfoStore, String allGroupsName, String groupPrefix, List<String[]> listContents) {
		/*  This routine checks each SAML group to check current group membership,
		 * 	it will remove any existing group members who are not in the current CSV file
		 */
		//Define positions of fields in file
		final int groupNameField = 0;
		final int userNameField = 1;

		try {
			System.out.println("");
			System.out.println("Checking each SAML group to check current group membership - it will remove any existing group members who are not in the current CSV file..");
			
			//Retrieve All SAML Group Object
			String queryString = "SELECT SI_ID, SI_NAME, SI_GROUP_MEMBERS FROM CI_SYSTEMOBJECTS where children (\"si_name = 'usergroup-user'\", \"si_name = '" + allGroupsName + "'\") and si_kind = 'UserGroup'";
			IInfoObjects groupCollection = boInfoStore.query(queryString);
		
			//Get the set of group members
			Iterator groupsIterator = groupCollection.iterator();
			
			// Loop through the list of subgroups
			while (groupsIterator.hasNext())
			{
				//Retrieve sub-group
				IUserGroup subgroup = (IUserGroup) groupsIterator.next();
				
				//Extract relevant part of group name
				String groupName = subgroup.getTitle().substring(groupPrefix.length());
				
				//Build a set of the users in the CSV file that belong to this group
				HashSet<String> csvUsers = new HashSet<String>();
				
				//Check each row of the CSV data
				for (String[] csvRow : listContents) {
					
					//Extract username and group from row
					String csvGroup = csvRow[groupNameField];
					String csvUser = csvRow[userNameField];
					
					// If the extracted user group is the same as the current sub-group,
					// then add the extracted user to the set of users
					if (csvGroup.equalsIgnoreCase(groupName))
						csvUsers.add(csvUser);
				}
				
				//Retrieve the list of user IDs who belong to the sub-group
				Set usersOfGroup = subgroup.getUsers();
				Iterator userIterator = usersOfGroup.iterator();
				
				//Iterate down the list of users of the group
				while (userIterator.hasNext()) {
					Integer userID = (Integer) userIterator.next();
					
					//Retrieve User information
					queryString = "SELECT SI_ID, SI_NAME, SI_USERGROUPS FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_ID = " + userID;
					IInfoObjects userCollection = boInfoStore.query(queryString);
					IUser user = (IUser) userCollection.get(0);
					String userName = user.getTitle();
					
					//Check to see if user is not a member of the processed users
					if (!csvUsers.contains(userName)) {
						user.getGroups().remove(subgroup.getID());
						boInfoStore.commit(userCollection);
						System.out.println("User " + userName + " was removed from group "+subgroup.getTitle());
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

}


