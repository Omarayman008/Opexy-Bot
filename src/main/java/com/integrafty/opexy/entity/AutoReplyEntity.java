package com.integrafty.opexy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "auto_replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoReplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", unique = true, nullable = false)
    private String keyword;

    @Column(name = "response_text", nullable = false, length = 2000)
    private String responseText;

    @Column(name = "added_by")
    private String addedBy;
}
