package model;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.stereotype.Component;

@Component
public class Total {

    @Getter
    @Setter
    private  long sites;
    @Getter
    @Setter
    private  long pages;
    @Getter
    @Setter
    private long lemmas;

    @Getter
    @Setter
    private boolean isIndexing = false;
}
