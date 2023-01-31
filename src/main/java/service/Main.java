package service;

import controllers.DefaultController;
import controllers.PageController;
import controllers.SiteController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;
import java.sql.SQLException;


@SpringBootApplication(scanBasePackages = {"resources.static", "model", "service", "repository"})
@EnableJpaRepositories(basePackages = {"repository"})
@EnableConfigurationProperties()
@ComponentScan(basePackageClasses = {PageController.class,
        SiteController.class,
        DefaultController.class,
        service.URLCollector.class,
        service.HTMLDataFilter.class,
        service.ManagementCommands.class})
@ComponentScan(basePackages = {"model","service"})
@EntityScan(basePackages = {"model"})
public class Main {

    public static void main(String[] args) throws SQLException, IOException {
        SpringApplication.run(service.Main.class, args);
    }

}
