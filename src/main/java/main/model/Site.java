package main.model;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "site")
public class Site {


    public Site(String url){
        this.url = url;
    }
    public Site(){

    }


    @JsonIgnore
    @Id
    @Column(name = "id",columnDefinition = "BINARY(16)")
    @Getter
    @Setter
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Getter
    @Setter
    private StatusType status;
    @Column(name = "status_time",columnDefinition = "DATETIME")
    @Getter
    @Setter

    @Temporal(TemporalType.TIMESTAMP)
    private Date statusTime;
    @Column(name = "last_error",nullable = true)
    @Getter
    @Setter
    private String error;
    @Column(name = "url")
    @Getter
    @Setter
    private String url;
    @Column(name = "name")
    @Getter
    @Setter
    private String name;
    @Transient
    @Setter
    @Getter
    private long pages;
    @Transient
    @Setter
    @Getter
    private long lemmas;
    @Override
    public int hashCode(){
        final int primal = 11;

        return primal+ name.hashCode();
    }
    @Override
    public  boolean equals(final  Object obj){
        if(this == obj){
            return true;
        }
        if(obj == null){
            return  false;
        }
        if(getClass() !=obj.getClass()){
            return false;
        }
        final Site other = (Site) obj;
        if(name == null){
            if(other.name != null){
                return false;
            }
        }else if(!name.equals(other.name)){
            return false;
        }
        return true;
    }


}
