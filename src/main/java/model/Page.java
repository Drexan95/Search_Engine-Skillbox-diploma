package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;
import org.hibernate.annotations.Type;


import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name="page")
@JsonIgnoreProperties(value = { "content","lemms","lemmRank" })
public class Page implements  Comparable<Page>{

@JsonIgnoreProperties
    @javax.persistence.Id
    @Column(name ="id")
    @Setter
    @Getter
    private int id;
    @Column(name = "path")
    @Setter
    @Getter
    private String path;
    @Column(name = "code")
    private int code;

    @Column(name = "content")
    @JsonIgnore
    @Type(type = "text")
    private String content;
    @Transient
    @Getter
    @Setter
    private String site;

    @Column(name = "site_id")
    @Getter
    @Setter
    private Long siteid;
    @Transient
    @Getter
    @Setter
    private String siteName;
    @Transient
    @Getter
    @Setter
    private String title;
    @Transient
    @Getter
    @Setter
    private String snippet;
    @Transient
    @Setter
    @Getter
    @com.fasterxml.jackson.annotation.JsonIgnore
    private float absRelevancy;
    @Transient
    private float relevance;
    @Transient
    @Getter
    @Setter
    private List<String> lemms = new ArrayList<>();
    @Transient
    @Getter
    @Setter
    private String uri;

    public Page(int id, String path,int code,String content) {
        this.id = id;
        this.code = code;
        this.content = content;
        this.path = path;

    }
    public Page(String path){
        this.path = path;
    }

    public Page(){

    }



    @JsonIgnore
    public String getContent() {
        return content;
    }

    @JsonIgnore
    public void setContent(String content) {
        this.content = content;
    }


    public float getRelevance() {
        return relevance;
    }

    public void setRelevance(float relevance) {
        this.relevance = relevance;
    }

    @Override
    public String toString(){

        return path +"\n "+getTitle()+"\n Релевантность : "+ getRelevance()+"\n"+"Леммы :"+"\n"+lemms+"\n"+"Отрывок текста:"+"\n"+getSnippet();
    }
    @Override
    public int hashCode(){
        final int primal = 11;

        return primal+ path.hashCode();
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
        final Page other = (Page) obj;
        if(path == null){
            if(other.path != null){
                return false;
            }
        }else if(!path.equals(other.path)){
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Page o) {
    if(this.relevance > o.relevance){
        return -1;
    }else if(this.relevance < o.relevance){
        return 1;
    }
    else return 0;

    }
}
