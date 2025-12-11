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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;

@Configuration
public class ElasticConfig {

    protected Logger logger = LogManager.getLogger(ElasticConfig.class);

    @Value("${aodn.geonetwork4.elasticFieldLimit:5000}")
    protected Long elasticFieldLimit;

    @Value("${aodn.geonetwork4.commitInterval:50}")
    protected int commitInterval;

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
        JeevesApplicationContext ctx = (JeevesApplicationContext)ApplicationContextHolder.get();
        EsSearchManager esSearchManagerRawBean = ctx.getBean(EsSearchManager.class);
        // Override the commitInterval value which is hardcode in the geonetwork and no way to change with
        // param, this value control how many docs store in memory before batch write to elastic, unfortunately
        // number of docs is not reliable and may result in total docs size too big for bulkSave. Here we reduce
        // it to 100 from 200 and pray it works.
        try {
            logger.info("Override document commit interval to {}", commitInterval);
            Field field = esSearchManagerRawBean.getClass().getDeclaredField("commitInterval");
            field.setAccessible(true);
            field.setInt(esSearchManagerRawBean, commitInterval);
        } catch (Exception ignored) {}

        class EsSearchManagerInterceptor implements MethodInterceptor {
            @Nullable
            @Override
            public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
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

        ctx.setAllowBeanDefinitionOverriding(true);

        ProxyFactory factory1 = new ProxyFactory(esSearchManagerRawBean);
        factory1.setProxyTargetClass(true); // CGLIB for XML beans
        factory1.addAdvice(new EsSearchManagerInterceptor());
        EsSearchManager proxiedBean1 = (EsSearchManager) factory1.getProxy();

        DefaultListableBeanFactory f = (DefaultListableBeanFactory)ctx.getBeanFactory();
        String[] names = ctx.getBeanNamesForType(EsSearchManager.class);
        f.destroySingleton(names[0]);
        f.registerSingleton(names[0], proxiedBean1); // Add proxied
    }
}
