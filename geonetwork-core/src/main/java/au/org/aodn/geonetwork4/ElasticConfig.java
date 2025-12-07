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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ElasticConfig {

    protected Logger logger = LogManager.getLogger(ElasticConfig.class);

    @Value("${aodn.geonetwork4.elasticFieldLimit:5000}")
    protected Long elasticFieldLimit;

    @Value("${aodn.geonetwork4.elasticRecordLimit:4.5e+8}")
    protected Long elasticRecordLimit;

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

        JeevesApplicationContext ctx = (JeevesApplicationContext)ApplicationContextHolder.get();
        ctx.setAllowBeanDefinitionOverriding(true);

        EsSearchManager esSearchManagerRawBean = ctx.getBean(EsSearchManager.class);
        EsRestClient esRestClientRawBean = ctx.getBean(EsRestClient.class);

        class EsRestClientInterceptor implements MethodInterceptor {
            @Nullable
            @Override
            public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
                if ("bulkRequest".equals(invocation.getMethod().getName()) &&
                        invocation.getArguments().length == 2 &&
                        invocation.getArguments()[0] instanceof String &&
                        invocation.getArguments()[1] instanceof Map) {
                    // Geonetwork do not limit the document size and therefore sometimes the bulk save failed due to too
                    // large of bulk size.
                    String index = invocation.getArguments()[0].toString();

                    @SuppressWarnings("unchecked")
                    Map<String, String> documents = (Map<String, String>)invocation.getArguments()[1];

                    // Make sure the number of doc is not too large
                    for(Map<String, String> doc : splitBySize(documents)) {
                        if(!doc.isEmpty()) {
                            esRestClientRawBean.bulkRequest(index, doc);
                        }
                    }
                }
                return invocation.proceed();
            }
            /**
             * Split the map by inspecting the size entry value, if greater than the set value, we split the map
             * @param data - Original unsplit value.
             * @return Possible split map
             */
            protected List<Map<String, String>> splitBySize(Map<String, String> data) {
                long byteCount = 0;
                Map<String, String> map = new HashMap<>();

                List<Map<String, String>> split = new ArrayList<>();
                split.add(map);

                for(Map.Entry<String, String> entry : data.entrySet()) {
                    map.put(entry.getKey(), entry.getValue());
                    byteCount += entry.getValue().getBytes(StandardCharsets.UTF_8).length;

                    if (byteCount >= elasticRecordLimit) {
                        logger.info("Batch larger than {} bytes, split", elasticRecordLimit);
                        // If it is greater than 400mb
                        byteCount = 0;
                        map = new HashMap<>();
                        split.add(map);
                    }
                }
                return split;
            }
        }

        ProxyFactory factory1 = new ProxyFactory(esSearchManagerRawBean);
        factory1.setProxyTargetClass(true); // CGLIB for XML beans
        factory1.addAdvice(new EsSearchManagerInterceptor());
        EsSearchManager proxiedBean1 = (EsSearchManager) factory1.getProxy();

        ProxyFactory factory2 = new ProxyFactory(esRestClientRawBean);
        factory2.setProxyTargetClass(true); // CGLIB for XML beans
        factory2.addAdvice(new EsRestClientInterceptor());
        EsRestClient proxiedBean2 = (EsRestClient) factory2.getProxy();

        DefaultListableBeanFactory f = (DefaultListableBeanFactory)ctx.getBeanFactory();
        String[] names = ctx.getBeanNamesForType(EsSearchManager.class);
        f.destroySingleton(names[0]);
        f.registerSingleton(names[0], proxiedBean1); // Add proxied

        names = ctx.getBeanNamesForType(EsRestClient.class);
        f.destroySingleton(names[0]);
        f.registerSingleton(names[0], proxiedBean2); // Add proxied
    }
}
