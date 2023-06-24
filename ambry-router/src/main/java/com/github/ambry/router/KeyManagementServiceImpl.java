package com.github.ambry.router;

import com.github.ambry.rest.RestRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;


public class KeyManagementServiceImpl implements KeyManagementService{

  public KeyManagementServiceImpl(){

  }
  /**
   * Registers with KMS to create key for a unique pair of AccountId and ContainerId
   *
   * @param accountId   refers to the id of the {@link Account} to register
   * @param containerId refers to the id of the {@link Container} to register
   * @throws {@link GeneralSecurityException} on KMS unavailability or duplicate registration
   */
  @Override
  public void register(short accountId, short containerId) throws GeneralSecurityException {

  }

  /**
   * Registers with KMS to create key for a unique context.
   *
   * @param context refers to the key context to register
   * @throws {@link GeneralSecurityException} on KMS unavailability or duplicate registration
   */
  @Override
  public void register(String context) throws GeneralSecurityException {

  }

  /**
   * Fetches the key associated with the pair of AccountId and ContainerId. User is expected to have registered using
   * {@link #register(short, short)} for this pair before fetching keys.
   *
   * @param restRequest the {@link RestRequest} to use. A null pointer might be passed for this argument, service has to
   *                    be able to deal with null {@link RestRequest}.
   * @param accountId   refers to the id of the {@link Account} for which key is expected
   * @param containerId refers to the id of the {@link Container} for which key is expected
   * @return T the key associated with the accountId and containerId
   * @throws {@link GeneralSecurityException} on KMS unavailability or if key is not registered
   */
  @Override
  public Object getKey(RestRequest restRequest, short accountId, short containerId) throws GeneralSecurityException {
    return null;
  }

  /**
   * Fetches the key associated with the specified context. User is expected to have registered using
   * {@link #register(String)} for this context before fetching keys.
   *
   * @param restRequest the {@link RestRequest} to use. A null pointer might be passed for this argument, service has to
   *                    be able to deal with null {@link RestRequest}.
   * @param context     refers to the context for which key is expected
   * @return T the key associated with the context
   * @throws {@link GeneralSecurityException} on KMS unavailability or if key is not registered
   */
  @Override
  public Object getKey(RestRequest restRequest, String context) throws GeneralSecurityException {
    return null;
  }

  /**
   * Generate and return a random key (of type T)
   *
   * @return a random key (of type T)
   */
  @Override
  public Object getRandomKey() throws GeneralSecurityException {
    return null;
  }

  /**
   * Closes this stream and releases any system resources associated with it. If the stream is already closed then
   * invoking this method has no effect.
   *
   * <p> As noted in {@link AutoCloseable#close()}, cases where the
   * close may fail require careful attention. It is strongly advised to relinquish the underlying resources and to
   * internally
   * <em>mark</em> the {@code Closeable} as closed, prior to throwing
   * the {@code IOException}.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException {

  }
}
