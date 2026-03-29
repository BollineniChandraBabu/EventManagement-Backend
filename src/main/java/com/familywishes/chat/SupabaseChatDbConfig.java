package com.familywishes.chat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class SupabaseChatDbConfig {

  @Bean(name = "supabaseChatDataSource")
  @ConfigurationProperties(prefix = "app.supabase.db")
  public DataSource supabaseChatDataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean(name = "supabaseChatJdbc")
  public NamedParameterJdbcTemplate supabaseChatJdbc(
      @Qualifier("supabaseChatDataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }
}
