package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.fao.geonet.api.records.formatters.FormatterParams;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * This class is used to add AspectJ support to the geonetwork. Mainly override or intercept function call
 * that is not possible without alter the code of geonetwork.
 */
@Aspect
@Configuration
@EnableAspectJAutoProxy
public class AspectConfig {

    protected Logger logger = LoggerFactory.getLogger(AspectConfig.class);

    @Pointcut("execution(public * org.fao.geonet.api.records.formatters.XsltFormatter.format(org.fao.geonet.api.records.formatters.FormatterParams))")
    public void interceptFormatter() {}
    /**
     * Use aspectJ to intercept all call that ends with WithHttpInfo, we must always use geonetwork api call
     * ends with WithHttpInfo because the way geonetworks works is you must present an X-XSRF-TOKEN and session
     * in the call with username password, otherwise it will fail.
     * You need to make an init call to get the X-XSRF-TOKEN, the call will have status forbidden
     * and comes back with the token in cookie, then you need to set the token before next call.
     */
    @Pointcut("execution(public * au.org.aodn.geonetwork_api.openapi.api..*.*WithHttpInfo(..))")
    public void interceptWithHttpInfo() {}

    @Around("interceptWithHttpInfo()")
    public ResponseEntity<?> aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return (ResponseEntity<?>) joinPoint.proceed();
        }
        catch(HttpClientErrorException.Forbidden e) {
            // cookie format is like this
            // XSRF-TOKEN=634f5b0d-49b6-43a6-a995-c7cb0db9eb64; Path=/geonetwork
            String cookie = e.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
            String token = cookie.split(";")[0].split("=")[1].trim();

            // All these api object have the getApiClient() method
            Method method = joinPoint.getTarget().getClass().getMethod("getApiClient");
            ApiClient apiClient = (ApiClient)method.invoke(joinPoint.getTarget());

            logger.info("Setting X-XSRF-TOKEN for {} to {}", joinPoint.getTarget().getClass(), token);
            apiClient.addDefaultHeader("X-XSRF-TOKEN", token);
            apiClient.addDefaultCookie("XSRF-TOKEN", token);

            return (ResponseEntity<?>)joinPoint.proceed();
        }
    }
    /**
     * Use to intercept the formatter to provide a post-processing transformation. That is after the original transform
     * you got a chance to do additional transformation based on the output content. By doing so you just need to
     * add a post-processing.xsl in the same folder, and it will work without duplicate or create new view.xsl
     * @param joinPoint - Aspect call param for @Around
     * @return - A post-processed string
     * @throws Throwable - Not expect to have this throw
     */
    @Around("interceptFormatter()")
    public Object afterProcessingXslt(ProceedingJoinPoint joinPoint) throws Throwable {
        if(joinPoint.getArgs()[0] instanceof FormatterParams) {
            FormatterParams params = (FormatterParams)joinPoint.getArgs()[0];

            // Expect a string of html after processing
            Object value = joinPoint.proceed();

            Path p = Paths.get(params.formatDir + "/post-processing.xsl");
            if(Files.exists(p)) {
                try {
                    Source xslt = new StreamSource(p.toFile());
                    // Input source (XHTML), but need to remove the extra tag as this is not valid during transformation
                    Source text = new StreamSource(
                            new StringReader(value.toString().replaceAll("<!DOCTYPE[^>]*>", ""))
                    );

                    TransformerFactory factory = TransformerFactory.newInstance();
                    Transformer transformer = factory.newTransformer(xslt);

                    // Set the parameter value
                    transformer.setParameter("xml", params.metadataInfo.asXml());

                    // Output destination
                    StringWriter outputWriter = new StringWriter();

                    // Perform the transformation
                    transformer.transform(text, new StreamResult(outputWriter));
                    // Add back the tag
                    return "<!DOCTYPE div\n SYSTEM \"html\">" + outputWriter;
                }
                catch(Exception e) {
                    logger.error("Error in post-processing", e);
                }
            }
            return value;
        }
        else {
            throw new RuntimeException("Format function call expected FormatterParams args");
        }
    }

    @PostConstruct
    public void init() {
        logger.info("Init XsltConfig completed");
    }
}
