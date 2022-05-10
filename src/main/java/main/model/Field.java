package main.model;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "field")
public class Field {
    public Field(Integer Id,String name,String selector,float weight){
        this.id = Id;
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }
    public Field(){

    }

    @Id
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "selector")
    @Type(type = "text")
    private String selector;
    @Column(name = "weight")
    private float weight;


}
