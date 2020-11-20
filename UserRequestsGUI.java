
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

class MyTask extends TimerTask{
	private UserRequestsGUI main;
	private String jobName;
	public MyTask(UserRequestsGUI this_instance, String jobName) {
		this.main = this_instance;
		this.jobName = jobName;
	}

	public void run() {
		JSONObject obj =  new JSONObject(UserRequestsGUI.Web("GET", "https://dataproc.googleapis.com/v1beta2/projects/" + main.projectId +"/regions/" + main.clusterRegion +"/jobs/" + main.jobId + "?key=" + main.apiKey, null, null, UserRequestsGUI.accessToken));
		if(obj.has("done")) {
			main.logPosition = obj.getString("driverOutputResourceUri");
			System.out.println(obj.getJSONObject("status").getString("state"));
			boolean success = obj.getJSONObject("status").getString("state").equals("DONE") ? true : false;
			main.endTime = obj.getJSONObject("status").getString("stateStartTime");
			JSONArray arr = obj.getJSONArray("statusHistory");
			for(int i=0;i<arr.length();i++) {
				if(arr.getJSONObject(i).getString("state").equals("PENDING")) {
					main.startTime = arr.getJSONObject(i).getString("stateStartTime");
					break;
				}
			}
			if(jobName.equals("invertDriver"))
				main.postSearchSuccess(success);
			else if(jobName.equals("TopN"))
				main.postNSearch(success);
			this.cancel();
		}
	}
}

public class UserRequestsGUI {
	public String projectId = "fiery-blade-295916";
	public String bucketName = "dataproc-staging-us-east1-656399245777-lxfd90ny";
	public String clusterRegion = "us-east1";
	public String clusterName = "cluster-6ecf";
	public static String accessToken;
	public String apiKey = "AIzaSyAuy7rurfmWzQEY2S59DGSNrnXiaMT42po";
	public String jobId; 
	public String logPosition;
	public String startTime;
	public String endTime;
	private String IItext;
	
	private JFrame frame;
	private JLabel lblLoading;
	private JButton btnLoadEngine;
	private JLabel lblElapsedTime;
	private File files[];
	private JTextField searchTextField;
	private JTable tableSearch = null;
	private JButton btnSearchForTerm;
	private JButton btnTopN;
	private JTextField topNTextField;
	private JTable tableTopN;
	private JTable tableTopN2;

	private JScrollPane scrollPane;
	private JLayeredPane layeredPane_3;
	private JLayeredPane layeredPane_4;
	private JLabel lblTopNFail;
	private JLabel labelTopNElapsedTime;
	private JButton btnGenerate;
	private int N;

	private UserRequestsGUI getThis() {
		return this;
	}
	public static void main(String[] args) {
		accessToken = System.getenv("ACCESS_TOKEN");
		System.out.println(accessToken);
		
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
	
	public static String Web(String type, String url, String contentType, HttpEntity entity, String accessToken) {
		try {
			HttpClient client = HttpClientBuilder.create().build();;
			HttpRequestBase request = type.equals("POST") ? new HttpPost(url) : (type.equals("DELETE")) ? new HttpDelete(url) : (type.equals("PATCH")) ? new HttpPatch(url) : new HttpGet(url);
			request.addHeader("Authorization", "Bearer " + accessToken);
			if(contentType != null)
				request.addHeader("Content-Type", contentType);
			
			if(type.equals("GET"))
				request.addHeader("Cache-Control","no-cache, max-age=0");
			
			if(entity != null) {
				if(type.equals("POST"))
					((HttpPost)request).setEntity(entity);
				else if(type.equals("PATCH"))
					((HttpPatch)request).setEntity(entity);
			}
				
		
			HttpResponse response = client.execute(request);
			if(type.equals("DELETE"))
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
	
	public void postSearchSuccess(boolean success) {
		
		lblLoading.setText(success ? "<html>Engine was loaded<br/>&<br/>Inverted indicies were constructed successfully!" : "Job Failed");
		for (File f: files) {
			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/input" + "%2F" + f.getName(), null, null, accessToken);
		}
		
	
		if(!success) return;
		
		IItext = Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "invertedOutput.txt" +"?alt=media", null, null, accessToken);
		btnSearchForTerm.setVisible(true);
		btnTopN.setVisible(true);
	}
	
	public void postNSearch(boolean success) {
		
		
		if(!success)
			lblTopNFail.setVisible(true);
		else{
			tableTopN = new JTable();
			tableTopN.setRowHeight(60);
			System.out.println("N: " + N);
			DefaultTableModel model = new DefaultTableModel();
			model.setColumnIdentifiers(new Object[] {"Term", "Total Frequencies"});
			//model.addRow(new Object[] {"Term", "Frequency"});
			
			String jsonBody = "{\"cacheControl\": \"no-cache, max-age=0\"}";
			try {
				Web("PATCH", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "TopN.txt", "application/json", new StringEntity(jsonBody), accessToken);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			String body = Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "TopN.txt" +"?alt=media", null, null, accessToken);
			Scanner scanner = new Scanner(body);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] arr = line.split("\t");
				if(arr.length == 2)
					model.addRow(new Object[] {arr[0], arr[1]});
			}
			scanner.close();
			tableTopN.setModel(model);
			scrollPane = new JScrollPane(tableTopN);
			scrollPane.setBounds(233, 96, 459, 240);
			layeredPane_4.add(scrollPane);
		}
		
		btnGenerate.setEnabled(true);
		layeredPane_3.setVisible(false);
		layeredPane_4.setVisible(true);
	}
	
	

