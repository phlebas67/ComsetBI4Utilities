package comset.boe4utilities;

import java.io.FileReader;
import java.util.List;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.program.IProgramBase;
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
		String csvFile = null;
		String csvSeparator = null;
		
		// Logon Information
		// To pass the arguments to the main method in the Eclipse IDE, set the values in the configuration. 
		// (Right Click the .java file > Run As > Run configuration > Arguments tab) 
		
		userName = args[0];
		password = args[1];
		cmsName = args[2];
		authType = args[3];
		csvFile = args[4];
		csvSeparator = args[5];
		
		// Enforce all expected cmd line arguments have been submitted. 
		// **Remember this is a sample, change values as needed.
		if ((args.length >= 6 ) && args[0] != null)   
		{
/*			try 
			{
				// Initialize the Session Manager 
				//boSessionMgr = CrystalEnterprise.getSessionMgr();
	
				// Logon to the Session Manager to create a new BOE session.
				//boEnterpriseSession = boSessionMgr.logon(userName, password, cmsName, authType);
				//System.out.println("user \"" + userName + "\" logged in via main() method");
				
				//Retrieve the InfoStore object
				//boInfoStore = (IInfoStore) boEnterpriseSession.getService("", "InfoStore");
			}
			catch (SDKException sdke)
			{
				System.out.println(sdke.getMessage());
				System.exit(1);
			}*/
			//Read CSV file
			readCSVFile(csvFile, csvSeparator);

			//call the run() method
			//UploadAgnosticFile uaf = new UploadAgnosticFile();
			//uaf.run(boEnterpriseSession, boInfoStore, args);
		
		}  //end of if statement

	} //end of main method

	public void run(IEnterpriseSession boEnterpriseSession, IInfoStore boInfoStore, java.lang.String[] args) {
	}
	
	public static void readCSVFile(String file,String separator)
	{
		System.out.println("File to read: " + file);
		System.out.println("CSV Separator: " + separator);
		
		try {
	        // Create an object of file reader class with CSV file as a parameter. 
	        FileReader filereader = new FileReader(file);
	        
	        // create csvParser object with custom separator
	        char delim = separator.charAt(0);
	        CSVReader csvReader = new CSVReader(filereader,delim);
	        
	        //Read the file into a list
	        List<String[]> allData = csvReader.readAll();
	        
	        // Print Data.
	        System.out.println(file+" has the following content:");
	        for (String[] row : allData) { 
	            for (String cell : row) { 
	                System.out.print(cell + "\t"); 
	            } 
	            System.out.println(); 
	        } 
	        
	        //Close the readers
	        filereader.close();
	        csvReader.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
}
