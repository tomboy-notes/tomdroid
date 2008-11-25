package org.tomdroid.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class NotesDAOImpl implements NotesDAO {
	//TODO implement runnable and launch a thread from UI to fetch Notes
	
	private String noteURL;
	private String noteContent;
	
	public NotesDAOImpl (String url) {
		this.noteURL = url;
	}
	
	/* (non-Javadoc)
	 * @see org.tomdroid.dao.NotesDAO#getContent()
	 */
	public String getContent() {
		
		if (noteContent != null) {
			return noteContent;
		} else {
			try {
				noteContent = fetch(noteURL);
				return noteContent;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	
	/**
	 * Grab the content at the target address and convert it to a string.
	 * @param address
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String fetch(String address) throws MalformedURLException, IOException {
		
		//grab URL
		URL url = new URL(address);
		InputStream is = (InputStream) url.getContent();
		
		//Init BufferedReader and StringBuilder
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		//Convert from InputStream to String using StringBuilder
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();

		//Return the string
		return sb.toString();
	}
}
