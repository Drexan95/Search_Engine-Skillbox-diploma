package main;

import UrlService.HTMLDataFilter;
import main.controllers.DefaultController;
import main.controllers.PageController;
import main.controllers.SiteController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;
import java.sql.SQLException;


@SpringBootApplication(scanBasePackages = {"resources.static", "main.model"})
@EnableJpaRepositories
@EnableConfigurationProperties()
@ComponentScan(basePackageClasses = {PageController.class,
        SiteController.class,
        DefaultController.class,
        UrlService.URLCollector.class,
        HTMLDataFilter.class,
        main.ManagementCommands.class})
public class Main {

    public static void main(String[] args) throws SQLException, IOException {
        SpringApplication.run(Main.class, args);
    }

}
