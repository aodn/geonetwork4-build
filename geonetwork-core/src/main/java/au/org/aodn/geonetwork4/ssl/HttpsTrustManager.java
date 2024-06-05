package au.org.aodn.geonetwork4.ssl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class HttpsTrustManager implements X509TrustManager {

    protected static Logger logger = LogManager.getLogger(HttpsTrustManager.class);

    protected static TrustManager[] trustManagers = new TrustManager[]{new HttpsTrustManager()};
    protected static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return _AcceptedIssuers;
    }

    public static void allowAllSSL() throws NoSuchAlgorithmException, KeyManagementException {

        SSLContext context;

        try {
            context = SSLContext.getInstance("SSL");
            context.init(null, trustManagers, new SecureRandom());
        }
        catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw e;
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(context != null ? context.getSocketFactory() : null);

        // Set trust all host verifier as the default one.
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        logger.info("In DEV env - Set to trust all host even host verification failed!!");
    }
}
