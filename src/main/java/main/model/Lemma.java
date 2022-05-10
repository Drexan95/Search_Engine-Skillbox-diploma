package main.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Entity
@Table(name ="lemma")
public class Lemma implements Comparable<Lemma> {
@Column(name = "lemma")
@Getter
@Setter
    private String name;
    @Id
    @Column(name= "id")
    @Getter
    private int id;
    @Setter
    @Getter
    @Column(name = "frequency")
    private int frequency;
    @Setter
    @Getter
    @Column(name = "site_id")
    private Long siteId;
    @Transient
    @Setter
    @Getter
    private List<Page> urls = new ArrayList<>(); ;
//    @Transient
//    private Map<Page,Float> pageAndRank;


    public Lemma(int Id,String name,int frequency){
        this.id = Id;
        this.name = name;
        this.frequency = frequency;
//        pageAndRank = new HashMap<>();

    }
    public Lemma(){

    }


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
        final Lemma other = (Lemma) obj;
        if(name == null){
            if(other.name != null){
                return false;
            }
        }else if(!name.equals(other.name)){
            return false;
        }
        return true;
    }
    @Override
    public int compareTo(Lemma o) {
        if(this.frequency > o.frequency){
            return  1;
        }
        else if(this.frequency < o.frequency){
            return -1;
        }
        else return 0;
    }
    @Override
    public String toString(){
        return name;
    }
}
