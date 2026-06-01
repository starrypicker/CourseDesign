package com.sports.sales.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class KeyStoreGenerator {

    public static void main(String[] args) throws Exception {
        String keyStorePath = "src/main/resources/keystore.p12";
        String password = "changeit";

        File keyStoreFile = new File(keyStorePath);
        if (keyStoreFile.exists()) {
            System.out.println("Keystore already exists at: " + keyStorePath);
            return;
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());

        char[] pwd = password.toCharArray();
        keyStore.store(new FileOutputStream(keyStorePath), pwd);

        System.out.println("Keystore generated at: " + keyStorePath);
        System.out.println("For production, please replace with a real SSL certificate.");
        System.out.println("You can use keytool to generate a self-signed certificate:");
        System.out.println("keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore " + keyStorePath + " -validity 365 -storepass " + password);
    }
}
