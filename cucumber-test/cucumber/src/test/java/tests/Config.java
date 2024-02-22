package tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class Config {

    private static Config instance = null;

    private String baseUrl;

    private Config(){
        try {

        	baseUrl = System.getProperty("baseURL");
        	
        	if( Objects.isNull(baseUrl) ) {
	            Properties prop = new Properties();
	            InputStream fileStream = this.getClass().getClassLoader().getResourceAsStream("application-test.properties");
	            if(fileStream == null){
	                System.out.println("File properties non trovato");
	                System.exit(1);
	            }
	            prop.load(fileStream);
	            this.baseUrl = prop.getProperty("baseURL");
        	}
            
        } catch (IOException ex) {
            System.out.println("Errore nel caricamento delle properties -> " + ex.getMessage());
            System.exit(1);
        }
    }


    public static Config getInstance() {
        if (Config.instance == null) {
            Config.instance = new Config();
        }

        return Config.instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