	public UserRequestsGUI() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 951, 612);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane);
		
		JLayeredPane layeredPane_1 = new JLayeredPane();
		layeredPane_1.setBounds(0, 37, 935, 536);
		layeredPane_1.setVisible(false);
		frame.getContentPane().add(layeredPane_1);
		
		JLayeredPane layeredPane_2 = new JLayeredPane();
		layeredPane_2.setVisible(false);
		layeredPane_2.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_2);
		
		layeredPane_3 = new JLayeredPane();
		layeredPane_3.setVisible(false);
		layeredPane_3.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_3);
		
		layeredPane_4 = new JLayeredPane();
		layeredPane_4.setVisible(false);
		layeredPane_4.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_4);
		
		JLabel lblEnterN = new JLabel("Enter N:");
		lblEnterN.setBounds(233, 66, 459, 85);
		layeredPane_3.add(lblEnterN);
		lblEnterN.setHorizontalAlignment(SwingConstants.CENTER);
		
		topNTextField = new JTextField();
		topNTextField.setBounds(149, 158, 619, 48);
		layeredPane_3.add(topNTextField);
		topNTextField.setColumns(10);
		
		JButton buttonTopNGoBack = new JButton("Go Back");
		buttonTopNGoBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layeredPane_3.setVisible(false);
				layeredPane.setVisible(true);
			}
		});
		buttonTopNGoBack.setBounds(814, 0, 121, 48);
		layeredPane_3.add(buttonTopNGoBack);
		
		btnGenerate = new JButton("Search");
		btnGenerate.setBounds(233, 232, 459, 115);
		layeredPane_3.add(btnGenerate);
		
	
		
		JLabel lblN = new JLabel("N");
		lblN.setBounds(58, 51, 680, 48);
		layeredPane_4.add(lblN);
		
		lblTopNFail = new JLabel("Failure");
		lblTopNFail.setVisible(false);
		lblTopNFail.setHorizontalAlignment(SwingConstants.CENTER);
		lblTopNFail.setBounds(233, 162, 459, 120);
		layeredPane_4.add(lblTopNFail);
		
		labelTopNElapsedTime = new JLabel("Top N Elapsed Time");
		labelTopNElapsedTime.setVisible(false);
		labelTopNElapsedTime.setBounds(58, 51, 680, 48);
		layeredPane_4.add(labelTopNElapsedTime);
		
		JButton buttonGoBackToTopN = new JButton("Go Back To Top N");
		buttonGoBackToTopN.setBounds(738, 0, 197, 48);
		layeredPane_4.add(buttonGoBackToTopN);
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
			
		JLabel lblFileToBe = new JLabel("");
		lblFileToBe.setBounds(0, 169, 935, 217);
		layeredPane.add(lblFileToBe);
		lblFileToBe.setVerticalAlignment(SwingConstants.TOP);
		lblFileToBe.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton btnChooseFile = new JButton("Choose File");
		
		btnLoadEngine = new JButton("Construct Inverted Indicies and Load Engine");

		btnLoadEngine.setVisible(false);
		btnLoadEngine.setBounds(233, 393, 459, 85);
		layeredPane.add(btnLoadEngine);
		
		btnChooseFile.setBounds(233, 66, 459, 85);
		layeredPane.add(btnChooseFile);
		
		lblLoading = new JLabel("Loading engine and constructing inverted indicies...");
		lblLoading.setBounds(10, 162, 937, 210);
		lblLoading.setVisible(false);
		layeredPane.add(lblLoading);
		lblLoading.setHorizontalAlignment(SwingConstants.CENTER);
		
	
		lblElapsedTime = new JLabel("");
		lblElapsedTime.setBounds(12, 0, 136, 48);
		layeredPane.add(lblElapsedTime);
		
		btnSearchForTerm = new JButton("Search For Term");
		btnSearchForTerm.setVisible(false);
		btnSearchForTerm.setBounds(0, 393, 459, 85);
		layeredPane.add(btnSearchForTerm);
		
		
		btnTopN = new JButton("Top N");
		btnTopN.setVisible(false);
		btnTopN.setBounds(476, 393, 459, 85);
		layeredPane.add(btnTopN);
			
		
		JButton btnSearchBack = new JButton("Go Back");
		btnSearchBack.setBounds(814, 0, 121, 48);
		layeredPane_1.add(btnSearchBack);
		
		JLabel lblEnterSearchTerm = new JLabel("Enter Your Search Term: ");
		lblEnterSearchTerm.setBounds(233, 66, 459, 85);
		layeredPane_1.add(lblEnterSearchTerm);
		lblEnterSearchTerm.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton btnSearch = new JButton("Search");	
		searchTextField = new JTextField();
		searchTextField.setBounds(149, 158, 619, 48);
		layeredPane_1.add(searchTextField);
		searchTextField.setColumns(10);
		btnSearch.setBounds(233, 232, 459, 115);
		layeredPane_1.add(btnSearch);
		

		
		JButton btnGoBackToSearch = new JButton("Go Back To Search");
		btnGoBackToSearch.setBounds(738, 0, 197, 48);
		layeredPane_2.add(btnGoBackToSearch);
		
		JLabel lblSearchedTerm = new JLabel("You searched for the term: ");
		lblSearchedTerm.setBounds(58, 0, 680, 48);
		layeredPane_2.add(lblSearchedTerm);
		
		JLabel lblSearchElapsedTime = new JLabel("");
		lblSearchElapsedTime.setBounds(58, 51, 680, 48);
		layeredPane_2.add(lblSearchElapsedTime);
		
		
		JLabel lblTermNotExist = new JLabel("Term Not Found");
		lblTermNotExist.setVisible(false);
		lblTermNotExist.setBounds(233, 162, 459, 120);
		layeredPane_2.add(lblTermNotExist);
		lblTermNotExist.setHorizontalAlignment(SwingConstants.CENTER);
	
		
		tableSearch = new JTable();
		tableSearch.setBounds(233, 120, 459, 240);
		tableSearch.setRowHeight(60);
		layeredPane_2.add(tableSearch);
		

		JLabel lblNewLabel = new JLabel("Joseph Weiss Search Engine");
		lblNewLabel.setBounds(5, 5, 935, 25);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		frame.getContentPane().add(lblNewLabel);
		
				
		btnChooseFile.addActionListener(new ActionListener() {
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
					btnLoadEngine.setVisible(true);
				}
					
			}
		});
		
		btnLoadEngine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lblFileToBe.setVisible(false);
				btnChooseFile.setVisible(false);
				lblLoading.setVisible(true);
				btnLoadEngine.setVisible(false);
				
				for (File f: files) {
					String obj = Web("POST", "https://storage.googleapis.com/upload/storage/v1/b/" + bucketName + "/o??uploadType=media&name=input/" + f.getName(), "application/octet-stream", new FileEntity(f), accessToken);
					if(obj == null) {
						lblLoading.setText("Failure");
						return;
					}
				}
				//lblLoading.setText("<html>Engine was loaded<br/>&<br/>Inverted indicies were constructed successfully!</html>");	
				
				String jsonBody = "{\"projectId\": \"" + projectId + "\"," +"\"job\": {\"placement\": {\"clusterName\": \"" + clusterName + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + bucketName +"/Jar_Files/invert.jar\"],\"args\": [\"gs://" + bucketName + "/input\",\"gs://" + bucketName + "/IIOutput\"],\"mainClass\": \"invertDriver\"}}}";
				try {
					JSONObject obj = new JSONObject(Web("POST", "https://dataproc.googleapis.com/v1/projects/" + projectId +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + apiKey, "application/json", new StringEntity(jsonBody), accessToken));
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTask task = new MyTask(getThis(), "invertDriver");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
	
		btnSearch.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				btnSearch.setText("Making search...");
				
				long start = System.currentTimeMillis();
			      
				String term = searchTextField.getText();
				List<String> docList = new ArrayList<String>(files.length);
				List<String> frequencyList = new ArrayList<String>(files.length);
				
				Scanner scanner = new Scanner(IItext);
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
					tableSearch.setModel(new DefaultTableModel());
					lblTermNotExist.setVisible(true);
				}
				else {						
					tableTopN2 = new JTable();
					tableTopN2.setRowHeight(60);
					DefaultTableModel model = new DefaultTableModel();
				    model.setColumnIdentifiers(new Object[] {"Doc Name", "Frequencies"});
				    //model.addRow(new Object[] {"Doc Name", "Frequencies"});
				    for(int n=0;n<docList.size();n++) 
				    	model.addRow(new Object[] {docList.get(n), frequencyList.get(n)});
				
				    tableTopN2.setModel(model);
					scrollPane = new JScrollPane(tableTopN2);
					scrollPane.setBounds(233, 96, 459, 240);
					layeredPane_2.add(scrollPane);
				}
				layeredPane_1.setVisible(false);
				layeredPane_2.setVisible(true);	
			}
		});
		
		btnGenerate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!topNTextField.getText().matches("-?\\d+")) {
					topNTextField.setText("Failure");
					return;
				}
				N = Integer.parseInt(topNTextField.getText());
				if(N <= 0) {
					topNTextField.setText("Failure");
					return;
				}
				lblN.setText("N: " + N);
				
				btnGenerate.setText("Making search...");
				btnGenerate.setEnabled(false);
				String jsonBody = "{\"projectId\": \"" + projectId + "\"," +"\"job\": {\"placement\": {\"clusterName\": \"" + clusterName + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + bucketName +"/Jar_Files/top-n.jar\"],\"args\": [\"gs://" + bucketName + "/IIOutput\",\"gs://" + bucketName + "/TopNOutput\",\"" + N + "\"],\"mainClass\": \"TopN\"}}}";
				try {
					JSONObject obj = new JSONObject(Web("POST", "https://dataproc.googleapis.com/v1/projects/" + projectId +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + apiKey, "application/json", new StringEntity(jsonBody), accessToken));
				
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException exc) {
					exc.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTask task = new MyTask(getThis(), "TopN");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
		btnGoBackToSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSearch.setText("Search");
				lblTermNotExist.setVisible(false);
				layeredPane_2.setVisible(false);
				layeredPane_1.setVisible(true);
			}
		});
		
		
		buttonGoBackToTopN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopN.txt", null, null, accessToken);
		    	JSONObject obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=TopNOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		JSONArray arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopNOutput" + "%2f", null, null, accessToken);
		    	}
				btnGenerate.setText("Search");
				layeredPane_4.remove(scrollPane);
				lblTopNFail.setVisible(false);
				layeredPane_4.setVisible(false);
				layeredPane_3.setVisible(true);
			}
		});
		
		btnTopN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				layeredPane.setVisible(false);
				layeredPane_3.setVisible(true);
			}
		});
		
		btnSearchForTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				layeredPane.setVisible(false);
				layeredPane_1.setVisible(true);
			}
		});
		
		btnSearchBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layeredPane_1.setVisible(false);
				layeredPane.setVisible(true);
			}
		});

	
		
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/invertedOutput.txt", null, null, accessToken);
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopN.txt", null, null, accessToken);
		    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/SearchTerm.txt", null, null, accessToken);
		    	
		    	
		    	JSONObject obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=IIOutput", null, null, accessToken));
		    	JSONArray arr;
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("IIOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/IIOutput" + "%2f", null, null, accessToken);
			    	
		    	}
		    	
		    	obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=TopNOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopNOutput" + "%2f", null, null, accessToken);
		    	}
		    	
		    	
		    	obj= new JSONObject(Web("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=SearchTermOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("SearchTermOutput/")) 
			    			Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	Web("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/SearchTermOutput" + "%2f", null, null, accessToken);
		    	}
		    	
		    }
		});
		
	}
}