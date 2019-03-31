package com.fvp.kubeson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.stream.Collectors;

public class CreateSignature {

    private static final String PRIVATE_KEY = "C:\\Projects\\kubeson\\keys\\private.key";

    private static final String INPUT_FOLDER = "target\\jfx\\app";

    private static final String OUTPUT_SIGNATURE = "target\\jfx\\app\\val.sig";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        // Read Private Key.
        File filePrivateKey = new File(PRIVATE_KEY);
        FileInputStream fis = new FileInputStream(PRIVATE_KEY);
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        // Create Signature
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(privateKey);

        List<Path> files =
            Files.find(Paths.get(INPUT_FOLDER), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".jar"))
                .collect(Collectors.toList());

        for (Path file : files) {
            FileInputStream jar = new FileInputStream(file.toFile());
            byte[] bytesIn = new byte[102400];
            int read;
            while ((read = jar.read(bytesIn)) != -1) {
                rsa.update(bytesIn, 0, read);
            }
            jar.close();
        }

        // Save Signature
        FileOutputStream key = new FileOutputStream(OUTPUT_SIGNATURE);
        key.write(rsa.sign());
        key.close();
    }
}
