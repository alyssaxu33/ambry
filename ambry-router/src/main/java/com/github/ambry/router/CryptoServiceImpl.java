package com.github.ambry.router;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import com.github.ambry.router.CryptoService;


public class CryptoServiceImpl implements CryptoService{

  public CryptoServiceImpl(){

  }
  /**
   * Encrypts the {@code toEncrypt} with the given key. This is used for encrypting data chunks.
   *
   * @param toEncrypt {@link ByteBuffer} that needs to be encrypted
   * @param key       the secret key (of type T) to use to encrypt
   * @return the {@link ByteBuffer} containing the encrypted content. Ensure the result has all the information like the
   * IV along with the encrypted content, in order to decrypt the content with a given key
   * @throws {@link GeneralSecurityException} on any exception with encryption
   */
  @Override
  public ByteBuffer encrypt(ByteBuffer toEncrypt, Object key) throws GeneralSecurityException {
    return null;
  }

  /**
   * Decrypts the {@code toDecrypt} with the given key. This is used for decrypting data chunks.
   *
   * @param toDecrypt {@link ByteBuffer} that needs to be decrypted
   * @param key       the secret key (of type T) to use to decrypt
   * @return the {@link ByteBuffer} containing the decrypted content
   * @throws {@link GeneralSecurityException} on any exception with decryption
   */
  @Override
  public ByteBuffer decrypt(ByteBuffer toDecrypt, Object key) throws GeneralSecurityException {
    return null;
  }

  /**
   * Returns the encrypted form of the key in bytes.
   *
   * @param toEncrypt the secret key (of type T) that needs to be encrypted
   * @param key       the secret key (of type T) to use to encrypt
   * @return the {@link ByteBuffer} representing the encrypted key
   * @throws {@link GeneralSecurityException}
   */
  @Override
  public ByteBuffer encryptKey(Object toEncrypt, Object key) throws GeneralSecurityException {
    return null;
  }

  /**
   * Decrypts the key using the given {@code key}
   *
   * @param toDecrypt the {@link ByteBuffer} from which key needs to be decrypted
   * @param key       the secret key (of type T) to use to decrypt
   * @return the key thus decrypted
   * @throws {@link GeneralSecurityException}
   */
  @Override
  public Object decryptKey(ByteBuffer toDecrypt, Object key) throws GeneralSecurityException {
    return null;
  }
}
