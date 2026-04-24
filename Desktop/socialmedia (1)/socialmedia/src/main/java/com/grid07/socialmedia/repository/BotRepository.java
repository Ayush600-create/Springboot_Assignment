package com.grid07.socialmedia.repository;

import com.grid07.socialmedia.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {

}