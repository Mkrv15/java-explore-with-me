package ru.practicum.ewm.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "total_likes")
    private Integer totalLikes = 0;

    @Column(name = "total_dislikes")
    private Integer totalDislikes = 0;

    @Column(name = "author_rating")
    private Integer authorRating = 0;
}
