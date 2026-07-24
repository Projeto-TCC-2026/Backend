package com.tcc.application.dto.response;

public class HospitalSummary {

    private Long id;
    private String name;
    private String cnpj;
    private String city;
    private String state;
    private String phone;
    private String email;
    private Long totalDoctors;
    private Boolean active;

    public HospitalSummary() {}

    public HospitalSummary(Long id, String name, String cnpj, String city, String state, 
                          String phone, String email, Long totalDoctors, Boolean active) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.city = city;
        this.state = state;
        this.phone = phone;
        this.email = email;
        this.totalDoctors = totalDoctors;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(Long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}