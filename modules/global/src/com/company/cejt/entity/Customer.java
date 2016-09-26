package com.company.cejt.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import com.haulmont.cuba.core.entity.StandardEntity;

@Table(name = "CEJT_CUSTOMER")
@Entity(name = "cejt$Customer")
public class Customer extends StandardEntity {
    private static final long serialVersionUID = -7957143557784750526L;

    @Column(name = "NAME")
    protected String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


}