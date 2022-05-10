package main;

import lombok.Getter;
import lombok.Setter;
import main.model.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResultResponse {
    @Getter
    @Setter
    private  long count;
    @Getter
    private boolean result = true;
    @Getter
    @Setter
    private List<Page> data = new ArrayList<>();
}
