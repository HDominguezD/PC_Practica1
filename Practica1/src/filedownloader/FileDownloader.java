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
        volatile boolean[] petitions = null;
        volatile String[] actualFileUrl = null;
        volatile String[] actualFileName = null;
        volatile int actualThread = 0;
        volatile boolean newPetition = false;
        
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
            actualFileUrl[0] = downloadsFileURL;
            actualFileName[0] = "output";
            newPetition = true;
            DownloadRunnable firstFile = new DownloadRunnable();
            firstFile.downloadUrls();
            
            File file = new File(DIR+ "/output.txt");
            //downloads files
            List<List<String>> files;
            if(file.exists()){
                files = readUrls(file);
            } else {
                System.out.println("incorrect url");
                return;
            }
            
            //Array of N threads
            Thread[] threads = new Thread[nDownloads];
            for(int i = 0; i < nDownloads; i++){
                threads[i] = new Thread(new DownloadRunnable());
                System.out.println("Creado el hilo " + threads[i].getId());
                threads[i].start();
            }

            for(List<String> fileUrls : files){
                for(String url : fileUrls){
                    boolean downloading = false;
                    if(fileUrls.get(0) != url){
                        //descargamos el archivo
                        while(!downloading){
                            //si el thread no esta ocupado
                            if(petitions[actualThread] == false){
                                downloading = true;
                                System.out.println("Descargando "+ url + " por el hilo " + threads[actualThread].getId());
                                //updating a new petition
                                petitions[actualThread] = true;
                                actualFileUrl[actualThread] = url;
                                actualFileName[actualThread] = fileUrls.get(0)+".part" + (fileUrls.indexOf(url)-1);
                                newPetition = true;
                             
                            }
                            //waiting while the critical section of the downloading is been done
                                while(newPetition){}
                                //going to the next thread
                                actualThread++;
                                actualThread = actualThread % nDownloads;
                                
                        }
                    }
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
                 
            //postprotocol
            newPetition = false;
 
            //No critical section
            URL website = new URL(actualFileUrl[position]);
            Path pathOut = Paths.get(DIR+ "/"+ actualFileName[position] + ".txt");
            InputStream in = website.openStream();
            File folder = new File(DIR+ "/");
            folder.mkdirs(); // esto crea la carpeta java, independientemente que no exista
            Files.copy(in, pathOut, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            System.out.println(actualFileName[position] +" Descargado");
            petitions[position] = false;
        }
    }
  }