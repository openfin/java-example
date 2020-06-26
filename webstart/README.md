## Guidelines

1. Retrieve alias from signing certificate

```commandline
keytool -list -v -storetype pkcs12 -keystore CodeSigning.p12
```

2. Sign each jar

```commandline
jarsigner.exe -storetype pkcs12  -keystore CodeSigning.p12  -tsa http://timestamp.comodoca.com/authenticode  jar-file signing-cert-alias
```

3. Upload all files to S3 bucket specified in jnlp