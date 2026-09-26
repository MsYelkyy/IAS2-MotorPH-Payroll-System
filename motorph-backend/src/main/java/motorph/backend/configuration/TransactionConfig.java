package motorph.backend.configuration;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

@Configuration
public class TransactionConfig {

    // 4.2 fix: repositories call dataSource.getConnection() directly, so wrap the pool to let
    // those connections join the current @Transactional method instead of auto-committing.
    @Bean
    public static BeanPostProcessor transactionAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof TransactionAwareDataSourceProxy)) {
                    return new TransactionAwareDataSourceProxy(dataSource);
                }
                return bean;
            }
        };
    }
}
