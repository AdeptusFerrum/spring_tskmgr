package com.example.taskmanager.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagTest {

    @Test
    void constructorWithName_shouldSetName() {
        Tag tag = new Tag("работа");
        assertThat(tag.getName()).isEqualTo("работа");
        assertThat(tag.getId()).isNull();
    }

    @Test
    void settersAndGetters_shouldWork() {
        Tag tag = new Tag();
        tag.setId(5L);
        tag.setName("home");

        assertThat(tag.getId()).isEqualTo(5L);
        assertThat(tag.getName()).isEqualTo("home");
    }
}