package main.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class Statistics {
    public Statistics() {

    }


    @Autowired
    @Getter
    @Setter
    private Total total;
    @Getter
    @Setter
    private final Set<Site> detailed = new HashSet<>();


}
