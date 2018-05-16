package filedownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileDownloader {

	static final String DIR = "./Downloads";
        private int nDownloads;
        volatile boolean descargados = false;
        volatile boolean[] petitions; //petitions[i] == true if the thread i has to download a new file
        volatile String[] actualFileUrl; //the url of the file that the thread i has to download
        volatile String[] actualFileName; //the name of the file that the thread i has to download
        volatile int actualThread;
        volatile boolean newPetition = false;
        volatile boolean petitionDone = false;
        
	public FileDownloader(int n) {
            this.nDownloads = n;
            this.petitions = new boolean[nDownloads];
            actualFileUrl = new String[nDownloads];
            actualFileName = new String[nDownloads];
            for(int i = 0; i < nDownloads; i++){
                petitions[i] = false;
            }
            
	}

    public int getnDownloads() {
        return nDownloads;
    }

    public void setnDownloads(int nDownloads) {
        this.nDownloads = nDownloads;
    }

    public void process(String downloadsFileURL) throws MalformedURLException, IOException {
        //download the first file
        actualThread = 0;
        actualFileUrl[0] = downloadsFileURL;
        actualFileName[0] = "output";
        newPetition = true;
        DownloadRunnable firstFile = new DownloadRunnable();
        firstFile.downloadUrls();

        //read the urls of the first file
        File file = new File(DIR+ "/output.txt");
        List<List<String>> files;
        if(file.exists()){
            files = readUrls(file);
        } else {
            System.out.println("incorrect url");
            return;
        }

        //Array of N threads and initialise the threads
        Thread[] threads = new Thread[nDownloads];
        for(int i = 0; i < nDownloads; i++){
            threads[i] = new Thread(new DownloadRunnable());
            System.out.println("Creado el hilo " + threads[i].getId());
            threads[i].start();
        }

        // reading the urls and downloading using the threads
        for(List<String> fileUrls : files){
            for(String url : fileUrls){
                boolean downloading = false;
                if(fileUrls.get(0) != url){ //in fileUrls.get(0) is saved the name of the file, so it isn't a url to download a file
                    //try to run one by one the threads until one thread is free and can start the download
                    while(!downloading){
                        //if the thread is free
                        if(petitions[actualThread] == false){
                            downloading = true;
                            System.out.println("Descargando "+ url + " por el hilo " + threads[actualThread].getId());
                            //updating the preconditions of the actualThread to start downloading
                            petitions[actualThread] = true;
                            actualFileUrl[actualThread] = url;
                            actualFileName[actualThread] = fileUrls.get(0)+".part" + (fileUrls.indexOf(url)-1);
                            newPetition = true;
                            //in this moment the actualThreads start to download the url

                        }
                        //if the actual thread is in the critical section, wait until the critical section is finished
                            while(newPetition){}
                            //going to the next thread
                            actualThread++;
                            actualThread = actualThread % nDownloads;

                    }
                }
            }
            //wait until the threads has finished
            for(int i = 0; i < nDownloads ; i++){
                while(petitions[i]);
            }

        }

        descargados = true;

        //esperamos a que acaben los threads
        for(int i = 0; i < nDownloads ; i++){
            while(threads[i].isAlive());
        }

        SplitAndMerge splitAndMerge = new SplitAndMerge();
        for(List<String> fileUrls : files){
            splitAndMerge.mergeFile(DIR+ "/", fileUrls.get(0));
        }
        for(List<String> fileUrls : files){
            for(String url : fileUrls){
                if(fileUrls.get(0) != url){
                    Path path =Paths.get(DIR+ "/" +  fileUrls.get(0)+".part" + fileUrls.indexOf(url)+".txt");
                    Files.deleteIfExists(path);
                }
            }
        }          
        Files.deleteIfExists(Paths.get(file.getPath()));
    }
        
    private List<List<String>> readUrls(File file) throws IOException{
        List<List<String>> fileUrls = new ArrayList<>();
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(file));
        String text = null;
        int i = -1;
        for(text = reader.readLine(); text != null; text = reader.readLine()){
            if(text.contains("Fichero: ")){
                i++;
                text = text.replace("Fichero: ","");
                List<String> url = new ArrayList<>();
                url.add(text);
                fileUrls.add(i, url);
            }
            else {
                fileUrls.get(i).add(text);
            }
        }
        return fileUrls;
    }
	
    public static void main(String[] args) throws IOException {
            String downloadFile = "https://github.com/jesussanchezoro/PracticaPC/raw/master/descargas.txt";
            FileDownloader fd = new FileDownloader(4);
            fd.process(downloadFile);
    }
        
    private class DownloadRunnable implements Runnable {
        
        public DownloadRunnable(){};
        
        @Override
        public void run() {
            //running the threads until all urls has been downloaded
            while(!descargados){
                try {
                        downloadUrls();
                    } catch (IOException ex) {
                    Logger.getLogger(FileDownloader.class.getName()).log(Level.SEVERE, null, ex);
            
                    }
            }
        }
        private void downloadUrls() throws MalformedURLException, IOException{
            //preprotocol
            while(!newPetition){}
            //critical section
                 int position = actualThread;
                 URL website = new URL(actualFileUrl[position]);
                 Path pathOut = Paths.get(DIR+ "/"+ actualFileName[position] + ".txt");
                 
            //postprotocol
            newPetition = false;
 
            //No critical section
            try (InputStream in = website.openStream()) {
                File folder = new File(DIR+ "/");
                folder.mkdirs(); // create the new folder
                Files.copy(in, pathOut, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println(actualFileName[position] +" Descargado");
            
            petitions[position] = false; //enabled the thread to process new downloads
           
            
        }
    }
  }