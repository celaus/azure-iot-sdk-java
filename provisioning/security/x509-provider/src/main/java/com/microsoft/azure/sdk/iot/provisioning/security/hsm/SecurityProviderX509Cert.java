/*
 *
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 *
 */

package com.microsoft.azure.sdk.iot.provisioning.security.hsm;

import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderX509;
import com.microsoft.azure.sdk.iot.provisioning.security.exceptions.SecurityProviderException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;

public class SecurityProviderX509Cert extends SecurityProviderX509
{
    private static final String CN = "CN=";
    private static final String COMMA = ",";
    private static final String EQUALS = "=";
    private final String commonNameLeaf;
    private X509Certificate leafCertificatePublic;
    private Key leafPrivateKey;
    private Collection<X509Certificate> signerCertificates;

    private String leafCertificatePublicPem;
    private String leafPrivateKeyPem;
    private Collection<String> signerCertificatesPem;
    /**
     * Constructor to build the DICE certs from the simulator
     */
    public SecurityProviderX509Cert(String leafPublicPem, String leafPrivateKey, Collection<String> signerCertificates) throws SecurityProviderException
    {
        if (leafPublicPem == null || leafPublicPem.isEmpty())
        {
            throw new IllegalArgumentException("leaf public certificate cannot be null or empty");
        }

        if (leafPrivateKey == null || leafPrivateKey.isEmpty())
        {
            throw new IllegalArgumentException("leaf private key cannot be null or empty");
        }

        this.leafCertificatePublicPem = leafPublicPem;
        this.leafPrivateKeyPem = leafPrivateKey;
        this.signerCertificatesPem = signerCertificates;
        this.signerCertificates = new LinkedList<>();
        try
        {
            this.leafCertificatePublic = SecurityProviderX509Cert.parsePublicKeyCertificate(leafCertificatePublicPem);
            this.leafPrivateKey = SecurityProviderX509Cert.parsePrivateKey(leafPrivateKeyPem);
            for (String cert : signerCertificates)
            {
                this.signerCertificates.add(SecurityProviderX509Cert.parsePublicKeyCertificate(cert));
            }
            this.commonNameLeaf = this.getCommonName(this.leafCertificatePublic);
        }
        catch (CertificateException e)
        {
            throw new SecurityProviderException(e);
        }
    }

    private String getCommonName(X509Certificate certificate) throws SecurityProviderException
    {
        //Expected format CN=<CNName>,O=<>,C=<US>
        String cnName = certificate.getSubjectDN().getName();
        String[] tokens = cnName.split(COMMA);
        for (String token : tokens)
        {
            if (token.contains(CN))
            {
                String[] cn = token.split(EQUALS);
                return cn[cn.length - 1];
            }
        }

        throw new SecurityProviderException("CN name could not be found");
    }

     /**
     * Getter for the common name
     * @return The common name for the root cert
     */
    @Override
    public String getClientCertificateCommonName()
    {
        //SRS_SecurityClientDiceEmulator_25_005: [ This method shall return Leaf certificate name as common name ]
        return this.commonNameLeaf;
    }

    /**
     * Getter for the Alias certificate
     * @return Alias certificate
     */
    @Override
    public X509Certificate getClientCertificate()
    {
        //SRS_SecurityClientDiceEmulator_25_006: [ This method shall return Alias certificate generated by DICE ]
        return this.leafCertificatePublic;
    }

    /**
     * Getter for Alias key
     * @return Alias private key
     */
    @Override
    public Key getClientPrivateKey()
    {
        //SRS_SecurityClientDiceEmulator_25_007: [ This method shall return Alias private key generated by DICE ]
        return this.leafPrivateKey;
    }

    /**
     * Getter for the signer cert
     * @return Signer cert
     */
    public Collection<X509Certificate> getIntermediateCertificatesChain()
    {
        return this.signerCertificates;
    }

    /**
     * Getter for the Alias cert in PEM format
     * @return Alias cert in PEM format
     */
    public String getLeafCertPem()
    {
        //SRS_SecurityClientDiceEmulator_25_009: [ This method shall return Alias certificate generated by DICE as PEM string]
        return this.leafCertificatePublicPem;
    }

    /**
     * Getter for the Signer cert in PEM format
     * @return Signer cert in PEM format
     */
    public Collection<String> getSignerCertPem()
    {
        //SRS_SecurityClientDiceEmulator_25_010: [ This method shall return Signer certificate generated by DICE as PEM string ]
        return this.signerCertificatesPem;
    }

    /**
     * Generates leaf certificate with the unique id as common name
     * @param uniqueId Unique ID to be used in common name. Cannot be {@code null} or empty
     * @return A PEM formatted leaf cert with unique ID as common name
     */
    public String generateLeafCert(String uniqueId) throws SecurityProviderException
    {
        //SRS_SecurityClientDiceEmulator_25_012: [ This method shall throw SecurityProviderException if unique id is null or empty ]
        if (uniqueId == null || uniqueId.isEmpty())
        {
            throw new SecurityProviderException(new IllegalArgumentException("unique id cannot be null or empty"));
        }

        throw new UnsupportedOperationException("This method is not supported, use other means to validate certificate");
    }

    private static Key parsePrivateKey(String privateKeyString) throws CertificateException
    {
        try
        {
            // Codes_SRS_SecurityClientDiceEmulator_34_001: [This function shall return a Private Key instance created by the provided PEM formatted privateKeyString.]
            Security.addProvider(new BouncyCastleProvider());
            PEMParser privateKeyParser = new PEMParser(new StringReader(privateKeyString));
            Object possiblePrivateKey = privateKeyParser.readObject();
            PEMKeyPair ukp = (PEMKeyPair) possiblePrivateKey;
            return SecurityProviderX509Cert.getPrivateKeyFromPEMKeyPair(ukp);
        }
        catch (Exception e)
        {
            // Codes_SRS_SecurityClientDiceEmulator_34_002: [If any exception is encountered while attempting to create the private key instance, this function shall throw a CertificateException.]
            throw new CertificateException(e);
        }
    }

    private static X509Certificate parsePublicKeyCertificate(String publicKeyCertificateString) throws CertificateException
    {
        try
        {
            // Codes_SRS_SecurityClientDiceEmulator_34_003: [This function shall return an X509Certificate instance created by the provided PEM formatted publicKeyCertificateString.]
            Security.addProvider(new BouncyCastleProvider());
            PemReader publicKeyCertificateReader = new PemReader(new StringReader(publicKeyCertificateString));
            PemObject possiblePublicKeyCertificate = publicKeyCertificateReader.readPemObject();
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(possiblePublicKeyCertificate.getContent()));
        }
        catch (Exception e)
        {
            // Codes_SRS_SecurityClientDiceEmulator_34_004: [If any exception is encountered while attempting to create the public key certificate instance, this function shall throw a CertificateException.]
            throw new CertificateException(e);
        }
    }

    private static Key getPrivateKeyFromPEMKeyPair(PEMKeyPair ukp) throws PEMException
    {
        return new JcaPEMKeyConverter().setProvider("BC").getKeyPair(ukp).getPrivate();
    }
}
