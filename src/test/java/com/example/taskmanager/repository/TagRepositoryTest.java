package com.example.taskmanager.repository;

import com.example.taskmanager.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TagRepositoryTest {

    @Autowired private TagRepository tagRepository;

    @Test
    void save_shouldPersist() {
        Tag saved = tagRepository.save(new Tag("работа"));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("работа");
    }

    @Test
    void findByName_whenExists_shouldReturn() {
        tagRepository.save(new Tag("home"));
        Optional<Tag> found = tagRepository.findByName("home");
        assertThat(found).isPresent();
    }

    @Test
    void findByName_whenNotExists_shouldReturnEmpty() {
        assertThat(tagRepository.findByName("nope")).isEmpty();
    }

    @Test
    void save_duplicateName_shouldThrow() {
        tagRepository.save(new Tag("unique"));
        try {
            tagRepository.saveAndFlush(new Tag("unique"));
        } catch (Exception e) {
            assertThat(e).isNotNull();
            return;
        }
        throw new AssertionError("Expected exception on duplicate name");
    }
}