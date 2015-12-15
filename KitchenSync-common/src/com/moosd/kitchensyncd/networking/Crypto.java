package com.moosd.kitchensyncd.networking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Crypto {

//	byte[] encKey;
//	Cipher cipherEnc, cipherDec;
AesCbcWithIntegrity.SecretKeys keys;


	public Crypto(String key) throws GeneralSecurityException {
try {
	keys = AesCbcWithIntegrity.generateKeyFromPassword(key, "LOLOLO");
} catch(IOException e){e.printStackTrace();}
        /*byte[] iv = { 5, 6, 55, 44, 0, 0, 0, 0, 45, 67, 87, 21, 121, 1, 7, 0 };
        IvParameterSpec ivspec = new IvParameterSpec(iv);
		//try {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(key.getBytes("UTF-8"));
		encKey = md.digest();

		cipherEnc = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKey = new SecretKeySpec(encKey, "AES");
		cipherEnc.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
			cipherDec = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipherDec.init(Cipher.DECRYPT_MODE, secretKey, ivspec);*/
		

		/*} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
/*
		try {
			System.out.println(new String(
					decrypt(encrypt("Crypto engine functional."))));
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public byte[] encrypt(String input) throws GeneralSecurityException,
			UnsupportedEncodingException {
		//byte[] b = Base64.encodeBase64(/*cipherEnc.doFinal(*/input.getBytes()/*)*/);
		//System.out.println(new String(b));
		AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(input, keys);
   return cipherTextIvMac.toString().getBytes();
	//	return b;
	}

	public byte[] decrypt(String input) throws GeneralSecurityException {
		//return /*cipherDec.doFinal(*/Base64.decodeBase64(input.getBytes())/*)*/;
		try {
			return AesCbcWithIntegrity.decryptString(new AesCbcWithIntegrity.CipherTextIvMac(new String(input)), keys).getBytes();
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	public byte[] encrypt(byte[] input) throws GeneralSecurityException {
		//byte[] b = Base64.encodeBase64(/*cipherEnc.doFinal(*/input/*)*/);
		//System.out.println(new String(b));
		//return b;

		AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(input, keys);
		return cipherTextIvMac.toString().getBytes();
	}

	public byte[] decrypt(byte[] input) throws GeneralSecurityException {
		//return /*cipherDec.doFinal(*/Base64.decodeBase64(input)/*)*/;
		try {
			return AesCbcWithIntegrity.decrypt(new AesCbcWithIntegrity.CipherTextIvMac(new String(input)), keys);
		} catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

}
