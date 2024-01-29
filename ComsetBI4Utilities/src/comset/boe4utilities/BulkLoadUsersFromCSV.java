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
		if ((args.length >= 7 ) && args[0] != null)   
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
		if (args.length == 7)
		{
			csvFile = args[4];	
			csvSeparator = args[5];
			addToGroup = args[6];
		}
			
		//Retrieve arguments if run from Job Server
		else if (args.length == 3)
		{
			csvFile = args[0];	
			csvSeparator = args[1];
			addToGroup = args[2];
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
        processList(boEnterpriseSession, boInfoStore, csvData, addToGroup);
       
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
	private static void processList (IEnterpriseSession boEnterpriseSession,IInfoStore boInfoStore, List<String[]> listContents, String groupName)
	{
		try {
			// See if group already exists
			String queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'UserGroup' AND SI_NAME = '" + groupName +"'";
		
			IInfoObjects boGroupInfoObjects=null;
			boGroupInfoObjects = boInfoStore.query(queryString);
			
			if (boGroupInfoObjects.isEmpty())
			{
				//Group doesn't exist, so create it
				IInfoObjects newGroups = boInfoStore.newInfoObjectCollection();
				IUserGroup newUserGroup = (IUserGroup) newGroups.add(IUserGroup.KIND);
				
				//Set Group Properties
				newUserGroup.setTitle(groupName);
				newUserGroup.setDescription("Created automatically from Bulk Upload routine");
				
				//Commit the changes
				boInfoStore.commit(newGroups);
				boGroupInfoObjects = newGroups;
				System.out.println("Created group "+groupName);

			}
			// Fetch an instance of the retrieved / newly created user group
			IUserGroup addUsersGroup = (IUserGroup) boGroupInfoObjects.get(0);
			
			// Create users container
			Set usersOfGroup = addUsersGroup.getUsers();
			
			// Create a users collection
			IInfoObjects newUsers = boInfoStore.newInfoObjectCollection();
			
			//Process Users
			for (String[] userrow : listContents) { //Loop row by row down the list
	        	String userName = userrow[2];
	        	String userTitle = userrow[3];
	        	String userPassword = userrow[4];
	        	String userEmail = userrow[5];
	        	
				// See if the user already exists
	        	//Build query string
				queryString = "SELECT SI_ID, SI_NAME FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' AND SI_NAME = '" + userName +"'";
				
				//Execute Query
				IInfoObjects boUserObjects = boInfoStore.query(queryString);
				
				if (boUserObjects.isEmpty())
				{
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
				}
				
			}
			
	
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		
		return;
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
}
