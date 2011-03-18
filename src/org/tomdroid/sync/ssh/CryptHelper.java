package org.tomdroid.sync.ssh;

/**
 * Crypt helper for encrypt password
 * not realy safe but better than storing pw's unencrypted.
 * @author alexander rausch <mail@arausch.de>
 *
 */
public final class CryptHelper {
	public static final String SEED = "W$H%GPIOHW%$GUZ)W$ZE%GHBwreih3gopo354zwbrbsrtbuifdhbsvöioe%H$WN$%H$";
	
	public static String decodePW(String pw) {
		String decodedText = null;
		try {
			decodedText = SimpleCrypto.decrypt(CryptHelper.SEED, pw);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return decodedText;
	}
	
	public static String encodePW(String pw) {
		String encryptedText = null;
		try {
			encryptedText = SimpleCrypto.encrypt(CryptHelper.SEED, pw);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return encryptedText;
	}
}
