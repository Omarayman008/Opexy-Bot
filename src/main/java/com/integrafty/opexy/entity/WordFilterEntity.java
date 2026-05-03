package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "word_filter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WordFilterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word", unique = true, nullable = false)
    private String word;
}
