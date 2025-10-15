package au.org.aodn.geonetwork4;

import jeeves.config.springutil.JeevesApplicationContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.index.es.EsRestClient;
import org.fao.geonet.kernel.search.EsSearchManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class ElasticConfig {

    protected Logger logger = LogManager.getLogger(ElasticConfig.class);

    @Value("${aodn.geonetwork4.elasticFieldLimit:5000}")
    protected Long elasticFieldLimit;

    @Value("${es.index.records}")
    protected String recordIndexName;

    @Autowired
    protected EsRestClient client;

    /**
     * The geonetwork may create record that exceed the field limit of default elastic setting, it is not good
     * to manually set it plus you cannot stop user from re-create the index, here we increase the limit
     * of the field setting after index created or recreate
     */
    @PostConstruct
    public void init() {
        class EsSearchManagerInterceptor implements MethodInterceptor {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                if ("init".equals(invocation.getMethod().getName())) {
                    Object result = invocation.proceed();
                    try {
                        logger.info("Patch elastic search field limit to {}", elasticFieldLimit);
                        // Just patch the record index, other have no issue
                        client.getClient()
                                .indices()
                                .putSettings(put -> put
                                        .index(recordIndexName)
                                        .settings(setting -> setting
                                                .index(i -> i
                                                        .mapping(m -> m.totalFields(tf -> tf.limit(elasticFieldLimit)))
                                                )
                                        )
                                );
                    } catch(IOException ioe) {
                        logger.warn("Fail to change field limit setting, you may experience error during indexing indicate field limit exceed default value");
                    }
                    return result;
                }
                return invocation.proceed(); // Other methods pass through
            }
        }

        JeevesApplicationContext ctx = (JeevesApplicationContext)ApplicationContextHolder.get();
        EsSearchManager rawBean = ctx.getBean(EsSearchManager.class);
        String[] names = ctx.getBeanNamesForType(EsSearchManager.class);

        ProxyFactory factory = new ProxyFactory(rawBean);
        factory.setProxyTargetClass(true); // CGLIB for XML beans
        factory.addAdvice(new EsSearchManagerInterceptor());
        EsSearchManager proxiedBean = (EsSearchManager) factory.getProxy();

        ctx.setAllowBeanDefinitionOverriding(true);

        DefaultListableBeanFactory f = (DefaultListableBeanFactory)ctx.getBeanFactory();
        f.destroySingleton(names[0]);
        f.registerSingleton(names[0], proxiedBean); // Add proxied
    }
}
