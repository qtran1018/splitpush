package com.splitpush.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;

    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private Set<TripGroup> tripGroups = new HashSet<>();

    @OneToMany(mappedBy = "paidBy", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Expense> expensesPaid = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<ExpenseParticipant> expenseParticipants = new HashSet<>();

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<TripGroup> getTripGroups() {
        return tripGroups;
    }

    public void setTripGroups(Set<TripGroup> tripGroups) {
        this.tripGroups = tripGroups;
    }

    public Set<Expense> getExpensesPaid() {
        return expensesPaid;
    }

    public void setExpensesPaid(Set<Expense> expensesPaid) {
        this.expensesPaid = expensesPaid;
    }

    public Set<ExpenseParticipant> getExpenseParticipants() {
        return expenseParticipants;
    }

    public void setExpenseParticipants(Set<ExpenseParticipant> expenseParticipants) {
        this.expenseParticipants = expenseParticipants;
    }
}
