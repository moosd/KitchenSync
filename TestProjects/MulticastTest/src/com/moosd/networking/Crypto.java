package com.moosd.networking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class Crypto {
    AesCbcWithIntegrity.SecretKeys keys;


    public Crypto(String key) throws GeneralSecurityException {
        try {
            keys = AesCbcWithIntegrity.generateKeyFromPassword(key, "LOLOLO");
        } catch(IOException e){e.printStackTrace();}
    }

    public byte[] encrypt(String input) throws GeneralSecurityException,
            UnsupportedEncodingException {
        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(input, keys);
        return cipherTextIvMac.toString().getBytes();
    }

    public byte[] decrypt(String input) throws GeneralSecurityException {
        try {
            return AesCbcWithIntegrity.decryptString(new AesCbcWithIntegrity.CipherTextIvMac(new String(input)), keys).getBytes();
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public byte[] encrypt(byte[] input) throws GeneralSecurityException {
        AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(input, keys);
        return cipherTextIvMac.toString().getBytes();
    }

    public byte[] decrypt(byte[] input) throws GeneralSecurityException {
        try {
            return AesCbcWithIntegrity.decrypt(new AesCbcWithIntegrity.CipherTextIvMac(new String(input)), keys);
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

}
