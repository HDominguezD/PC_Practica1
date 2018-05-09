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

public class FileDownloader {

	static final String DIR = "./Downloads";
        private int nDownloads;
        
	public FileDownloader(int n) {
            this.nDownloads = n;
	}

    public int getnDownloads() {
        return nDownloads;
    }

    public void setnDownloads(int nDownloads) {
        this.nDownloads = nDownloads;
    }

	public void process(String downloadsFileURL) throws MalformedURLException, IOException {
            
            File file = downloadUrls(downloadsFileURL, "output");
            
            List<List<String>> files = readUrls(file);
            SplitAndMerge splitAndMerge = new SplitAndMerge();
            for(List<String> fileUrls : files){
                for(String url : fileUrls){
                    if(fileUrls.get(0) != url){
                        downloadUrls(url, fileUrls.get(0)+".part" + fileUrls.indexOf(url));
                    }
                }
                splitAndMerge.mergeFile(DIR+ "/", fileUrls.get(0));
            }
            for(List<String> fileUrls : files){
                for(String url : fileUrls){
                    if(fileUrls.get(0) != url){
                        Path path =Paths.get(DIR+ "/" +  fileUrls.get(0)+".part" + fileUrls.indexOf(url)+".txt");
                        Files.deleteIfExists(path);
                    }
                }
                splitAndMerge.mergeFile(DIR+ "/", fileUrls.get(0));
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
        
         private File downloadUrls(String url, String header) throws MalformedURLException, IOException{
            URL website = new URL(url);
            InputStream in = website.openStream();
            Path pathOut = Paths.get(DIR+ "/"+ header + ".txt");
            File folder = new File(DIR+ "/");
            folder.mkdirs(); // esto crea la carpeta java, independientemente que no exista
            Files.copy(in, pathOut, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            File file = new File(DIR+ "/output.txt");
            return file;
         }
	
	public static void main(String[] args) throws IOException {
		String downloadFile = "https://github.com/jesussanchezoro/PracticaPC/raw/master/descargas.txt";
		FileDownloader fd = new FileDownloader(4);
		fd.process(downloadFile);
	}
}
