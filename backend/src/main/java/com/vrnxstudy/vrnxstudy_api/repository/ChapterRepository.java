package com.vrnxstudy.vrnxstudy_api.repository;

import com.vrnxstudy.vrnxstudy_api.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
}
