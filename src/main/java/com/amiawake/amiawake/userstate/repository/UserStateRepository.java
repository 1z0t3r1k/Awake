package com.amiawake.amiawake.userstate.repository;

import com.amiawake.amiawake.userstate.entity.UserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserStateRepository extends JpaRepository<UserState, UUID> {

}
