package com.openfin.desktop.demo;

import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Signer {
    private final String filename;

    public Signer(String filename) {
        this.filename = filename;
    }

    public void verify() throws Exception {
        JarFile jar = new JarFile(this.filename, true);

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            byte[] buffer = new byte[8192];
            InputStream is = jar.getInputStream(entry);
            while ((is.read(buffer, 0, buffer.length)) != -1) {
                // We just read. This will throw a SecurityException
                // if a signature/digest check fails.
            }
            is.close();
        }

        if (!checkSign(jar)) {
            throw new SecurityException("not signed");
        }

    }

    private boolean checkSign(JarFile jar) throws Exception {
        InputStream jis = jar.getInputStream(jar.getEntry("META-INF/MANIFEST.MF"));
        Manifest man = new Manifest(jis);
        jis.close();

        HashSet<String> signed = new HashSet<>();
        for(Map.Entry<String, Attributes> entry: man.getEntries().entrySet()) {
            for(Object attrkey: entry.getValue().keySet()) {
                if (attrkey instanceof Attributes.Name && attrkey.toString().contains("-Digest")) {
                    signed.add(entry.getKey());
                }
            }
        }
        System.out.printf("Number of Digest from manifest %d \n", signed.size());

        Set<String> entries = new HashSet<>();
        for(Enumeration<JarEntry> entry = jar.entries(); entry.hasMoreElements(); ) {
            JarEntry je = entry.nextElement();
            String fileName = je.getName().toUpperCase();
            if (!je.isDirectory()
                    && !fileName.endsWith(".MF")
                    && !fileName.endsWith(".SF")
                    && !fileName.endsWith(".DSA")
                    && !fileName.endsWith(".EC")
                    && !fileName.endsWith(".RSA")
            ) {
                CodeSigner[] signers = je.getCodeSigners();
                if (signers != null && signers.length == 1) {
                    CodeSigner signer = signers[0];
                    if (signer.getSignerCertPath().getCertificates().size() != 4) {
                        throw new SecurityException(String.format("invalid cert chain %s", je.getName()));
                    }
                    X509Certificate cert = (X509Certificate) signer.getSignerCertPath().getCertificates().get(0);
                    if (!cert.getSubjectDN().toString().contains("OpenFin Inc.")) {
                        throw new SecurityException(String.format("invalid signed %s", je.getName()));
                    }
                    entries.add(je.getName());
                } else {
                    throw new SecurityException(String.format("missing cert %s", je.getName()));
                }
            }
        }
        System.out.printf("Number of signed entries %d \n", entries.size());

        Set<String> unsigned = new HashSet<>(entries);
        unsigned.removeAll(signed);
        return unsigned.size() == 0;
    }

    public static void main(String[] args) throws Exception {
        Signer signer = new Signer(args[0]);
        signer.verify();
    }

}
