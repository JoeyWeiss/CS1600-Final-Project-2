import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

class MyTaskTest extends TimerTask{
	private UserRequestsGUI main;
	private String jobName;
	public MyTaskTest(UserRequestsGUI this_instance, String jobName) {
		this.main = this_instance;
		this.jobName = jobName;
	}

	public void run() {
		JSONObject obj =  new JSONObject(UserRequestsGUI.Web("GET", "https://dataproc.googleapis.com/v1beta2/projects/" + main.mainID +"/regions/" + main.clusterRegion +"/jobs/" + main.jobId + "?key=" + main.myKey, null, null, UserRequestsGUI.token));
		if(obj.has("done")) {
			main.logPosition = obj.getString("driverOutputResourceUri");
			System.out.println(obj.getJSONObject("status").getString("state"));
			boolean check = obj.getJSONObject("status").getString("state").equals("DONE") ? true : false;
			main.end = obj.getJSONObject("status").getString("statestart");
			JSONArray arr = obj.getJSONArray("statusHistory");
			for(int i=0;i<arr.length();i++) {
				if(arr.getJSONObject(i).getString("state").equals("PENDING")) {
					main.start = arr.getJSONObject(i).getString("statestart");
					break;
				}
			}
			if(jobName.equals("invertDriver"))
				main.postSearchSuccess(check);
			else if(jobName.equals("TopN"))
				main.postNSearch(check);
			this.cancel();
		}
	}
}

public class UserRequestsGUI {
	public String jobId; 
	public String logPosition;
	public String start;
	public String end;
	public static String token;
	public String mainID = "fiery-blade-295916";
	public String dataprocID = "dataproc-staging-us-east1-656399245777-lxfd90ny";
	public String clusterRegion = "us-east1";
	public String clusterID = "cluster-6ecf";
	public String myKey = "AIzaSyAuy7rurfmWzQEY2S59DGSNrnXiaMT42po";
	
	
	private UserRequestsGUI getThis() {
		return this;
	}
	
	
	private JFrame frame;

