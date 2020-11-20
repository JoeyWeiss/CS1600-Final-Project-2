Steps for Running Application
-----------------------------

Note: I am using Windows 10

Step 1: Download project files. Make sure input files are located somewhere inside the project folder. 

Step 2: Have Docker installed. Have Xming downloaded to enable GUI support as we did in the Docker Homework earlier this semester (https://docs.microsoft.com/en-us/archive/blogs/jamiedalton/windows-10-docker-gui). Launch Xming. Launch Docker. In a command line interface (I use PowerShell), locate and enter project file. 

Step 3: Go to https://developers.google.com/oauthplayground/, click on the settings button in the top right corner and click the "Use your own OAuth credentials" checkbox. Insert the following information:

  OAuth Client ID: 656399245777-pt0lp4baqsi7pmas20j640fm3g55jvn0.apps.googleusercontent.com
  OAuth Client secret: IGmZcsSgrRmLIhGO0o8mnyer

Then scroll down in the box below "Step 1 Select & authorize APIs" on the left until you find "Cloud Dataproc API v1". Click on it, select "https://www.googleapis.com/auth/cloud-platform" so I has a checkbox to the left of it, and then click the "Authorize API's" button. Select your email account. If you get a page which says "This app isn't verified" then click "Advanced" and click "Go to final project (unsafe)" and then click "Allow". If you do not see that warning page, just click "Allow" to allow the site to "View and manage your data across Google Cloud Platform services". Under "Step 2 Exchange authorization code for tokens" click the "Exchange authorization code for tokens" button. Then copy the access token. It will give you a span of time to use this token. If you run out of time, click the "Refresh access token button" to get a new access token. Save your access code for step 4.

Step 4: In PowerShell (or your command line interface of choice) enter the following commands. Where it says <access key>, insert the access key you copied at the end of step 3.

Commands:

 Build Image:
 
    docker build --tag <name> .                          

  Run Image:
  
    docker run --privileged --env DISPLAY=<insert your IP address>:0.0 --env ACCESS_TOKEN=<access token> <name>


For example, on my computer I might run the following two commands:
    
    docker build --tag gui .                          

    docker run --privileged --env DISPLAY=192.168.0.40:0.0 --env ACCESS_TOKEN=ya29.a0AfH6SMCDoBtJEyxfZlp4J8VAnZNmngYB97-fkHMuF8rloYTdCIl1Itq3tVJlVoqaR_M1_MeepOpcOsh5preJNke9vkGAQ3rM6gXMlLpmRs1hA0lMjKmhuGjE8PP3R1Y0sx7ehWl6cpfNGMQ2EM1Rzpjy1PqqeRWfZZO4hkCDyCI gui

Step 5: When he program launches, click the "Choose Files" button. Then navigate to "/usr/src/app/input" and select as many .tar.gz files as you want. Then click "Construct Inverted Indicies and Load Engine". The program will function as the Mockup.pdf file you gave us works. Click "Search for Term" to look for thhe frequency of particular words or click "Top-N" to find the n most frequent words. There is a back button in the top right corner of every sub page to bring you back.






My Info:

  OAuth 2.0 Client ID (NEEDED TO CREATE ACCESS TOKEN): 
  
    656399245777-pt0lp4baqsi7pmas20j640fm3g55jvn0.apps.googleusercontent.com
  
  Client Secret (NEEDED TO CREATE ACCESS TOKEN):
  
     IGmZcsSgrRmLIhGO0o8mnyer

  API Key: 
  
    AIzaSyAuy7rurfmWzQEY2S59DGSNrnXiaMT42po



References:
  General help with setting up Inverted Index on GCP
    http://www-scf.usc.edu/~shin630/Youngmin/files/HadoopInvertedIndexV5.pdf
  In schedule tasks, so I implement TimerTask
    https://springframework.guru/java-timer/
  Inverted Index guides
    https://timepasstechies.com/map-reduce-inverted-index-sample/
    https://acadgild.com/blog/building-inverted-index-mapreduce#:~:text=Inverted%20index%20is%20index%20data,or%20a%20set%20of%20documents.
  NaturalKeyPartitioner Reference
    https://www.javatips.net/API/clickstream-tutorial-master/03_processing/02_sessionization/mr/src/main/java/com/hadooparchitecturebook/NaturalKeyPartitioner.java
  My references to the GCP, inclusing use of oauthplayground, and GUI orgainzation using layers
    https://github.com/catsae/searchGUI
  TopN reference
    https://www.geeksforgeeks.org/how-to-find-top-n-records-using-mapreduce/
  Site for making access token
    https://developers.google.com/oauthplayground/