	public static void main(String[] args) {
		token = System.getenv("ACCESS_TOKEN");
		System.out.println(token);
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UserRequestsGUI window = new UserRequestsGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static String Web(String input, String link, String contType, HttpEntity entity, String token) {
		try {
			HttpClient client = HttpClientBuilder.create().build();;
			HttpRequestBase request = input.equals("POST") ? new HttpPost(link) : (input.equals("DELETE")) ? new HttpDelete(link) : (input.equals("PATCH")) ? new HttpPatch(link) : new HttpGet(link);
			request.addHeader("Authorization", "Bearer " + token);
			if(contType != null)
				request.addHeader("Content-Type", contType);
			
			if(input.equals("GET"))
				request.addHeader("Cache-Control","no-cache, max-age=0");
			
			if(entity != null) {
				if(input.equals("POST"))
					((HttpPost)request).setEntity(entity);
				else if(input.equals("PATCH"))
					((HttpPatch)request).setEntity(entity);
			}
				
		
			HttpResponse response = client.execute(request);
			if(input.equals("DELETE"))
				return null;
			InputStream in = response.getEntity().getContent();
			String body = IOUtils.toString(in, "UTF-8");
			System.out.println(body);
			return body;
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}				
	}
	private JLabel loadLabel;
	private String testText;
	private JLayeredPane level3;
	private JLayeredPane level4;
	private File files[];
	private JTextField searchField;
	private JTable searchTable = null;
	private JButton searchWord;
	private JButton nButton;
	private JTextField nField;
	private JTable tableTopN;

	public void postSearchSuccess(boolean check) {
		
		loadLabel.setText(check ? "<html>Engine was loaded<br/>&<br/>Inverted indicies were constructed successfully!" : "Job Failed");
		for (File f: files) {
			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/input" + "%2F" + f.getName(), null, null, token);
		}
		
	
		if(!check) return;
		
		testText = Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + "invertedOutput.txt" +"?alt=media", null, null, token);
		searchWord.setVisible(true);
		nButton.setVisible(true);
	}
	private JScrollPane roll;
	private JLabel lblTopmakeSearchButton;
	private JLabel labelTopNElapsedTime;
	private JButton makeSearchButton;
	private int N;
	public void postNSearch(boolean check) {
		
		
		if(!check)
			lblTopmakeSearchButton.setVisible(true);
		else{
			tableTopN = new JTable();
			tableTopN.setRowHeight(60);
			System.out.println("N: " + N);
			DefaultTableModel model = new DefaultTableModel();
			model.setColumnIdentifiers(new Object[] {"Term", "Total Frequencies"});
			
			String jsonText = "{\"cacheControl\": \"no-cache, max-age=0\"}";
			try {
				Web("PATCH", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + "TopNOutput.txt", "application/json", new StringEntity(jsonText), token);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			String body = Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + "TopNOutput.txt" +"?alt=media", null, null, token);
			Scanner scanner = new Scanner(body);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] arr = line.split("\t");
				if(arr.length == 2)
					model.addRow(new Object[] {arr[0], arr[1]});
			}
			scanner.close();
			tableTopN.setModel(model);
			roll = new JScrollPane(tableTopN);
			roll.setBounds(233, 96, 459, 240);
			level4.add(roll);
		}
		
		makeSearchButton.setEnabled(true);
		level3.setVisible(false);
		level4.setVisible(true);
	}
	
	

	public UserRequestsGUI() {
		makeGUI();
	}
	private JButton loadEngine;
	private JLabel timePassed;
	private void makeGUI() {
		frame = new JFrame();
		frame.setBounds(100, 100, 951, 612);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLayeredPane level = new JLayeredPane();
		level.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(level);
		
		JLayeredPane level1 = new JLayeredPane();
		level1.setBounds(0, 37, 935, 536);
		level1.setVisible(false);
		frame.getContentPane().add(level1);
		
		JLayeredPane level2 = new JLayeredPane();
		level2.setVisible(false);
		level2.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(level2);
		
		level3 = new JLayeredPane();
		level3.setVisible(false);
		level3.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(level3);
		
		level4 = new JLayeredPane();
		level4.setVisible(false);
		level4.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(level4);
		
		JLabel lblEnterN = new JLabel("Enter N:");
		lblEnterN.setBounds(233, 66, 459, 85);
		level3.add(lblEnterN);
		lblEnterN.setHorizontalAlignment(SwingConstants.CENTER);
		
		nField = new JTextField();
		nField.setBounds(149, 158, 619, 48);
		level3.add(nField);
		nField.setColumns(10);
		
		JButton nGoBackButton = new JButton("Go Back");
		nGoBackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				level3.setVisible(false);
				level.setVisible(true);
			}
		});
		nGoBackButton.setBounds(814, 0, 121, 48);
		level3.add(nGoBackButton);
		
		makeSearchButton = new JButton("Search");
		makeSearchButton.setBounds(233, 232, 459, 115);
		level3.add(makeSearchButton);
		
	
		
		JLabel nLabel = new JLabel("N");
		nLabel.setBounds(58, 51, 680, 48);
		level4.add(nLabel);
		
		lblTopmakeSearchButton = new JLabel("Failure");
		lblTopmakeSearchButton.setVisible(false);
		lblTopmakeSearchButton.setHorizontalAlignment(SwingConstants.CENTER);
		lblTopmakeSearchButton.setBounds(233, 162, 459, 120);
		level4.add(lblTopmakeSearchButton);
		
		labelTopNElapsedTime = new JLabel("Top N Elapsed Time");
		labelTopNElapsedTime.setVisible(false);
		labelTopNElapsedTime.setBounds(58, 51, 680, 48);
		level4.add(labelTopNElapsedTime);
		
		JButton nGoBack = new JButton("Go Back To Top N");
		nGoBack.setBounds(738, 0, 197, 48);
		level4.add(nGoBack);
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
			
		JLabel lblFileToBe = new JLabel("");
		lblFileToBe.setBounds(0, 169, 935, 217);
		level.add(lblFileToBe);
		lblFileToBe.setVerticalAlignment(SwingConstants.TOP);
		lblFileToBe.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton pickFile = new JButton("Choose File");
		
		loadEngine = new JButton("Construct Inverted Indicies and Load Engine");

		loadEngine.setVisible(false);
		loadEngine.setBounds(233, 393, 459, 85);
		level.add(loadEngine);
		
		pickFile.setBounds(233, 66, 459, 85);
		level.add(pickFile);
		
		loadLabel = new JLabel("Loading engine and constructing inverted indicies...");
		loadLabel.setBounds(10, 162, 937, 210);
		loadLabel.setVisible(false);
		level.add(loadLabel);
		loadLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
	
		timePassed = new JLabel("");
		timePassed.setBounds(12, 0, 136, 48);
		level.add(timePassed);
		
		searchWord = new JButton("Search For Term");
		searchWord.setVisible(false);
		searchWord.setBounds(0, 393, 459, 85);
		level.add(searchWord);
		
		
		nButton = new JButton("Top N");
		nButton.setVisible(false);
		nButton.setBounds(476, 393, 459, 85);
		level.add(nButton);
			
		
		JButton searchBack = new JButton("Go Back");
		searchBack.setBounds(814, 0, 121, 48);
		level1.add(searchBack);
		
		JLabel lblEnterSearchTerm = new JLabel("Enter Your Search Term: ");
		lblEnterSearchTerm.setBounds(233, 66, 459, 85);
		level1.add(lblEnterSearchTerm);
		lblEnterSearchTerm.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton searchButton1 = new JButton("Search");	
		searchField = new JTextField();
		searchField.setBounds(149, 158, 619, 48);
		level1.add(searchField);
		searchField.setColumns(10);
		searchButton1.setBounds(233, 232, 459, 115);
		level1.add(searchButton1);
		
		JButton searchGoBack = new JButton("Go Back To Search");
		searchGoBack.setBounds(738, 0, 197, 48);
		level2.add(searchGoBack);
		
		JLabel lblSearchedTerm = new JLabel("You searched for the term: ");
		lblSearchedTerm.setBounds(58, 0, 680, 48);
		level2.add(lblSearchedTerm);
		
		JLabel lblSearchElapsedTime = new JLabel("");
		lblSearchElapsedTime.setBounds(58, 51, 680, 48);
		level2.add(lblSearchElapsedTime);
		
		
		JLabel lblTermNotExist = new JLabel("Term Not Found");
		lblTermNotExist.setVisible(false);
		lblTermNotExist.setBounds(233, 162, 459, 120);
		level2.add(lblTermNotExist);
		lblTermNotExist.setHorizontalAlignment(SwingConstants.CENTER);
	
		
		searchTable = new JTable();
		searchTable.setBounds(233, 120, 459, 240);
		searchTable.setRowHeight(60);
		level2.add(searchTable);
		

		JLabel nLabelewLabel = new JLabel("Joseph Weiss Search Engine");
		nLabelewLabel.setBounds(5, 5, 935, 25);
		nLabelewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		frame.getContentPane().add(nLabelewLabel);
		
				
		pickFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int res = fileChooser.showSaveDialog(null);
				if (res == JFileChooser.APPROVE_OPTION) {
					String text = "";
					
					files = fileChooser.getSelectedFiles();
					text = "<html><div style='text-align: center;'>";
					for (int n = 0; n < files.length; n++)
						text += files[n] + "<br/>";
					text += "</div></html>";
					
					lblFileToBe.setText(text);
					loadEngine.setVisible(true);
				}
					
			}
		});
		
		loadEngine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lblFileToBe.setVisible(false);
				pickFile.setVisible(false);
				loadLabel.setVisible(true);
				loadEngine.setVisible(false);
				
				for (File f: files) {
					String obj = Web("POST", "https://storage.googleapis.com/upload/storage/v1/b/" + dataprocID + "/o??uploadType=media&name=input/" + f.getName(), "application/octet-stream", new FileEntity(f), token);
					if(obj == null) {
						loadLabel.setText("Failure");
						return;
					}
				}
				
				String jsonText = "{\"mainID\": \"" + mainID + "\"," +"\"job\": {\"placement\": {\"clusterID\": \"" + clusterID + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + dataprocID +"/Jar_Files/invert.jar\"],\"args\": [\"gs://" + dataprocID + "/input\",\"gs://" + dataprocID + "/Output\"],\"mainClass\": \"invertDriver\"}}}";
				try {
					JSONObject obj = new JSONObject(Web("POST", "https://dataproc.googleapis.com/v1/projects/" + mainID +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + myKey, "application/json", new StringEntity(jsonText), token));
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTaskTest task = new MyTaskTest(getThis(), "invertDriver");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
	
		searchButton1.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				searchButton1.setText("Making search...");
				
				long start = System.currentTimeMillis();
			      
				String term = searchField.getText();
				List<String> docList = new ArrayList<String>(files.length);
				List<String> frequencyList = new ArrayList<String>(files.length);
				
				Scanner scanner = new Scanner(testText);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String[] arr = line.split("\t");
					if(term.equals(arr[0])) {
						System.out.println("Term found");
						for(int n=0;n < files.length; n++) {
							if(!arr[0].equals(term)) 
								break;
							
							docList.add(arr[1]);
							frequencyList.add(arr[2]);
							
							if(!scanner.hasNextLine()) 
								break;
							
							line = scanner.nextLine();
							arr = line.split("\t");
						}
						break;
					}
				}
				scanner.close();
				
				long end = System.currentTimeMillis();
				
				lblSearchedTerm.setText("You searched for the term: " + term);
				lblSearchElapsedTime.setText("Your search was executed in " + Float.toString((end - start) / 1000F) +" ms");
				
				if(docList.size() == 0) {
					searchTable.setModel(new DefaultTableModel());
					lblTermNotExist.setVisible(true);
				}
				else {						
				
					DefaultTableModel model = new DefaultTableModel();
				    model.setColumnIdentifiers(new Object[] {"Doc Name", "Frequencies"});
				    model.addRow(new Object[] {"Doc Name", "Frequencies"});
				    for(int n=0;n<docList.size();n++) 
				    	model.addRow(new Object[] {docList.get(n), frequencyList.get(n)});
				
				}
				level1.setVisible(false);
				level2.setVisible(true);	

			}
		});
		
		makeSearchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!nField.getText().matches("-?\\d+")) {
					nField.setText("Failure");
					return;
				}
				N = Integer.parseInt(nField.getText());
				if(N <= 0) {
					nField.setText("Failure");
					return;
				}
				nLabel.setText("Top " + N + " Frequent Terms");
				
				makeSearchButton.setText("Making search...");
				makeSearchButton.setEnabled(false);
				String jsonText = "{\"mainID\": \"" + mainID + "\"," +"\"job\": {\"placement\": {\"clusterID\": \"" + clusterID + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + dataprocID +"/Jar_Files/top-n.jar\"],\"args\": [\"gs://" + dataprocID + "/Output\",\"gs://" + dataprocID + "/TopNOutput\",\"" + N + "\"],\"mainClass\": \"TopN\"}}}";
				try {
					JSONObject obj = new JSONObject(Web("POST", "https://dataproc.googleapis.com/v1/projects/" + mainID +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + myKey, "application/json", new StringEntity(jsonText), token));
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException exc) {
					exc.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTaskTest task = new MyTaskTest(getThis(), "TopN");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
		searchGoBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchButton1.setText("Search");
				lblTermNotExist.setVisible(false);
				level2.setVisible(false);
				level1.setVisible(true);
			}
		});
		
		
		nGoBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/TopNOutput.txt", null, null, token);
		    	JSONObject obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o" + "?prefix=TopNOutput", null, null, token));
		    	if(obj.has("items")) {
		    		JSONArray arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + fName.replace("/", "%2F"), null, null, token);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/TopNOutput" + "%2f", null, null, token);
		    	}
				makeSearchButton.setText("Search");
				level4.remove(roll);
				lblTopmakeSearchButton.setVisible(false);
				level4.setVisible(false);
				level3.setVisible(true);
			}
		});
		
		nButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				level.setVisible(false);
				level3.setVisible(true);
			}
		});
		
		searchWord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				level.setVisible(false);
				level1.setVisible(true);
			}
		});
		
		searchBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				level1.setVisible(false);
				level.setVisible(true);
			}
		});

	
		
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/invertedOutput.txt", null, null, token);
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/TopNOutput.txt", null, null, token);
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/SearchTerm.txt", null, null, token);
		    	
		    	
		    	JSONObject obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o" + "?prefix=Output", null, null, token));
		    	JSONArray arr;
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("Output/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + fName.replace("/", "%2F"), null, null, token);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/Output" + "%2f", null, null, token);
			    	
		    	}
		    	
		    	obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o" + "?prefix=TopNOutput", null, null, token));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + fName.replace("/", "%2F"), null, null, token);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/TopNOutput" + "%2f", null, null, token);
		    	}
		    	
		    	
		    	obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o" + "?prefix=SearchTermOutput", null, null, token));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("SearchTermOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/" + fName.replace("/", "%2F"), null, null, token);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + dataprocID + "/o/SearchTermOutput" + "%2f", null, null, token);
		    	}
		    	
		    }
		});
		
	}
}